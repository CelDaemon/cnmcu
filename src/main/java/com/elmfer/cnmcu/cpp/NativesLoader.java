package com.elmfer.cnmcu.cpp;

import com.elmfer.cnmcu.CodeNodeMicrocontrollers;
import org.lwjgl.system.Platform;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

import static com.elmfer.cnmcu.CodeNodeMicrocontrollers.LOGGER;

public final class NativesLoader {

    private NativesLoader() {
    }
    public static final Platform PLATFORM = Platform.get();
    public static final Platform.Architecture ARCHITECTURE = Platform.getArchitecture();
    public static final String EXE_EXT = PLATFORM == Platform.WINDOWS ? ".exe" : "";
    public static final Path NATIVES_PATH = CodeNodeMicrocontrollers.DATA_PATH.resolve("natives")
            .resolve(CodeNodeMicrocontrollers.MOD_VERSION);

    public static void loadNatives() {
        LOGGER.debug("Loading native library...");

        var nativePath = NATIVES_PATH.resolve(resolveNative());

        try {
            System.load(nativePath.toString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load native library: " + nativePath, e);
        }
    }

    public static Path resolveNative() {
        return Path.of(ARCHITECTURE.name().toLowerCase(Locale.ROOT),
                System.mapLibraryName(CodeNodeMicrocontrollers.MOD_ID));
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
