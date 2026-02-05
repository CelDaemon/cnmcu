package com.elmfer.cnmcu.client.toolchain;

import com.elmfer.cnmcu.client.config.ToolchainConfig;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static com.elmfer.cnmcu.CNMCU.LOGGER;

public class BuildProcess {
    private final StringBuffer output;
    private final ToolchainConfig config;
    private final CompletableFuture<Process> processFuture = new CompletableFuture<>();
    private final CompletableFuture<byte[]> future = new CompletableFuture<>();

    public BuildProcess(StringBuffer output, ToolchainConfig config) {
        this.output = output;
        this.config = config;
    }
    public void start(String code) {
        future.completeAsync(() -> {
            final var workingDirectory = Toolchain.BUILD_PATH.resolve(config.getWorkingDirectory());
            final var inputFile = workingDirectory.resolve(config.getInputPath());

            final var outputFile = workingDirectory.resolve(config.getOutputPath());

            try {
                Files.writeString(inputFile, code);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            final var builder = getProcessBuilder(inputFile, outputFile, workingDirectory);

            final Process process;

            try {
                process = builder.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            processFuture.complete(process);

            try (final var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null)
                    output.append(line).append('\n');
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            final int exitCode;
            try {
                exitCode = process.waitFor();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            if (exitCode != 0) {
                throw new RuntimeException("Build failed");
            }

            try {
                return Files.readAllBytes(outputFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, Util.backgroundExecutor()).exceptionally(e -> {
            if(future.isCancelled())
                throw new RuntimeException(e);

            LOGGER.error("Build failed with exception", e);

            output.append("[build] Build failed with exception: ").append(e).append('\n');

            throw new RuntimeException(e);
        });
    }

    private @NonNull ProcessBuilder getProcessBuilder(Path inputFile, Path outputFile, Path workingDirectory) {
        var arguments = config.getArguments();

        arguments = arguments.replace("${input}", inputFile.toString());
        arguments = arguments.replace("${output}", outputFile.toString());

        for (final var entry : config.getVariables().entrySet()) {
            arguments = arguments.replace("${" + entry.getKey() + "}",
                    entry.getValue());
        }
        final var builder = new ProcessBuilder(Toolchain.TOOLCHAIN_PATH.resolve(config.getExecutable()).toString());
        builder.command().addAll(Arrays.asList(arguments.split("\\s+")));
        builder.directory(workingDirectory.toFile());
        builder.environment().putAll(config.getEnvironment());
        builder.redirectErrorStream(true);
        return builder;
    }

    public CompletableFuture<byte[]> onFinish() {
        return future;
    }

    public void cancel() {
        future.cancel(true);
        if(processFuture.isDone())
            processFuture.join().destroy();
    }
}
