package com.elmfer.cnmcu.ui;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.elmfer.cnmcu.mixins.DrawContextInvoker;
import com.elmfer.cnmcu.network.*;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gl.*;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.texture.GlTexture;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;

import com.elmfer.cnmcu.EventHandler;
import com.elmfer.cnmcu.animation.ClockTimer;
import com.elmfer.cnmcu.config.Config;
import com.elmfer.cnmcu.cpp.NativesUtils;
import com.elmfer.cnmcu.mcu.Sketches;
import com.elmfer.cnmcu.mcu.Toolchain;
import com.elmfer.cnmcu.network.IDEScreenMCUControlPayload.Control;
import com.elmfer.cnmcu.network.IDEScreenSyncPayload.BusStatus;
import com.elmfer.cnmcu.network.IDEScreenSyncPayload.CPUStatus;
import com.elmfer.cnmcu.ui.handler.IDEScreenHandler;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.extension.imguifiledialog.ImGuiFileDialog;
import imgui.extension.memedit.MemoryEditor;
import imgui.extension.texteditor.TextEditor;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiDockNodeFlags;
import imgui.flag.ImGuiFocusedFlags;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import org.lwjgl.opengl.*;

public class IDEScreen extends HandledScreen<IDEScreenHandler> {

    private static final String DOCKSPACE_NAME = "DockSpace";
    private static final String CODE_EDITOR_NAME = "Code Editor";
    private static final String CONSOLE_NAME = "Console";

    private static final ImGuiIO IO = ImGui.getIO();

    private final TextEditor textEditor;
    private final MemoryEditor memoryEditor;
    private final IDEScreenHandler handler;
    private final Framebuffer framebuffer;

    private boolean saved = true;

    public CPUStatus cpuStatus = new CPUStatus(0, 0, 0, 0, 0, 0, 0);
    public BusStatus busStatus = new BusStatus(0, 0, false);
    public boolean isPowered = false;
    public boolean isClockPaused = false;
    public ByteBuffer zeroPage = BufferUtils.createByteBuffer(256);

    private final ClockTimer heartbeatTimer = new ClockTimer(1);
    private final IDEScreenHeartbeatPayload heartbeatPacket;

    private CompletableFuture<byte[]> compileFuture;
    private Future<UploadROMResponsePayload> uploadPacket;
    private boolean shouldUpload = false;

    private boolean showAbout = false;
    private boolean showUpdates = false;
    private boolean showToolchainSettings = false;
    private boolean showLoadBackup = false;
    private boolean shouldLoadDefaults = false;
    private boolean showRegistersInHex = Config.showRegistersInHex();

    public IDEScreen(IDEScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);

        textEditor = new TextEditor();
        memoryEditor = new MemoryEditor();
        heartbeatPacket = new IDEScreenHeartbeatPayload(handler.getMcuID());

        textEditor.setText(handler.getCode());

        this.handler = handler;

