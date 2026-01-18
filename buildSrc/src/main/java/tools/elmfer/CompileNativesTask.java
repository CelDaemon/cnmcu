package tools.elmfer;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.process.ExecOperations;

import javax.inject.Inject;
import java.util.Arrays;

public abstract class CompileNativesTask extends DefaultTask {

    @InputDirectory
    public abstract DirectoryProperty getSourceDir();
    @InputDirectory
    public abstract DirectoryProperty getBridgeDir();
    @Input
    public abstract Property<String> getCmakeTarget();
    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();
    @Input
    public abstract Property<String> getBuildType();

    private final ExecOperations execOperations;

    @Inject
    public CompileNativesTask(ExecOperations execOperations) {
        this.execOperations = execOperations;

        setGroup("build");
        setDescription("Compiles native source files using CMake.");

        getBuildType().convention("Release");
        getCmakeTarget().convention("cnmcu-natives");
    }

    @TaskAction
    void execute() {
        final var project = getProject();
        final var sourceDir = getSourceDir();
        final var bridgeDir = getBridgeDir();
        final var outputDir = getOutputDir();
        final var buildType = getBuildType();
        final var cmakeTarget = getCmakeTarget();

        if (!sourceDir.isPresent())
            throw new RuntimeException("You must specify source directory for generating native source files!");

        if (!outputDir.isPresent())
            throw new RuntimeException("You must specify output directory for generating native source files!");

        final var absSourceDir = project.file(sourceDir).getAbsolutePath();
        final var absBuildDir = getTemporaryDir().getAbsolutePath();
        final var absBridgeDir = project.file(bridgeDir).getAbsolutePath();
        final var absOutputDir = project.file(outputDir).getAbsolutePath();

        cmakeExec("-S", absSourceDir, "-B", absBuildDir, "-DCMAKE_BUILD_TYPE=" + buildType.get(),
                "-DGENERATED_SOURCES_DIR=" + absBridgeDir);

        cmakeExec("--build", absBuildDir, "--parallel", "4", "--target", cmakeTarget.get(),
                "--config", buildType.get());

        cmakeExec("--install", absBuildDir, "--prefix", absOutputDir, "--strip");
    }

    private void cmakeExec(String... args) {
        execOperations.exec(innerAction -> {
            innerAction.setExecutable("cmake");
            innerAction.args(Arrays.asList(args));
        });
    }
}
