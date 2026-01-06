package com.elmfer.cnmcu.ui.handler;

import com.elmfer.cnmcu.CodeNodeMicrocontrollers;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;

public class Menus {
    
    public static final MenuType<IDEMenu> IDE_MENU = new ExtendedScreenHandlerType<>(IDEMenu::new, IDEMenu.OpenData.STREAM_CODEC);
    
    public static void init() {
        Registry.register(BuiltInRegistries.MENU, CodeNodeMicrocontrollers.id("ide"), IDE_MENU);
    }
}
