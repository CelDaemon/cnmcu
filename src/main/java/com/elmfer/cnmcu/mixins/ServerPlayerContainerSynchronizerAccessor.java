package com.elmfer.cnmcu.mixins;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.server.level.ServerPlayer$1")
public interface ServerPlayerContainerSynchronizerAccessor {
    @Accessor(value = "field_58075")
    ServerPlayer cnmcu$getPlayer();
}
