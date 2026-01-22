package com.elmfer.cnmcu.config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import com.elmfer.cnmcu.CodeNodeMicrocontrollers;
import com.elmfer.cnmcu.mcu.Sketches;
import com.google.gson.FormattingStyle;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Util;

import static com.elmfer.cnmcu.CodeNodeMicrocontrollers.LOGGER;

public class Config {
    public boolean isAdviseUpdates() {
        return adviseUpdates;
    }

    public void setAdviseUpdates(boolean adviseUpdates) {
        this.adviseUpdates = adviseUpdates;
    }

    public boolean isHexRegisters() {
        return hexRegisters;
    }

    public void setHexRegisters(boolean hexRegisters) {
        this.hexRegisters = hexRegisters;
    }

    public boolean isShowDocs() {
        return showDocs;
    }

    public int getMaxBackups() {
        return maxBackups;
    }

    public void setMaxBackups(int maxBackups) {
        this.maxBackups = maxBackups;
    }

    public String getLastSavePath() {
        return lastSavePath;
    }

    public void setLastSavePath(String lastSavePath) {
        this.lastSavePath = lastSavePath;
    }

    public Config(boolean adviseUpdates, boolean hexRegisters, boolean showDocs, int maxBackups, String lastSaveFilePath) {
        this.adviseUpdates = adviseUpdates;
        this.hexRegisters = hexRegisters;
        this.showDocs = showDocs;
        this.maxBackups = maxBackups;
        this.lastSavePath = lastSaveFilePath;
    }
    private boolean adviseUpdates;
    private boolean hexRegisters;
    private boolean showDocs;
    private int maxBackups;
    private String lastSavePath;
    private static final Codec<Config> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.fieldOf("advise_updates").forGetter(Config::isAdviseUpdates),
                    Codec.BOOL.fieldOf("hex_registers").forGetter(Config::isHexRegisters),
                    Codec.BOOL.fieldOf("show_docs").forGetter(Config::isShowDocs),
                    Codec.INT.fieldOf("max_backups").forGetter(Config::getMaxBackups),
                    Codec.STRING.fieldOf("last_save_path").forGetter(Config::getLastSavePath)
            ).apply(instance, Config::new));
    public static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve(CodeNodeMicrocontrollers.MOD_ID + ".json");
    private static CompletableFuture<Void> saveTask;

    public static Config defaultConfig() {
        return new Config(true, false, false, 30,
                Sketches.SKETCHES_PATH.resolve("untitled.s").toAbsolutePath().toString());
    }

    public void setShowDocs(boolean showDocs) {
        this.showDocs = showDocs;
    }

    public CompletableFuture<Void> save() {
        waitForSave();

        final var element = CODEC.encodeStart(JsonOps.INSTANCE, this).getOrThrow();

        return saveTask = CompletableFuture.runAsync(() -> {
            try(var writer = new JsonWriter(new OutputStreamWriter(Files.newOutputStream(CONFIG_PATH)))) {
                writer.setFormattingStyle(FormattingStyle.PRETTY);
                GsonHelper.writeValue(writer, element, null);
            } catch (IOException e) {
                LOGGER.error("Failed to save config file", e);
            }
            saveTask = null;
        }, Util.backgroundExecutor());
    }

    public static Config load() {
        if(Files.notExists(CONFIG_PATH))
            return defaultConfig();
        final JsonElement element;
        try(var reader = new JsonReader(new InputStreamReader(Files.newInputStream(CONFIG_PATH)))) {
            element = JsonParser.parseReader(reader);
        } catch (Exception e) {
            LOGGER.error("Failed to load config file", e);
            return defaultConfig();
        }
        var result = CODEC.parse(JsonOps.INSTANCE, element);
        return result.mapOrElse(x -> x,
                x -> defaultConfig());
    }
    
    public void waitForSave() {
        if (saveTask != null)
            saveTask.join();
    }
}
