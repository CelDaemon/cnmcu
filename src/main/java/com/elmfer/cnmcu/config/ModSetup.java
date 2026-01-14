package com.elmfer.cnmcu.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import net.minecraft.resources.Identifier;
import com.elmfer.cnmcu.CodeNodeMicrocontrollers;
import com.elmfer.cnmcu.cpp.NativesLoader;
import com.elmfer.cnmcu.mcu.Sketches;
import com.elmfer.cnmcu.mcu.Toolchain;
import com.elmfer.cnmcu.util.HTTPSFetcher;
import com.elmfer.cnmcu.util.ResourceLoader;
import com.google.gson.JsonArray;

import static com.elmfer.cnmcu.CodeNodeMicrocontrollers.LOGGER;

public final class ModSetup {

    public static final String IMGUI_INI_FILE = CodeNodeMicrocontrollers.MOD_ID + "/imgui.ini";

    private static final String GITHUB_REPO_URL = "https://api.github.com/repos/elmfrain/cnmcu";

    private static JsonArray githubAssets;
    
    private ModSetup() {
    }

    public static void createDirectories() {
        try {
            Files.createDirectories(Toolchain.TOOLCHAIN_PATH);
            Files.createDirectories(Toolchain.TEMP_PATH);
            Files.createDirectories(NativesLoader.BINARIES_PATH);
            Files.createDirectories(Sketches.BACKUP_PATH);
        } catch (IOException e) {
            LOGGER.error("Failed to create directories", e);
        }
    }

    public static void imguiIniFile() {
        final Identifier imguiIniId = CodeNodeMicrocontrollers.id("setup/imgui.ini");

        Path configPath = Paths.get(IMGUI_INI_FILE);
        if (Files.exists(configPath))
            return;

        try {
            InputStream imguiIni = ResourceLoader.getInputStream(imguiIniId);
            Files.copy(imguiIni, configPath);

            imguiIni.close();
        } catch (Exception e) {
            LOGGER.error("Failed to write defaint imgui config file", e);
        }
    }

    public static void downloadNatives() {
        ensureInstallNatives(NativesLoader.BINARIES_PATH.resolve(NativesLoader.getBinaryFilename()),
                NativesLoader.getBinaryFilename());
    }

    public static void downloadToolchain() {
        final String vasmFilename = "vasm6502_oldstyle";
        ensureInstall("vasm", Toolchain.TOOLCHAIN_PATH.resolve(vasmFilename + NativesLoader.EXE_EXT),
                NativesLoader.getExecutableFilename(vasmFilename));

        final String vobjFilename = "vobjdump";
        ensureInstall("vobjdump", Toolchain.TOOLCHAIN_PATH.resolve(vobjFilename + NativesLoader.EXE_EXT),
                NativesLoader.getExecutableFilename(vobjFilename));

        final String cygFilename = "cygwin1.dll";
        if (NativesLoader.NATIVES_OS.equals("windows"))
            ensureInstall("cygwin1.dll", Toolchain.TOOLCHAIN_PATH.resolve(cygFilename),
                    "cygwin1.dll");
    }

    private static byte[] getGitHubAsset(String assetNameTarget) {
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
                GITHUB_REPO_URL + "/releases/tags/" + "0.0.10a-1.20.4"); // + CodeNodeMicrocontrollers.MOD_VERSION
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

    private static void ensureInstallNatives(Path localPath, String assetName) {
        if (Files.exists(localPath)) {
            LOGGER.debug("Natives is already installed! Skipping extract...");
            return;
        }

        LOGGER.info("Natives are not installed! Extracting...");

        try(var inputStream = ModSetup.class.getResourceAsStream(assetName)) {
            if(inputStream == null)
                throw new RuntimeException("Asset: '%s' was not found".formatted(assetName));

            Files.copy(inputStream, localPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void ensureInstall(String moduleName, Path localPath, String assetName) {
        if (Files.exists(localPath)) {
            LOGGER.debug("{} is already installed! Skipping download...", moduleName);
            return;
        }

        LOGGER.info("{} is not installed! Downloading...", moduleName);

        var rawBinary = getGitHubAsset(assetName);

        if (rawBinary == null)
            throw new RuntimeException("Failed to download " + moduleName + "!");

        try {
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-xr-x");
            FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(perms);
            Files.createFile(localPath, attr);

            Files.write(localPath, rawBinary);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write " + moduleName + " to disk!", e);
        }
    }

}
