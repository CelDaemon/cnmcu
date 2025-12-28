package tools.elmfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class SaveVersionTask extends DefaultTask {

    public static final String GROUP = "build";
    public static final String DESCRIPTION = "Saves the version of the project to a list of files.";

    private static final FileEntry[] FILES = {

            new FileEntry("src/main/java/com/elmfer/cnmcu/CodeNodeMicrocontrollers.java",
                    new Entry("public static final String MOD_VERSION", ";",
                            "public static final String MOD_VERSION = \"%s-%s\"", "version", "mc_version")),

            new FileEntry("src/main/resources/fabric.mod.json",
                    new Entry("\"version\":", ",",
                            "\"version\": \"%s-%s\"", "version", "mc_version"),
                    new Entry("\"minecraft\":", ",",
                            "\"minecraft\": \"~%s\"", "mc_version")),

            new FileEntry("gradle.properties",
                    new Entry("mod_version=", "\n",
                            "mod_version=%s-%s", "version", "mc_version")),
    };

    private final Map<String, String> params = new HashMap<>();

    // @formatter:off

    // @formatter:on

    public SaveVersionTask() {
        setGroup(GROUP);
        setDescription(DESCRIPTION);
    }

    @TaskAction
    public void execute() {
        final var project = getProject();
        final var env = System.getenv();
        final var logger = getLogger();

        params.put("mc_version", env.getOrDefault("MINECRAFT_VERSION", (String) project.property("minecraft_version")));
        params.put("version", env.getOrDefault( "MOD_VERSION", (String) project.property("mod_version")));

        for (final var file : FILES) {
            final var f = project.file(file.path);
            
            if(!f.exists()) {
                logger.warn("File not found: {}, skipping...", file.path);
                continue;
            }

            var content = readFile(f);

            for (var entry : file.entries) {
                var start = entry.start;
                var end = entry.end;

                var value = entry.format(params);
                content = content.replaceAll(start + ".*?" + end, value + end);
            }

            writeFile(f, content);
        }
    }

    private static String readFile(File file) {
        try {
            int fileSize = (int) file.length();
            byte[] buffer = new byte[fileSize];

            final var fis = new FileInputStream(file);
            fis.read(buffer);
            fis.close();

            return new String(buffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static void writeFile(File file, String content) {
        try {
            Files.write(file.toPath(), content.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private record Entry(String start, String end, String format, String... params) {
        
        String format(Map<String, String> globalParams) {
            final var params = new ArrayList<>();
            
            for(final var param : this.params)
                params.add(globalParams.get(param));

            return format.formatted(params.toArray());
        }
    }

    private record FileEntry(String path, Entry... entries) {}
}
