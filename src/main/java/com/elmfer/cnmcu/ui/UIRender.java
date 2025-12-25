package com.elmfer.cnmcu.ui;

import net.minecraft.client.MinecraftClient;

//Rendering implementation for UIs.
public class UIRender {
    public static MinecraftClient mc = MinecraftClient.getInstance();

    public static int getWindowWidth() {
        return mc.getWindow().getWidth();
    }

    public static int getWindowHeight() {
        return mc.getWindow().getHeight();
    }

}