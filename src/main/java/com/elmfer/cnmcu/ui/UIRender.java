package com.elmfer.cnmcu.ui;

import net.minecraft.client.Minecraft;

//Rendering implementation for UIs.
public class UIRender {
    public static Minecraft mc = Minecraft.getInstance();

    public static int getWindowWidth() {
        return mc.getWindow().getScreenWidth();
    }

    public static int getWindowHeight() {
        return mc.getWindow().getScreenHeight();
    }

}