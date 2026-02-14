package com.elmfer.cnmcu.config;

import com.elmfer.cnmcu.Initialiser;
import com.elmfer.cnmcu.common.Common;
import com.elmfer.cnmcu.util.HTTPSFetcher;
import com.google.gson.JsonArray;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.elmfer.cnmcu.common.Common.LOGGER;

public final class ModSetup {
    public static final Path IMGUI_CONFIG_PATH = Initialiser.DATA_PATH.resolve("imgui.ini");

    private static final String GITHUB_REPO_URL = "https://api.github.com/repos/elmfrain/cnmcu";

    private static JsonArray githubAssets;
    
    private ModSetup() {
    }

    public static void copyDefaultImGuiConfig(@NotNull ResourceManager resourceLoader) {
        if (Files.exists(IMGUI_CONFIG_PATH))
            return;

        final var resource = resourceLoader.getResource(Common.id("setup/imgui.ini"))
                .orElseThrow();

        try(final var stream = resource.open()) {
            Files.copy(stream, IMGUI_CONFIG_PATH);
        } catch (Exception e) {
            LOGGER.error("Failed to write default imgui config file", e);
        }
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

}
