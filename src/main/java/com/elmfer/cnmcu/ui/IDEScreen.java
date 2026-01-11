package com.elmfer.cnmcu.ui;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.elmfer.cnmcu.mixins.GuiContextInvoker;
import com.elmfer.cnmcu.network.*;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import imgui.flag.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;

import com.elmfer.cnmcu.EventHandler;
import com.elmfer.cnmcu.animation.ClockTimer;
import com.elmfer.cnmcu.cpp.NativesUtils;
import com.elmfer.cnmcu.mcu.Sketches;
import com.elmfer.cnmcu.network.IDEScreenMCUControlPayload.Control;
import com.elmfer.cnmcu.network.IDEScreenSyncPayload.BusStatus;
import com.elmfer.cnmcu.network.IDEScreenSyncPayload.CPUStatus;
import com.elmfer.cnmcu.ui.handler.IDEMenu;

import imgui.ImGui;
import imgui.extension.imguifiledialog.ImGuiFileDialog;
import imgui.extension.memedit.MemoryEditor;
import imgui.extension.texteditor.TextEditor;
import org.lwjgl.opengl.GL30C;

import static com.elmfer.cnmcu.CodeNodeMicrocontrollers.CONFIG;
import static com.elmfer.cnmcu.CodeNodeMicrocontrollers.TOOLCHAIN;
import static com.elmfer.cnmcu.EventHandler.IMGUI;
import static com.elmfer.cnmcu.EventHandler.IMGUI_IO;

public class IDEScreen extends AbstractContainerScreen<IDEMenu> {
    private static final String CODE_EDITOR_NAME = "Code Editor";
    private static final String CONSOLE_NAME = "Console";

    private final TextEditor textEditor;
    private final MemoryEditor memoryEditor;
    private final IDEMenu handler;
    private final RenderTarget renderTarget;

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

    public IDEScreen(IDEMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);

        textEditor = new TextEditor();
        memoryEditor = new MemoryEditor();
        heartbeatPacket = new IDEScreenHeartbeatPayload(handler.getMcuID());

        textEditor.setText(handler.getCode());

        this.handler = handler;

