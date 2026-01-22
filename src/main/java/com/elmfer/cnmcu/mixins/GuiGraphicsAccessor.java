package com.elmfer.cnmcu.mixins;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiGraphics.class)
public interface GuiGraphicsAccessor {
    @Invoker("submitBlit")
    void cnmcu$submitBlit(RenderPipeline pipeline, GpuTextureView texture, GpuSampler sampler, int x1, int y1, int x2, int y2, float u1, float v1, float u2, float v2, int color);
}
