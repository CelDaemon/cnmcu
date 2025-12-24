package com.elmfer.cnmcu.ui.handler;

import com.elmfer.cnmcu.CodeNodeMicrocontrollers;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;

public class ScreenHandlers {
    
    public static final ScreenHandlerType<IDEScreenHandler> IDE_SCREEN_HANDLER = new ExtendedScreenHandlerType<>(IDEScreenHandler::new, IDEScreenHandler.OpenData.PACKET_CODEC);
    
    public static void init() {
        Registry.register(Registries.SCREEN_HANDLER, CodeNodeMicrocontrollers.id("ide"), IDE_SCREEN_HANDLER);
    }
}
