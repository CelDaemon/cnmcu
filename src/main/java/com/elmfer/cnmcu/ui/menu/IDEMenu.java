package com.elmfer.cnmcu.ui.menu;

import com.elmfer.cnmcu.blocks.Blocks;
import com.elmfer.cnmcu.network.IDEScreenSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import com.elmfer.cnmcu.blockentities.NanoBlockEntity;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IDEMenu extends AbstractContainerMenu {
    public final String code;
    public final ContainerLevelAccess containerAccess;
    @Nullable
    public final NanoBlockEntity blockEntity;
    @Nullable
    public final ServerPlayer player;
    public IDEMenu(int containerId, Inventory ignoredPlayerInventory, OpenData data) {
        super(Menus.IDE_MENU, containerId);

        code = data.code;
        containerAccess = ContainerLevelAccess.NULL;

        blockEntity = null;
        player = null;
    }
    public IDEMenu(int containerId, ContainerLevelAccess containerAccess, @NotNull NanoBlockEntity blockEntity,
                   @NotNull ServerPlayer player) {
        super(Menus.IDE_MENU, containerId);
        this.code = "";
        this.containerAccess = containerAccess;

        this.blockEntity = blockEntity;
        this.player = player;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(containerAccess, player, Blocks.CN_NANO_BLOCK);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if(blockEntity == null || player == null)
            return;
        ServerPlayNetworking.send(player, IDEScreenSyncPayload.create(blockEntity));
    }

    public record OpenData(
            String code
    ) {
       public static final StreamCodec<ByteBuf, OpenData> STREAM_CODEC = StreamCodec.composite(
               ByteBufCodecs.STRING_UTF8, OpenData::code,
               OpenData::new
       );
    }
}
