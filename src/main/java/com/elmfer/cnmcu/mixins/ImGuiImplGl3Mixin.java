package com.elmfer.cnmcu.mixins;

import imgui.gl3.ImGuiImplGl3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ImGuiImplGl3.class, remap = false)
public class ImGuiImplGl3Mixin {
    @Inject(method = "createFontsTexture", at = @At(value = "INVOKE", remap = false, target = "Lorg/lwjgl/opengl/GL32;glTexParameteri(III)V"))
    void updateFontsTexture(CallbackInfoReturnable<Boolean> cir) {
//        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_MIN_LOD, 0);
//        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_MAX_LOD, 0);
//        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_MAX_LEVEL, 0);
    }
}
