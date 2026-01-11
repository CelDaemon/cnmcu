package com.elmfer.cnmcu;

import com.elmfer.cnmcu.config.ModSetup;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.internal.ImGuiContext;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldTerrainRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;

import static com.elmfer.cnmcu.CodeNodeMicrocontrollers.CONFIG;
import static com.elmfer.cnmcu.CodeNodeMicrocontrollers.TOOLCHAIN;

public class EventHandler {

    public final static ImGuiImplGlfw IMGUI_GLFW = new ImGuiImplGlfw();
    public final static ImGuiImplGl3 IMGUI_GL3 = new ImGuiImplGl3();
    public static ImGuiContext IMGUI;
    public static ImGuiIO IMGUI_IO;
    private static boolean hasNotifiedPlayerAboutUpdate = false;

    public static void registerClientEventHandlers() {
        ClientLifecycleEvents.CLIENT_STARTED.register(EventHandler::onClientStarted);
        ClientLifecycleEvents.CLIENT_STOPPING.register(EventHandler::onClientStopping);
        WorldRenderEvents.START_MAIN.register(EventHandler::onStartRenderWorld);
    }

    private static void onStartRenderWorld(WorldTerrainRenderContext context) {
        if (!hasNotifiedPlayerAboutUpdate)
            notifyPlayerAboutUpdate();

        EventHandler.IMGUI_GLFW.newFrame();
    }

    private static void onClientStarted(Minecraft client) {
        IMGUI = ImGui.createContext();

        IMGUI_GLFW.init(client.getWindow().handle(), true);
        IMGUI_GL3.init("#version 330 core");

        IMGUI_IO = ImGui.getIO();

        IMGUI_IO.setIniFilename(ModSetup.IMGUI_INI_FILE);
        IMGUI_IO.setDisplaySize(client.getWindow().getScreenWidth(), client.getWindow().getScreenHeight());
        IMGUI_IO.setConfigFlags(ImGuiConfigFlags.DockingEnable);
    }

    private static void onClientStopping(Minecraft client) {
        IMGUI_GL3.dispose();
        IMGUI_GLFW.dispose();
        ImGui.destroyContext(IMGUI);
        CONFIG.save().join();
        TOOLCHAIN.saveConfig().join();
    }

    private static void notifyPlayerAboutUpdate() {
        if (hasNotifiedPlayerAboutUpdate)
            return;

        hasNotifiedPlayerAboutUpdate = true;

        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (!ModSetup.hasCheckedForUpdates() || !ModSetup.isUpdateAvailable())
                return;

            MutableComponent updateMessage = MutableComponent
                    .create(new PlainTextContents.LiteralContents("An update is available for CodeNode Microcontrollers: v"));
            updateMessage.setStyle(updateMessage.getStyle().withColor(0xFF5CFF5C));

            MutableComponent version = MutableComponent.create(new PlainTextContents.LiteralContents(ModSetup.getLatestVersion() + " "));
            version.setStyle(version.getStyle().withColor(0xFFFFF41F));

            MutableComponent forMCVersions = MutableComponent.create(new PlainTextContents.LiteralContents(
                    "for Minecraft " + String.join(", ", ModSetup.getLatestForMinecraftVersions())));
            forMCVersions.setStyle(forMCVersions.getStyle().withColor(0xFF85A9FF));

            updateMessage.getSiblings().add(version);
            updateMessage.getSiblings().add(forMCVersions);

            if(client.player != null)
                client.player.displayClientMessage(updateMessage, true);
        });
    }
}
