package tools.elmfer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

public abstract class CompileNativesTask extends DefaultTask {

    private static final Predicate<Path> LIBS_FILTER = path -> {
        final var name = path.getFileName().toString();
        return name.endsWith(".so") || name.endsWith(".dll") || name.endsWith(".dylib");
    };

    @InputDirectory
    public abstract DirectoryProperty getSourceDir();
    @OutputDirectory
    public abstract DirectoryProperty getBuildDir();
    @Input
    public abstract Property<String> getCmakeTarget();
    @OutputDirectory
    public abstract DirectoryProperty getTargetDir();
    @Input
    public abstract Property<String> getBuildType();

    public CompileNativesTask() {
        setGroup("build");
        setDescription("Compiles native source files using CMake.");

        getBuildType().convention("Release");
        getCmakeTarget().convention("cnmcu-natives");
    }

    @TaskAction
    void execute() throws Exception {
        final var project = getProject();
        final var sourceDir = getSourceDir();
        final var buildDir = getBuildDir();
        final var buildType = getBuildType();
        final var cmakeTarget = getCmakeTarget();
        if (!sourceDir.isPresent())
            throw new RuntimeException("You must specify source directory for generating native source files!");

        if (!buildDir.isPresent())
            throw new RuntimeException("You must specify build directory for generating native source files!");

        if(!executeCommand("cmake", "--version"))
            throw new RuntimeException("CMake is not installed on your system!");

        final var absSourceDir = project.file(sourceDir).getAbsolutePath();
        final var absBuildDir = project.file(buildDir).getAbsolutePath();

        if(!executeCommand("cmake", "-S", absSourceDir, "-B", absBuildDir, "-DCMAKE_BUILD_TYPE=" + buildType.get()))
            throw new RuntimeException("Error configuring CMake project!");

        if(!executeCommand("cmake", "--build", absBuildDir, "--parallel", "4", "--target", cmakeTarget.get(), "--config", buildType.get()))
                throw new RuntimeException("Error compiling native source files!");

        final var inProduction = System.getenv("PRODUCTION") != null;
        if (!inProduction && getTargetDir().isPresent())
            copyBinaries();
    }

    private boolean executeCommand(String ...args) throws Exception {
        final var logger = getLogger();

        final var builder = new ProcessBuilder(args);
        builder.redirectErrorStream(true);

        final var process = builder.start();

        final var outThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null)
                    System.out.println("[cmake] " + line);
            } catch (IOException e) {
                logger.error("Failed to read cmake command output");
            }
        });
        outThread.start();

        final var exitCode = process.waitFor();
        outThread.join();

        return exitCode == 0;
    }

    /**
     * Copy binaries to target directory, if specified.
     * This is useful when you want to copy the compiled binaries to
     * quickly to aid in development.
     */
    private void copyBinaries() throws IOException {
        final var project = getProject();
        final var copyError = new AtomicReference<IOException>();

        final var targetPath = project.file(getTargetDir()).toPath();
        final var buildDir = project.file(getBuildDir()).toPath();

        try(var walker = Files.walk(buildDir)) {
            walker.filter(LIBS_FILTER).forEach(source -> {
                try {
                    final var target = targetPath.resolve(source.getFileName());

                    Files.createDirectories(target.getParent());
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    copyError.set(e);
                }
            });
        }
        if (copyError.get() != null)
            throw copyError.get();
    }
}
