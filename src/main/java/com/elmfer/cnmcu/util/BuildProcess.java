package com.elmfer.cnmcu.util;

import com.elmfer.cnmcu.cpp.NativesLoader;
import com.elmfer.cnmcu.mcu.Toolchain;
import net.minecraft.util.Util;
import org.lwjgl.system.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;

import static com.elmfer.cnmcu.CodeNodeMicrocontrollers.LOGGER;

public class BuildProcess {
    private final StringBuffer output;
    private final Toolchain.ToolchainConfig config;
    private final CompletableFuture<Process> processFuture = new CompletableFuture<>();
    private final CompletableFuture<byte[]> future = new CompletableFuture<>();
    public BuildProcess(StringBuffer output, Toolchain.ToolchainConfig config) {
        this.output = output;
        this.config = config;
    }
    public void start(String code) {
        future.completeAsync(() -> {
            final var workingDirectory = config.getWorkingDirectory();
            final var inputFile = workingDirectory.resolve(config.getInputPath().orElseThrow(
                    () -> new NoSuchElementException("Input build variable not set")));

            final var outputFile = workingDirectory.resolve(config.getOutputPath().orElseThrow(
                    () -> new NoSuchElementException("Output build variable not set")));

            try {
                Files.writeString(inputFile, code);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            final var shell = NativesLoader.PLATFORM == Platform.WINDOWS ? "cmd" : "sh";
            final var shellFlag = NativesLoader.PLATFORM == Platform.WINDOWS ? "/c" : "-c";
            var buildCommand = config.getBuildCommand();

            for (final var entry : config.getBuildVariables().entrySet()) {
                buildCommand = buildCommand.replace("${" + entry.getKey() + "}",
                        Matcher.quoteReplacement(entry.getValue()));
            }

            final var builder = new ProcessBuilder(shell, shellFlag, buildCommand);
            builder.directory(workingDirectory.toFile());
            builder.redirectErrorStream(true);
            builder.environment().putAll(config.getEnvironmentVariables());

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

    public CompletableFuture<byte[]> onFinish() {
        return Objects.requireNonNullElseGet(future, CompletableFuture::new);
    }

    public void cancel() {
        future.cancel(true);
        if(processFuture.isDone())
            processFuture.join().destroy();
    }
}