        final var descriptor = getTargetDescriptor();
        this.renderTarget = descriptor.allocate();
    }

    private RenderTargetDescriptor getTargetDescriptor() {
        final var target = minecraft.getMainRenderTarget();
        return new RenderTargetDescriptor(target.width, target.height, false, 0);
    }

    private void prepareRenderTarget() {
        final var descriptor = getTargetDescriptor();
        if(this.renderTarget.width != descriptor.width() || this.renderTarget.height != descriptor.height()) {
            this.renderTarget.resize(descriptor.width(), descriptor.height());
        }
        descriptor.prepare(renderTarget);
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float delta) {
        sendHeartbeat();

        ImGui.setCurrentContext(IMGUI);

        ImGui.newFrame();

        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0.0f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0.0f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0.0f, 0.0f);
        ImGui.dockSpaceOverViewport(ImGui.getMainViewport(), ImGuiDockNodeFlags.PassthruCentralNode);
        ImGui.popStyleVar(3);
        
        genMainMenuBar();
        genTextEditor();
        genPopups();
        genConsole();
        genMCUStatus();
        genCPUStatus();
        genMemoryViewer();
        if (CONFIG.isShowDocs())
            genDocs();
        
        if (ImGui.isWindowFocused(ImGuiFocusedFlags.RootAndChildWindows) && ImGui.isKeyPressed(GLFW.GLFW_KEY_ESCAPE))
            ImGui.setWindowFocus(null);
        
        ImGui.end();

        ImGui.render();

        prepareRenderTarget();

        final var device = (GlDevice) RenderSystem.getDevice();
        final var debugLabels = device.debugLabels();
        debugLabels.pushDebugGroup(() -> "ImGui render");
        final var texture = (GlTexture) renderTarget.getColorTexture();
        if(texture == null)
            throw new RuntimeException("Failed to get color texture");

        final var stateAccess = device.directStateAccess();
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, texture
                .getFbo(stateAccess, null));
        GlStateManager._viewport(0, 0, renderTarget.width, renderTarget.height);
        EventHandler.IMGUI_GL3.renderDrawData(ImGui.getDrawData());
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, 0);
        debugLabels.popDebugGroup();
        var guiInvoker = (GuiContextInvoker) gui;
        guiInvoker.cnmcu$submitBlit(RenderPipelines.GUI_TEXTURED, renderTarget.getColorTextureView(),
                RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST),
                0, 0, width, height, .0f, 1.0f, 1.f, 0.f, -1);
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent key) {

        if (IMGUI_IO.getWantCaptureKeyboard())
            return true;

        if (key.key() == GLFW.GLFW_KEY_ESCAPE || minecraft.options.keyInventory.matches(key)) {
            onClose();
            return true;
        }

        return true;
    }

    private void genMainMenuBar() {
        if (!ImGui.beginMainMenuBar())
            return;

        if (ImGui.beginMenu("File")) {
            if (ImGui.menuItem("Load Backup##File"))
                showLoadBackup = true;
            if (ImGui.menuItem("Load File"))
                ImGuiFileDialog.openModal("##LoadSketchFile", "Load File", ".s,.asm,.c,.cpp", CONFIG.getLastSavePath(), 1, 0, 0);
            ImGui.separator();
            if (ImGui.menuItem("Save", "CTRL+S"))
                save();
            if (ImGui.menuItem("Save As"))
                ImGuiFileDialog.openModal("##SaveSketchFile", "Save As", ".s,.asm,.c,.cpp", CONFIG.getLastSavePath(), 1, 0, 0);

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
            if (ImGui.menuItem((CONFIG.isShowDocs() ? "Hide " : "Show ") + "Documentation"))
                CONFIG.setShowDocs(!CONFIG.isShowDocs());
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


        var width = renderTarget.width;
        var height = renderTarget.height;
        var centerX = width / 2;
        var centerY = height / 2;
        ImGui.setNextWindowPos(centerX, centerY, ImGuiCond.Always, 0.5f, 0.5f);
        ImGui.setNextWindowSize(800, 322, ImGuiCond.Once);
        
        if (ImGuiFileDialog.display("##SaveSketchFile", 0, 0, 0, width, height)) {
            if (ImGuiFileDialog.isOk()) {
                String filePath = ImGuiFileDialog.getFilePathName();
                if (filePath == null || filePath.isEmpty())
                    return;
                CONFIG.setLastSavePath(filePath);
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
                CONFIG.setLastSavePath(filePath);
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
            if (ImGui.checkbox("Notify me of updates", CONFIG.isAdviseUpdates()))
                CONFIG.setAdviseUpdates(!CONFIG.isAdviseUpdates());
            ImGui.endPopup();
        }

        ImGui.setNextWindowPos(centerX, centerY, ImGuiCond.Always, 0.5f, 0.5f);
        ImGui.setNextWindowSize(500, 300, ImGuiCond.Once);
        ImGui.setNextWindowSizeConstraints(0, 0, width, height);
        if (ImGui.beginPopupModal("Toolchain Settings")) {
            float windowHeight = ImGui.getContentRegionAvailY();
            TOOLCHAIN.genToolchainConfigUI();
            ImGui.newLine();
            ImGui.setCursorPosY(Math.max(windowHeight, ImGui.getCursorPosY()));
            if (ImGui.button("Close"))
                ImGui.closeCurrentPopup();
            ImGui.sameLine();
            if (ImGui.button("Refresh"))
                TOOLCHAIN.reloadConfig();
            ImGui.pushStyleColor(ImGuiCol.Text, shouldLoadDefaults ? 0xFF5555FF : 0xFFFFFFFF);
            ImGui.sameLine();
            if (ImGui.button(!shouldLoadDefaults ? "Load Defaults" : "Are you sure?")) {
                if (shouldLoadDefaults)
                    TOOLCHAIN.loadDefaults();
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
        var centerX = renderTarget.width / 2;
        var centerY = renderTarget.height / 2;

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

        if (isSaveKeybind())
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

        if (shouldUpload && compileFuture != null && compileFuture.isDone() && uploadPacket == null) {
            try {
                uploadPacket = UploadROMRequestPayload.send(handler.getMcuID(), compileFuture.get());
            } catch (Exception e) {
                shouldUpload = false;
            }
        }

        if (uploadPacket != null && uploadPacket.isDone()) {
            try {
                TOOLCHAIN.appendBuildStdout("Upload", uploadPacket.get().message());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            uploadPacket = null;
            shouldUpload = false;
        }

        ImGui.text(String.format("%s %s", TOOLCHAIN.getInputPath().map(Path::toString).orElse("<unknown>"),
                saved ? "[Saved]" : "[Unsaved]"));
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
            TOOLCHAIN.clearBuildStdout();
        ImGui.separator();

        ImGui.beginChild("OutputBody");
        ImGui.textWrapped(TOOLCHAIN.getBuildStdout());
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

        if (ImGui.checkbox("Registers in Hex", CONFIG.isHexRegisters()))
            CONFIG.setHexRegisters(!CONFIG.isHexRegisters());

        if (CONFIG.isHexRegisters()) {
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

        TOOLCHAIN.clearBuildStdout();
        compileFuture = TOOLCHAIN.build(textEditor.getText());
    }

    private void upload() {
        build();
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
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float f, int i, int j) {

    }

    @Override
    public void removed() {
        if (!saved && !textEditor.getText().isEmpty())
            Sketches.saveBackup(textEditor.getText());

        ImGui.setCurrentContext(IMGUI);
        renderTarget.destroyBuffers();
        textEditor.destroy();
        memoryEditor.destroy();
        ImGui.setMouseCursor(ImGuiMouseCursor.Arrow);
        CONFIG.save();
        TOOLCHAIN.saveConfig();
        TOOLCHAIN.clearBuildStdout();

        if (compileFuture != null)
            compileFuture.cancel(true);
    }

    private boolean isSaveKeybind() {
        var mod = IMGUI_IO.getConfigMacOSXBehaviors() ? IMGUI_IO.getKeySuper() : IMGUI_IO.getKeyCtrl();
        return mod && ImGui.isKeyPressed('S');
    }

    @Override
    public void onClose() {
        super.onClose();
    }
}
