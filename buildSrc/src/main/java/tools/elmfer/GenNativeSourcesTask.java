package tools.elmfer;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import com.badlogic.gdx.jnigen.NativeCodeGenerator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public abstract class GenNativeSourcesTask extends DefaultTask {
    
    public static final String GROUP = "build";
    public static final String DESCRIPTION = "Creates JNI bridge .h and .cpp files.";

    @Input
    public abstract Property<String> getClassPath();
    @InputDirectory
    public abstract DirectoryProperty getSourceDir();
    @OutputDirectory
    public abstract DirectoryProperty getBridgeDir();
    
    public GenNativeSourcesTask() {
        setGroup(GROUP);
        setDescription(DESCRIPTION);

        final var project = getProject();

        getClassPath().convention(project.provider(() -> {
            final var classPath = project.files(
                    project.getConfigurations().getByName("runtimeClasspath").getFiles(),
                    project.getExtensions().getByType(JavaPluginExtension.class)
                            .getSourceSets().getByName("main").getOutput().getFiles());

            return classPath.getAsPath();
        }));
    }
    
    @TaskAction
    void execute() throws Exception {
        final var logger = getLogger();
        final var sourceDir = getSourceDir();
        final var bridgeDir = getBridgeDir();
        final var classPath = getClassPath();
        if(!sourceDir.isPresent())
            throw new RuntimeException("You must specify source directory for generating native source files!");
        
        if (!bridgeDir.isPresent())
            throw new RuntimeException("You must specify bridge directory for generating native source files!");

        logger.warn("PATH: {}", System.getenv("PATH"));
        final var builder = new ProcessBuilder("javac", "--version");
        builder.redirectErrorStream(true);

        final var process = builder.start();

        final var outThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null)
                    System.out.println("[javac] " + line);
            } catch (IOException e) {
                logger.error("Failed to read cmake command output");
            }
        });
        outThread.start();

        final var exitCode = process.waitFor();
        outThread.join();
        final var srcGen = new NativeCodeGenerator();
        srcGen.generate(getSourceDir().get().getAsFile().getAbsolutePath(), classPath.get(), getBridgeDir().get().getAsFile().getAbsolutePath());
    }
}
