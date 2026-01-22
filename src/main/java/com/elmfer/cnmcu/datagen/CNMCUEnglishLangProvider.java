package com.elmfer.cnmcu.datagen;

import com.elmfer.cnmcu.blocks.Blocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class CNMCUEnglishLangProvider extends FabricLanguageProvider {
    protected CNMCUEnglishLangProvider(@NotNull FabricDataOutput dataOutput, @NotNull CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generateTranslations(@NotNull HolderLookup.Provider registryLookup, @NotNull TranslationBuilder translationBuilder) {
        translationBuilder.add(Blocks.CN_NANO_BLOCK, "Nano");
    }
}
