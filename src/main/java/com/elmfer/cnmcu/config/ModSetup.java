package com.elmfer.cnmcu.config;

import com.elmfer.cnmcu.CNMCU;
import com.elmfer.cnmcu.cpp.NativesLoader;
import com.elmfer.cnmcu.util.HTTPSFetcher;
import com.google.gson.JsonArray;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.elmfer.cnmcu.CNMCU.LOGGER;

public final class ModSetup {
    public static final Path IMGUI_CONFIG_PATH = CNMCU.DATA_PATH.resolve("imgui.ini");

    private static final String GITHUB_REPO_URL = "https://api.github.com/repos/elmfrain/cnmcu";

    private static JsonArray githubAssets;
    
    private ModSetup() {
    }

    public static void createDirectories() {
        try {
            Files.createDirectories(NativesLoader.NATIVES_PATH);
        } catch (IOException e) {
            LOGGER.error("Failed to create directories", e);
        }
    }

    public static void copyDefaultImGuiConfig(@NotNull ResourceManager resourceLoader) {
        if (Files.exists(IMGUI_CONFIG_PATH))
            return;

        final var resource = resourceLoader.getResource(CNMCU.id("setup/imgui.ini"))
                .orElseThrow();

        try(final var stream = resource.open()) {
            Files.copy(stream, IMGUI_CONFIG_PATH);
        } catch (Exception e) {
            LOGGER.error("Failed to write default imgui config file", e);
        }
    }

    public static void downloadNatives() {
        ensureInstallNatives(NativesLoader.NATIVES_PATH.resolve(NativesLoader.resolveNative()),
                NativesLoader.resolveNative());
    }

    public static byte[] getGitHubAsset(String assetNameTarget) {
        LOGGER.info("Downloading asset from GitHub... {}", assetNameTarget);

        listGitHubAssets();

        String assetDownloadUrl = null;

        try {
            JsonArray assets = ModSetup.githubAssets;

            for (int i = 0; i < assets.size(); i++) {
                String assetName = assets.get(i).getAsJsonObject().get("name").getAsString();
                if (assetName.equals(assetNameTarget)) {
                    assetDownloadUrl = assets.get(i).getAsJsonObject().get("url").getAsString();
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse github assets", e);
            return null;
        }

        if (assetDownloadUrl == null)
            return null;

        HTTPSFetcher fetcher = new HTTPSFetcher(assetDownloadUrl);
        fetcher.addHeader("Accept", "application/octet-stream");
        fetcher.start();
        fetcher.waitForCompletion();

        if (fetcher.hasFailed())
            return null;

        return fetcher.byteContent();
    }

    private static void listGitHubAssets() {
        if (githubAssets != null)
            return;

        LOGGER.debug("Listing assets from GitHub...");

        HTTPSFetcher fetcher = new HTTPSFetcher(
                GITHUB_REPO_URL + "/releases/tags/" + "0.0.10a-1.20.4"); // + CNMCU.MOD_VERSION
        fetcher.addHeader("Accept", "application/vnd.github.v3+json");
        fetcher.start();
        fetcher.waitForCompletion();

        if (fetcher.statusCode() == 0)
            throw new RuntimeException("Failed to connect to GitHub API! Check your internet connection.");

        if (fetcher.hasFailed())
            return;

        try {
            githubAssets = fetcher.jsonContent().getAsJsonObject().get("assets").getAsJsonArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse assets from GitHub!", e);
        }
    }

    private static void ensureInstallNatives(Path localPath, Path assetPath) {
        if (Files.exists(localPath)) {
            LOGGER.debug("Natives is already installed! Skipping extract...");
            return;
        }

        LOGGER.info("Natives are not installed! Extracting...");

        try(var inputStream = ModSetup.class.getResourceAsStream(assetPath.toString())) {
            if(inputStream == null)
                throw new RuntimeException("Asset: '%s' was not found".formatted(assetPath));

            Files.createDirectories(localPath.getParent());
            Files.copy(inputStream, localPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
