package com.elmfer.cnmcu;

import com.elmfer.cnmcu.mcu.NanoMCU;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;

import java.util.function.UnaryOperator;

public class DataComponents {
    public static final DataComponentType<String> CODE = register("code", builder -> builder.persistent(Codec.STRING).cacheEncoding());
    public static final DataComponentType<NanoMCU.State> MCU = register("mcu", builder -> builder.persistent(NanoMCU.State.CODEC).cacheEncoding());
    public static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> componentFactory) {
        var componentKey = ResourceKey.create(BuiltInRegistries.DATA_COMPONENT_TYPE.key(), CNMCU.id(name));

        var componentType = componentFactory.apply(DataComponentType.builder()).build();
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, componentKey, componentType);

        return componentType;
    }

    public static void init() {
        // DUMMY
    }
}
