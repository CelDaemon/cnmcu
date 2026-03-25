package com.elmfer.cnmcu.menu;

import com.elmfer.cnmcu.common.Common;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public class Menus {
    public static final MenuType<IDEMenu> IDE_MENU = register("ide", IDEMenu::new, IDEMenu.OpenData.STREAM_CODEC);

    private static <T extends AbstractContainerMenu, D> MenuType<T> register(
            String name, ExtendedMenuType.ExtendedFactory<T, D> factory,
            StreamCodec<ByteBuf, D> packetCodec) {
        var id = Common.id(name);
        var type = new ExtendedMenuType<>(factory, packetCodec);
        Registry.register(BuiltInRegistries.MENU, id, type);

        return type;
    }

    public static void init() {
        // DUMMY
    }
}
