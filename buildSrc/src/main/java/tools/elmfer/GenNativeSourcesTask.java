package tools.elmfer;

import com.badlogic.gdx.jnigen.NativeCodeGenerator;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.*;
import org.gradle.internal.file.Deleter;

import javax.inject.Inject;

public abstract class GenNativeSourcesTask extends DefaultTask {
    
    public static final String GROUP = "build";
    public static final String DESCRIPTION = "Creates JNI bridge .h and .cpp files.";

    @CompileClasspath
    public abstract ConfigurableFileCollection getClassPath();
    @InputDirectory
    public abstract DirectoryProperty getSourceDir();
    @OutputDirectory
    public abstract DirectoryProperty getBridgeDir();

    private final Deleter deleter;

    @Inject
    public GenNativeSourcesTask(Deleter deleter) {
        this.deleter = deleter;

        setGroup(GROUP);
        setDescription(DESCRIPTION);

        final var project = getProject();

        final var mainSourceSet = project.getExtensions().getByType(JavaPluginExtension.class)
                .getSourceSets()
                .getByName(SourceSet.MAIN_SOURCE_SET_NAME);

        getClassPath().convention(project.files(mainSourceSet.getCompileClasspath(),
                mainSourceSet.getOutput().getClassesDirs()));

        getBridgeDir().convention(project.getLayout().getBuildDirectory().dir("generated/sources/cpp"));
    }
    
    @TaskAction
    void execute() throws Exception {
        final var sourceDir = getSourceDir();
        final var bridgeDir = getBridgeDir();
        final var classPath = getClassPath();
        if(!sourceDir.isPresent())
            throw new RuntimeException("You must specify source directory for generating native source files!");
        
        if (!bridgeDir.isPresent())
            throw new RuntimeException("You must specify bridge directory for generating native source files!");

        deleter.ensureEmptyDirectory(bridgeDir.getAsFile().get());

        final var srcGen = new NativeCodeGenerator();
        srcGen.generate(getSourceDir().get().getAsFile().getAbsolutePath(), classPath.getAsPath(), getBridgeDir().get().getAsFile().getAbsolutePath());
    }
}
