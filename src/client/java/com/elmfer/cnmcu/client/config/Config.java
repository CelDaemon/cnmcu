package com.elmfer.cnmcu.client.config;

import com.elmfer.cnmcu.client.toolchain.Sketches;
import com.elmfer.cnmcu.common.Common;
import com.google.gson.FormattingStyle;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.JsonOps;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static com.elmfer.cnmcu.common.Common.LOGGER;

public class Config {
    public static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve(Common.MOD_ID + ".json");
    private final ToolchainConfig toolchainConfig = new ToolchainConfig();
    private CompletableFuture<Void> writingFuture = CompletableFuture.completedFuture(null);
    private boolean hexRegisters;
    private boolean showDocs;
    private int maxBackups;
    private Path lastSaved;

    public boolean getHexRegisters() {
        return hexRegisters;
    }

    public void setHexRegisters(boolean hexRegisters) {
        this.hexRegisters = hexRegisters;
    }

    public boolean getShowDocs() {
        return showDocs;
    }

    public void setShowDocs(boolean showDocs) {
        this.showDocs = showDocs;
    }

    public int getMaxBackups() {
        return maxBackups;
    }

    public Path getLastSaved() {
        return lastSaved;
    }

    public void setLastSaved(Path lastSaved) {
        this.lastSaved = lastSaved;
    }

    public ToolchainConfig getToolchainConfig() {
        return toolchainConfig;
    }

    public boolean isWriting() {
        return !writingFuture.isDone();
    }

    public CompletableFuture<Void> write() {
        if (isWriting())
            return writingFuture;

        final var record = getRecord();

        final var element = ConfigRecord.CODEC.encodeStart(JsonOps.INSTANCE, record).getOrThrow();

        return writingFuture = CompletableFuture.runAsync(() -> {
            try (var writer = new JsonWriter(new OutputStreamWriter(Files.newOutputStream(CONFIG_PATH)))) {
                writer.setFormattingStyle(FormattingStyle.PRETTY);
                GsonHelper.writeValue(writer, element, null);
            } catch (IOException e) {
                LOGGER.error("Failed to save config file", e);
            }
        }, Util.ioPool());
    }

    public void reset() {
        hexRegisters = false;
        showDocs = false;
        maxBackups = 30;
        lastSaved = Sketches.SKETCHES_PATH.resolve("untitled.s");
        toolchainConfig.reset();
    }

    private void load(ConfigRecord record) {
        hexRegisters = record.hexRegisters();
        showDocs = record.showDocs();
        maxBackups = record.maxBackups();
        lastSaved = record.lastSaved();
        toolchainConfig.load(record.toolchain());
    }

    public void read() {
        if (Files.notExists(CONFIG_PATH)) {
            reset();
            return;
        }

        final JsonElement element;
        try (var reader = new JsonReader(new InputStreamReader(Files.newInputStream(CONFIG_PATH)))) {
            element = JsonParser.parseReader(reader);
        } catch (Exception e) {
            LOGGER.error("Failed to load config file", e);
            reset();
            return;
        }

        var result = ConfigRecord.CODEC.parse(JsonOps.INSTANCE, element);

        if (result.isError()) {
            reset();
            return;
        }

        load(result.getOrThrow());
    }

    private ConfigRecord getRecord() {
        return new ConfigRecord(
                hexRegisters,
                showDocs,
                maxBackups,
                lastSaved,
                toolchainConfig.getRecord()
        );
    }
}
