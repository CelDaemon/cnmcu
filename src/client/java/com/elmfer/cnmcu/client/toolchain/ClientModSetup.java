package com.elmfer.cnmcu.client.toolchain;

import com.elmfer.cnmcu.config.ModSetup;
import com.elmfer.cnmcu.natives.NativesLoader;
import org.lwjgl.system.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import static com.elmfer.cnmcu.common.Common.LOGGER;

public class ClientModSetup {
    private ClientModSetup() {
    }

    public static void createDirectories() {
        try {
            Files.createDirectories(Toolchain.TOOLCHAIN_PATH);
            Files.createDirectories(Toolchain.BUILD_PATH);
            Files.createDirectories(Sketches.BACKUP_PATH);
        } catch (IOException e) {
            LOGGER.error("Failed to create directories", e);
        }
    }

    public static void downloadToolchain() {
        final var vasmFilename = "vasm6502_oldstyle";
        ensureInstall(vasmFilename, Toolchain.TOOLCHAIN_PATH.resolve(NativesLoader.getExecutablePath(vasmFilename)),
                NativesLoader.getExecutableFilename(vasmFilename));

        final var vobjFilename = "vobjdump";
        ensureInstall(vobjFilename, Toolchain.TOOLCHAIN_PATH.resolve(NativesLoader.getExecutablePath(vobjFilename)),
                NativesLoader.getExecutableFilename(vobjFilename));

        final var cygFilename = "cygwin1.dll";
        if (NativesLoader.PLATFORM == Platform.WINDOWS)
            ensureInstall(cygFilename, Toolchain.TOOLCHAIN_PATH.resolve(cygFilename),
                    cygFilename);
    }

    private static void ensureInstall(String moduleName, Path localPath, String assetName) {
        if (Files.exists(localPath)) {
            LOGGER.debug("{} is already installed! Skipping download...", moduleName);
            return;
        }

        LOGGER.info("{} is not installed! Downloading...", moduleName);

        var rawBinary = ModSetup.getGitHubAsset(assetName);

        if (rawBinary == null)
            throw new RuntimeException("Failed to download " + moduleName + "!");

        try {
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-xr-x");
            FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(perms);
            Files.createDirectories(localPath.getParent());
            Files.createFile(localPath, attr);

            Files.write(localPath, rawBinary);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write " + moduleName + " to disk!", e);
        }
    }
}