        final var factory = getFramebufferFactory();
        this.framebuffer = factory.create();
    }

    private SimpleFramebufferFactory getFramebufferFactory() {
        final var window = client.getFramebuffer();
        return new SimpleFramebufferFactory(window.textureWidth, window.textureHeight, false, 0);
    }
    private void prepareFramebuffer() {
        final var factory = getFramebufferFactory();
        if(this.framebuffer.textureWidth != factory.width() || this.framebuffer.textureHeight != factory.height()) {
            this.framebuffer.resize(factory.width(), factory.height());
        }
        factory.prepare(framebuffer);
    }

    @Override
    public void render(DrawContext stack, int mouseX, int mouseY, float delta) {
        sendHeartbeat();

        ImGui.newFrame(); // TODO: Fix incorrect cursor when exiting screen with resize cursor.

        ImGui.setNextWindowPos(0, 0, ImGuiCond.Always);
        ImGui.setNextWindowSize(IO.getDisplaySizeX(), IO.getDisplaySizeY(), ImGuiCond.Always);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0.0f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0.0f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0.0f, 0.0f);

        int windowFlags = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove
                | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoBringToFrontOnFocus | ImGuiWindowFlags.NoNavFocus
                | ImGuiWindowFlags.NoBackground | ImGuiWindowFlags.MenuBar;

        ImGui.begin(DOCKSPACE_NAME, windowFlags);
        ImGui.dockSpace(ImGui.getID(DOCKSPACE_NAME), 0, 0, ImGuiDockNodeFlags.PassthruCentralNode);
        ImGui.popStyleVar(3);
        
        genMainMenuBar();
        genTextEditor();
        genPopups();
        genConsole();
        genMCUStatus();
        genCPUStatus();
        genMemoryViewer();
        if (Config.showDocs())
            genDocs();
        
        if (ImGui.isWindowFocused(ImGuiFocusedFlags.RootAndChildWindows) && ImGui.isKeyPressed(GLFW.GLFW_KEY_ESCAPE))
            ImGui.setWindowFocus(null);
        
        ImGui.end();

        ImGui.render();

        prepareFramebuffer();

        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, ((GlTexture) framebuffer.getColorAttachment())
                .getOrCreateFramebuffer(((GlBackend) RenderSystem.getDevice()).getBufferManager(), null));
        GlStateManager._colorMask(true, true, true, true);
        GL11C.glClearColor(0.f, 0.f, 0.f, 0.f);
        GlStateManager._clear(GL11C.GL_COLOR_BUFFER_BIT);
        GlStateManager._viewport(0, 0, framebuffer.textureWidth, framebuffer.textureHeight);
        EventHandler.IMGUI_GL3.renderDrawData(ImGui.getDrawData());
        var stackInvoker = (DrawContextInvoker) stack;
        stackInvoker.cnmcu$drawTexturedQuad(RenderPipelines.GUI_TEXTURED, framebuffer.getColorAttachmentView(),
                RenderSystem.getSamplerCache().get(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.NEAREST, FilterMode.LINEAR, false),
                0, 0, width, height, .0f, 1.0f, 1.f, 0.f, -1);
    }

    @Override
    public boolean keyPressed(KeyInput key) {

        if (IO.getWantCaptureKeyboard())
            return true;

        if (key.key() == GLFW.GLFW_KEY_ESCAPE || client.options.inventoryKey.matchesKey(key)) {
            close();
            return true;
        }

        return true;
    }

    private void genMainMenuBar() {
        if (!ImGui.beginMenuBar())
            return;

        if (ImGui.beginMenu("File")) {
            if (ImGui.menuItem("Load Backup##File"))
                showLoadBackup = true;
            if (ImGui.menuItem("Load File"))
                ImGuiFileDialog.openModal("##LoadSketchFile", "Load File", ".s,.asm,.c,.cpp", Config.lastSaveFilePath(), 1, 0, 0);
            ImGui.separator();
            if (ImGui.menuItem("Save", "CTRL+S"))
                save();
            if (ImGui.menuItem("Save As"))
                ImGuiFileDialog.openModal("##SaveSketchFile", "Save As", ".s,.asm,.c,.cpp", Config.lastSaveFilePath(), 1, 0, 0);   

            ImGui.endMenu();
        }

        if (ImGui.beginMenu("Edit")) {
            if (ImGui.menuItem("Undo", "CTRL+Z"))
                textEditor.undo(1);
            if (ImGui.menuItem("Redo", "CTRL+Y"))
                textEditor.redo(1);
            ImGui.separator();
            if (ImGui.menuItem("Cut", "CTRL+X"))
                textEditor.cut();
            if (ImGui.menuItem("Copy", "CTRL+C"))
                textEditor.copy();
            if (ImGui.menuItem("Paste", "CTRL+V"))
                textEditor.paste();
            ImGui.endMenu();
        }

        if (ImGui.beginMenu("Select")) {
            if (ImGui.menuItem("Select All", "CTRL+A"))
                textEditor.selectAll();
            ImGui.endMenu();
        }

        if (ImGui.beginMenu("Tools")) {
            if (ImGui.menuItem("Build"))
                build();
            if (ImGui.menuItem("Upload"))
                upload();
            if (ImGui.menuItem("Settings"))
                showToolchainSettings = true;
            ImGui.endMenu();
        }

        if (ImGui.beginMenu("Help")) {
            if (ImGui.menuItem("About"))
                showAbout = true;
            if (ImGui.menuItem((Config.showDocs() ? "Hide " : "Show ") + "Documentation"))
                Config.setShowDocs(!Config.showDocs());
            if (ImGui.menuItem("Updates"))
                showUpdates = true;
            ImGui.endMenu();
        }
    }

    private void genPopups() {
        if (showAbout) {
            ImGui.openPopup("About");
            showAbout = false;
        }

        if (showUpdates) {
            ImGui.openPopup("Updates");
            showUpdates = false;
        }

        if (showToolchainSettings) {
            ImGui.openPopup("Toolchain Settings");
            showToolchainSettings = false;
        }

        if (showLoadBackup) {
            ImGui.openPopup("Load Backup");
            Sketches.listBackups();
            showLoadBackup = false;
        }


        float width = UIRender.getWindowWidth();
        float height = UIRender.getWindowHeight();
        float centerX = width / 2;
        float centerY = height / 2;
        ImGui.setNextWindowPos(centerX, centerY, ImGuiCond.Always, 0.5f, 0.5f);
        ImGui.setNextWindowSize(800, 322, ImGuiCond.Once);
        
        if (ImGuiFileDialog.display("##SaveSketchFile", 0, 0, 0, width, height)) {
            if (ImGuiFileDialog.isOk()) {
                String filePath = ImGuiFileDialog.getFilePathName();
                if (filePath == null || filePath.isEmpty())
                    return;
                Config.setLastSaveFilePath(filePath);
                Sketches.saveSketch(textEditor.getText(), filePath);
                save();
            }
            ImGuiFileDialog.close();
        }
        
        if (ImGuiFileDialog.display("##LoadSketchFile", 0, 0, 0, width, height)) {
            if (ImGuiFileDialog.isOk()) {
                Sketches.saveBackup(textEditor.getText());
                String filePath = ImGuiFileDialog.getFilePathName();
                if (filePath == null || filePath.isEmpty())
                    return;
                Config.setLastSaveFilePath(filePath);
                textEditor.setText(Sketches.loadSketch(filePath));
                save();
            }
            ImGuiFileDialog.close();
        }

        ImGui.setNextWindowPos(centerX, centerY, ImGuiCond.Always, 0.5f, 0.5f);
        ImGui.setNextWindowSize(800, 322, ImGuiCond.Once);
        ImGui.setNextWindowSizeConstraints(0, 0, width, height);
        if (ImGui.beginPopupModal("About")) {
            float windowHeight = ImGui.getContentRegionAvailY();
            QuickReferences.genAbout();
            ImGui.newLine();
            ImGui.setCursorPosY(Math.max(windowHeight, ImGui.getCursorPosY()));
            if (ImGui.button("Close"))
                ImGui.closeCurrentPopup();
            ImGui.endPopup();
        }

        ImGui.setNextWindowPos(centerX, centerY, ImGuiCond.Always, 0.5f, 0.5f);
        ImGui.setNextWindowSize(800, 322, ImGuiCond.Once);
        ImGui.setNextWindowSizeConstraints(0, 0, width, height);
        if (ImGui.beginPopupModal("Updates")) {
            float windowHeight = ImGui.getContentRegionAvailY();
            QuickReferences.genUpdates();
            ImGui.newLine();
            ImGui.setCursorPosY(Math.max(windowHeight, ImGui.getCursorPosY()));
            if (ImGui.button("Close"))
                ImGui.closeCurrentPopup();
            ImGui.sameLine();
            if (ImGui.checkbox("Notify me of updates", Config.adviseUpdates()))
                Config.setAdviseUpdates(!Config.adviseUpdates());
            ImGui.endPopup();
        }

        ImGui.setNextWindowPos(centerX, centerY, ImGuiCond.Always, 0.5f, 0.5f);
        ImGui.setNextWindowSize(500, 300, ImGuiCond.Once);
        ImGui.setNextWindowSizeConstraints(0, 0, width, height);
        if (ImGui.beginPopupModal("Toolchain Settings")) {
            float windowHeight = ImGui.getContentRegionAvailY();
            Toolchain.genToolchainConfigUI();
            ImGui.newLine();
            ImGui.setCursorPosY(Math.max(windowHeight, ImGui.getCursorPosY()));
            if (ImGui.button("Close"))
                ImGui.closeCurrentPopup();
            ImGui.sameLine();
            if (ImGui.button("Refresh"))
                Toolchain.loadConfig();
            ImGui.pushStyleColor(ImGuiCol.Text, shouldLoadDefaults ? 0xFF5555FF : 0xFFFFFFFF);
            ImGui.sameLine();
            if (ImGui.button(!shouldLoadDefaults ? "Load Defaults" : "Are you sure?")) {
                if (shouldLoadDefaults)
                    Toolchain.loadDefaults();
                shouldLoadDefaults = !shouldLoadDefaults;
            }
            ImGui.popStyleColor();
            ImGui.endPopup();
        }

        ImGui.setNextWindowPos(centerX, centerY, ImGuiCond.Always, 0.5f, 0.5f);
        ImGui.setNextWindowSize(800, 400, ImGuiCond.Once);
        if (ImGui.beginPopupModal("Load Backup")) {
            float windowHeight = ImGui.getContentRegionAvailY();
            ImGui.beginChild("####SketchBackups", 0, windowHeight - 30, false);
            Sketches.genLoadBackupUI();
            ImGui.endChild();
            ImGui.setCursorPosY(Math.max(windowHeight, ImGui.getCursorPosY()));
            if (ImGui.button("Close"))
                ImGui.closeCurrentPopup();
            ImGui.sameLine();
            if (ImGui.button("Load")) {
                ImGui.closeCurrentPopup();
                textEditor.setText(Sketches.getSelectedBackup());
            }
            ImGui.endPopup();
        }

        ImGui.endMenuBar();
    }

    private void genDocs() {
        float centerX = UIRender.getWindowWidth() / 2;
        float centerY = UIRender.getWindowHeight() / 2;

        ImGui.setNextWindowPos(centerX, centerY, ImGuiCond.FirstUseEver, 0.5f, 0.5f);
        ImGui.setNextWindowSize(800, 400, ImGuiCond.FirstUseEver);

        if (!ImGui.begin("Documentation")) {
            ImGui.end();
            return;
        }

        QuickReferences.genNanoDocumentation();

        ImGui.end();
    }

    private void genTextEditor() {
        if (!ImGui.begin(CODE_EDITOR_NAME)) {
            ImGui.end();
            return;
        }

        if (ctrlKeyCombo('S'))
            save();

        ImGui.beginDisabled(shouldUpload || compileFuture != null);
        if (ImGui.button("Build"))
            build();
        ImGui.sameLine();
        if (ImGui.button("Upload"))
            upload();
        ImGui.endDisabled();

        if (compileFuture != null && compileFuture.isDone() && !shouldUpload)
            compileFuture = null;

        if (shouldUpload && compileFuture.isDone() && uploadPacket == null) {
            try {
                uploadPacket = UploadROMRequestPayload.send(handler.getMcuID(), compileFuture.get());
            } catch (Exception e) {
                shouldUpload = false;
            }
        }

        if (uploadPacket != null && uploadPacket.isDone()) {
            try {
                Toolchain.appendBuildStdout("Upload", uploadPacket.get().message());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            uploadPacket = null;
            shouldUpload = false;
        }

        ImGui.text(String.format("%s %s", Toolchain.getBuildVariable("input"), saved ? "[Saved]" : "[Unsaved]"));
        ImGui.setNextWindowSize(0, 400);
        textEditor.render("TextEditor");

        if (ImGui.isWindowFocused(ImGuiFocusedFlags.RootAndChildWindows) && ImGui.isKeyPressed(GLFW.GLFW_KEY_ESCAPE))
            ImGui.setWindowFocus(null);
        
        if (textEditor.isTextChanged())
            saved = false;

        ImGui.end();
    }

    private void genConsole() {
        if (!ImGui.begin(CONSOLE_NAME)) {
            ImGui.end();
            return;
        }

        ImGui.text("Output");
        ImGui.sameLine();
        if (ImGui.button("Clear"))
            Toolchain.clearBuildStdout();
        ImGui.separator();

        ImGui.beginChild("OutputBody");
        ImGui.textWrapped(Toolchain.getBuildStdout());
        ImGui.endChild();

        ImGui.end();
    }

    private void genMemoryViewer() {
        if (!ImGui.begin("Zero Page Viewer")) {
            ImGui.end();
            return;
        }

        memoryEditor.drawContents(NativesUtils.getByteBufferAddress(zeroPage), zeroPage.capacity());

        ImGui.end();
    }

    private void genMCUStatus() {
        if (!ImGui.begin("MCU Status")) {
            ImGui.end();
            return;
        }

        ImGui.text(String.format("MCU ID: %s", handler.getMcuID()));

        ImGui.text("Specs");
        ImGui.sameLine();
        ImGui.separator();

        ImGui.text("CPU: mos6502");
        ImGui.text("RAM: 0.5KB");
        ImGui.text("ROM: 8KB");
        ImGui.text("Clock: 800Hz");
        ImGui.text("Modules: GPIO, EL, UART");

        ImGui.text("Controls");
        ImGui.sameLine();
        ImGui.separator();
        // TODO: Fix power immediately turning off when clicking power after an upload.
        if (ImGui.checkbox("Power", isPowered))
            ClientPlayNetworking.send(new IDEScreenMCUControlPayload(handler.getMcuID(),
                    isPowered ? Control.POWER_OFF : Control.POWER_ON)
            );
        ImGui.sameLine();
        if (ImGui.button("Reset"))
            ClientPlayNetworking.send(new IDEScreenMCUControlPayload(handler.getMcuID(), Control.RESET));
        ImGui.sameLine();
        ImGui.beginDisabled(!isPowered);
        if (ImGui.checkbox("Pause", isClockPaused))
            ClientPlayNetworking.send(new IDEScreenMCUControlPayload(handler.getMcuID(),
                    isClockPaused ? Control.RESUME_CLOCK : Control.PAUSE_CLOCK));
        ImGui.sameLine();
        ImGui.beginDisabled(!isClockPaused);
        if (ImGui.button("Step"))
            ClientPlayNetworking.send(new IDEScreenMCUControlPayload(handler.getMcuID(), Control.CYCLE));
        ImGui.endDisabled();
        ImGui.endDisabled();

        ImGui.end();
    }

    private void genCPUStatus() {
        if (!ImGui.begin("CPU Status")) {
            ImGui.end();
            return;
        }

        if (ImGui.checkbox("Registers in Hex", showRegistersInHex))
            showRegistersInHex = !showRegistersInHex;

        if (showRegistersInHex) {
            ImGui.text(String.format("A: 0x%02X", cpuStatus.a()));
            ImGui.text(String.format("X: 0x%02X", cpuStatus.x()));
            ImGui.text(String.format("Y: 0x%02X", cpuStatus.y()));
        } else {
            ImGui.text(String.format("A: %d", cpuStatus.a()));
            ImGui.text(String.format("X: %d", cpuStatus.x()));
            ImGui.text(String.format("Y: %d", cpuStatus.y()));
        }

        ImGui.text(String.format("PC: 0x%04X", cpuStatus.pc()));
        ImGui.text(String.format("SP: 0x%02X", cpuStatus.sp()));
        ImGui.text(String.format("Flags: %c%c%c%c%c%c%c%c", (cpuStatus.flags() & 0x80) != 0 ? 'N' : '-',
                (cpuStatus.flags() & 0x40) != 0 ? 'V' : '-', (cpuStatus.flags() & 0x20) != 0 ? 'U' : '-',
                (cpuStatus.flags() & 0x10) != 0 ? 'B' : '-', (cpuStatus.flags() & 0x08) != 0 ? 'D' : '-',
                (cpuStatus.flags() & 0x04) != 0 ? 'I' : '-', (cpuStatus.flags() & 0x02) != 0 ? 'Z' : '-',
                (cpuStatus.flags() & 0x01) != 0 ? 'C' : '-'));
        ImGui.text(String.format("Cycles: %d", cpuStatus.cycles()));

        ImGui.text("Bus");
        ImGui.sameLine();
        ImGui.separator();

        ImGui.text(String.format("Address: 0x%02X", busStatus.address()));
        ImGui.text(String.format("Data: 0x%02X", busStatus.data()));
        ImGui.text(String.format("RW: %s", !busStatus.rw() ? "Read" : "Write"));

        ImGui.end();
    }

    private void sendHeartbeat() {
        if (heartbeatTimer.ticksPassed() != 0)
            ClientPlayNetworking.send(heartbeatPacket);
    }

    private void build() {
        save();

        Toolchain.clearBuildStdout();
        compileFuture = Toolchain.build(textEditor.getText());
    }

    private void upload() {
        save();

        Toolchain.clearBuildStdout();
        compileFuture = Toolchain.build(textEditor.getText());
        shouldUpload = true;
    }

    private void save() {
        if (saved || textEditor.getText().isEmpty())
            return;
        
        saved = true;
        ClientPlayNetworking.send(new IDEScreenSaveCodePayload(handler.getMcuID(), textEditor.getText()));
        
        if (!textEditor.getText().isEmpty())
            Sketches.saveBackup(textEditor.getText());
    }

    @Override
    protected void drawBackground(DrawContext var1, float var2, int var3, int var4) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removed() {
        if (!saved && !textEditor.getText().isEmpty())
            Sketches.saveBackup(textEditor.getText());
        
        Config.setShowRegistersInHex(showRegistersInHex);
        Config.save();
        Toolchain.saveConfig();
        Toolchain.clearBuildStdout();

        if (compileFuture != null)
            compileFuture.cancel(true);
    }

    private boolean ctrlKeyCombo(char key) {
        boolean ctrl = IO.getConfigMacOSXBehaviors() ? IO.getKeySuper() : IO.getKeyCtrl();
        return ctrl && ImGui.isKeyPressed(key);
    }

    @Override
    public void close() {
        framebuffer.delete();

        super.close();
    }
}
