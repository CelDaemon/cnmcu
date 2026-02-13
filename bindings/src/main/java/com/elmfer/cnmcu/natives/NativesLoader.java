package com.elmfer.cnmcu.natives;

import org.apache.commons.io.file.PathUtils;
import org.lwjgl.system.Platform;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;

public final class NativesLoader {

    private NativesLoader() {
    }
    public static final Platform PLATFORM = Platform.get();
    public static final Platform.Architecture ARCHITECTURE = Platform.getArchitecture();
    public static final String EXE_EXT = PLATFORM == Platform.WINDOWS ? ".exe" : "";
    public static final Path PREFIX = Paths.get(
            PLATFORM.getName().toLowerCase(Locale.ROOT),
            ARCHITECTURE.name().toLowerCase(Locale.ROOT)
    );
    public static final Path EXTRACTED_PATH;
    private static final Path NATIVE = Path.of(System.mapLibraryName("cnmcu"));

    static {
        try {
            EXTRACTED_PATH = Files.createTempDirectory("cnmcu");
            Runtime.getRuntime().addShutdownHook(new Thread(NativesLoader::shutdown));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void shutdown() {
        try {
            PathUtils.deleteDirectory(EXTRACTED_PATH);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void extractNative(Path path) {
        try (final var stream = NativesLoader.class.getResourceAsStream(PREFIX.resolve(path.toString()).toString())) {
            if (stream == null)
                throw new FileNotFoundException();
            Files.copy(stream, EXTRACTED_PATH.resolve(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void extractNatives() {
        extractNative(NATIVE);

    }

    public static void loadNatives() {
        CNMCUNatives.LOGGER.debug("Loading native library...");

        var nativePath = EXTRACTED_PATH.resolve(resolveNative());

        try {
            System.load(nativePath.toString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load native library: " + nativePath, e);
        }
    }

    public static Path resolveNative() {
        return EXTRACTED_PATH.resolve(NATIVE);
    }

    public static String getExecutableFilename(String name) {
        final var arch = ARCHITECTURE.name().toLowerCase(Locale.ROOT);
        final var platform = PLATFORM.getName().toLowerCase(Locale.ROOT);
        final var ext = Optional.of(EXE_EXT)
                .filter(x -> !x.isEmpty())
                .map(x -> "." + x)
                .orElse("");
        return name + "-" + platform + "-" + arch + ext;
    }

    public static Path getExecutablePath(String name) {
        final var ext = Optional.of(EXE_EXT)
                .filter(x -> !x.isEmpty())
                .map(x -> "." + x)
                .orElse("");
        return Path.of(ARCHITECTURE.name().toLowerCase(Locale.ROOT), name + ext);
    }
}
