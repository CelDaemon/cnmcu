package com.elmfer.cnmcu.mcu;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;

import com.elmfer.cnmcu.CodeNodeMicrocontrollers;
import com.elmfer.cnmcu.cpp.NativesLoader;
import com.google.gson.*;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import imgui.ImGui;
import imgui.type.ImString;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Util;

import static com.elmfer.cnmcu.CodeNodeMicrocontrollers.LOGGER;

public class Toolchain {

	public static final Path TOOLCHAIN_PATH = CodeNodeMicrocontrollers.DATA_PATH
            .resolve("toolchain");
	public static final Path TEMP_PATH = TOOLCHAIN_PATH.resolve("temp");
	public static final Path CONFIG_PATH = TOOLCHAIN_PATH.resolve("config.json");

	private final StringBuffer buildStdout = new StringBuffer();
    private ToolchainConfig config = ToolchainConfig.load();
	
	public void loadDefaults() {
	    config = ToolchainConfig.defaultConfig();
	}
    public void reloadConfig() {
        config = ToolchainConfig.load();
    }
    public CompletableFuture<Void> saveConfig() {
        return config.save();
    }
    public Optional<Path> getInputPath() {
        final var value = config.buildVariables.get("input");
        return Optional.ofNullable(value).map(Path::of);
    }
    public Optional<Path> getOutputPath() {
        final var value = config.buildVariables.get("output");
        return Optional.ofNullable(value).map(Path::of);
    }
	public CompletableFuture<byte[]> build(String code) {
		CompletableFuture<byte[]> future = new CompletableFuture<>();


		CompletableFuture.runAsync(() -> {
			try {
                final var workingDirectory = config.workingDirectory;
                final var inputFile = getInputPath().orElseThrow(
                        () -> new NoSuchElementException("Input build variable not set"));

                final var outputFile = getOutputPath().orElseThrow(
                        () -> new NoSuchElementException("Output build variable not set"));

				Files.writeString(inputFile, code);

				final var shell = NativesLoader.NATIVES_OS.equals("windows") ? "cmd" : "sh";
                final var shellFlag = NativesLoader.NATIVES_OS.equals("windows") ? "/c" : "-c";
                var buildCommand = config.buildCommand;
				
                for (final var entry : config.buildVariables.entrySet()) {
                    buildCommand = buildCommand.replace("${" + entry.getKey() + "}",
                            Matcher.quoteReplacement(entry.getValue()));
                }

				final var builder = new ProcessBuilder(shell, shellFlag, buildCommand);
				builder.directory(workingDirectory.toFile());
				builder.redirectErrorStream(true);
                builder.environment().putAll(config.environmentVariables);

				final var process = builder.start();

				final var outThread = new Thread(() -> {
					try (final var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
						String line;
						while ((line = reader.readLine()) != null)
							appendBuildStdout(line);
					} catch (Exception e) {
						appendBuildStdout("build", "Failed to read build output");
					}
				});
				outThread.start();

				int exitCode = process.waitFor();
				outThread.join();

				if (exitCode != 0) {
                    future.completeExceptionally(new Exception("Build failed"));

                    appendBuildStdout("build", "Build failed");
                    return;

				}

                final var output = Files.readAllBytes(outputFile);

                future.complete(output);

                appendBuildStdout("build", "Build successful");
			} catch (Exception e) {
				future.completeExceptionally(e);

				appendBuildStdout("build", "Build failed with exception: \n" + e);

                LOGGER.error("Build failed with exception", e);
			}
		}, Util.backgroundExecutor());

		return future;
	}

	public String getBuildStdout() {
		return buildStdout.toString();
	}

	public void appendBuildStdout(String module, String output) {
		buildStdout.append("[").append(module).append("] ").append(output).append("\n");
	}

	public void appendBuildStdout(String output) {
		buildStdout.append(output).append("\n");
	}

	public void clearBuildStdout() {
		buildStdout.setLength(0);
	}
    
    private final ImString buildCommand = new ImString(config.buildCommand, 2048);
    private final ImString workingDirectory = new ImString(config.workingDirectory.toString(), 2048);
    public void genToolchainConfigUI() {
        float windowWidth = ImGui.getContentRegionAvailX();
        buildCommand.set(config.buildCommand);
        workingDirectory.set(config.workingDirectory.toString());
        
        ImGui.text("Build Command");
        ImGui.setNextItemWidth(windowWidth);
        if (ImGui.inputText("##Build Command", buildCommand))
            config.setBuildCommand(buildCommand.get());
        
        ImGui.text("Working Directory");
        ImGui.setNextItemWidth(windowWidth);
        
        if (ImGui.inputText("##Working Directory", workingDirectory))
            config.setWorkingDirectory(Path.of(workingDirectory.get()));
        ImGui.newLine();
        
        if (ImGui.collapsingHeader("Build Variables"))
            genBuildVariables();
        
        if (ImGui.collapsingHeader("Environment Variables"))
            genEnvVariables();
    }
    
    private final ImString newBuildVariableName = new ImString("", 64);
    private ImString[] buildVariablesInputs = new ImString[0];
    private boolean showBuildVariableWarning = false;
    private void genBuildVariables() {
        float windowWidth = ImGui.getContentRegionAvailX();
        
        ImGui.indent();
        
        ImGui.textWrapped("Create and use build variables for them to be use in your build command."
                + " You can use the variables in your build command by wrapping the variable name in ${}.");
        ImGui.newLine();
        
        if (buildVariablesInputs.length != config.buildVariables.size())
            buildVariablesInputs = new ImString[config.buildVariables.size()];
        
        int i = 0;
        final var buildVariableIterator = config.buildVariables.entrySet().iterator();
        while (buildVariableIterator.hasNext()) {
            final var entry = buildVariableIterator.next();

            if (buildVariablesInputs[i] == null)
                buildVariablesInputs[i] = new ImString(entry.getValue(), 1024);
            buildVariablesInputs[i].set(entry.getValue());

            if (ImGui.inputText(entry.getKey() + "##BuildVar" + i, buildVariablesInputs[i]))
                config.buildVariables.put(entry.getKey(), buildVariablesInputs[i].get());
            ImGui.sameLine();
            ImGui.setCursorPosX(windowWidth - 7);
            if (ImGui.button("x##BuildVar" + entry.getKey()))
                buildVariableIterator.remove();

            i++;
        }
        
        ImGui.text("New Build Variable");
        if (ImGui.inputText("Name##BuildVar", newBuildVariableName))
            showBuildVariableWarning = true;
        ImGui.sameLine();
        ImGui.setCursorPosX(windowWidth - 7);
        ImGui.beginDisabled(newBuildVariableName.isEmpty() || config.buildVariables.containsKey(newBuildVariableName.get()));
        if (ImGui.button("+##BuildVar")) {
            config.buildVariables.put(newBuildVariableName.get(), "");
            newBuildVariableName.set("");
            showBuildVariableWarning = false;
        }
        ImGui.endDisabled();
        if (showBuildVariableWarning) {
            if (newBuildVariableName.isEmpty())
                ImGui.textColored(0xFF8888FF, "Name cannot be empty");
            else if (config.buildVariables.containsKey(newBuildVariableName.get()))
                ImGui.textColored(0xFF8888FF, "Name already exists");
            else
                ImGui.newLine();
        } else
            ImGui.newLine();
        
        ImGui.unindent();
    }
    
    private final ImString newEnvVariableName = new ImString("", 64);
    private ImString[] envVariablesInputs = new ImString[config.environmentVariables.size()];
    private boolean showEnvVariableWarning = false;
    private void genEnvVariables() {
        float windowWidth = ImGui.getContentRegionAvailX();

        ImGui.indent();

        ImGui.textWrapped("Create and use environment variables for your command's process."
                + " They will also apply the child processes of the command.");
        ImGui.newLine();

        if (envVariablesInputs.length != config.environmentVariables.size())
            envVariablesInputs = new ImString[config.environmentVariables.size()];

        int i = 0;
        for (final var entry : config.environmentVariables.entrySet()) {
            if (envVariablesInputs[i] == null)
                envVariablesInputs[i] = new ImString(entry.getValue(), 1024);

            if (ImGui.inputText(entry.getKey() + "##EnvVar" + i, envVariablesInputs[i]))
                config.environmentVariables.put(entry.getKey(), envVariablesInputs[i].get());
            ImGui.sameLine();
            ImGui.setCursorPosX(windowWidth - 7);
            if (ImGui.button("x##EnvVar" + entry.getKey()))
                config.environmentVariables.remove(entry.getKey());

            i++;
        }

        ImGui.text("New Environment Variable");
        if (ImGui.inputText("Name##EnvVar", newEnvVariableName))
            showEnvVariableWarning = true;
        ImGui.sameLine();
        ImGui.setCursorPosX(windowWidth - 7);
        ImGui.beginDisabled(newEnvVariableName.isEmpty() || config.environmentVariables.containsKey(newEnvVariableName.get()));
        if (ImGui.button("+##EnvVar")) {
            config.environmentVariables.put(newEnvVariableName.get(), "");
            newEnvVariableName.set("");
            showEnvVariableWarning = false;
        }
        ImGui.endDisabled();
        if (showEnvVariableWarning) {
            if (newEnvVariableName.isEmpty())
                ImGui.textColored(0xFF8888FF, "Name cannot be empty");
            else if (config.environmentVariables.containsKey(newEnvVariableName.get()))
                ImGui.textColored(0xFF8888FF, "Name already exists");
            else
                ImGui.newLine();
        } else
            ImGui.newLine();

        ImGui.unindent();
    }

    public static class ToolchainConfig {

        private static final Codec<ToolchainConfig> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("build_command").forGetter(ToolchainConfig::getBuildCommand),
                        Codec.STRING.xmap(Path::of, Path::toString).fieldOf("working_directory").forGetter(ToolchainConfig::getWorkingDirectory),
                        Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("build_variables").forGetter(ToolchainConfig::getBuildVariables),
                        Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("environment_variables").forGetter(ToolchainConfig::getEnvironmentVariables)
                ).apply(instance, ToolchainConfig::new));

        public String getBuildCommand() {
            return buildCommand;
        }

        public void setBuildCommand(String buildCommand) {
            this.buildCommand = buildCommand;
        }

        public Path getWorkingDirectory() {
            return workingDirectory;
        }

        public void setWorkingDirectory(Path workingDirectory) {
            this.workingDirectory = workingDirectory;
        }

        public Map<String, String> getBuildVariables() {
            return buildVariables;
        }

        public Map<String, String> getEnvironmentVariables() {
            return environmentVariables;
        }

        private String buildCommand;
        private Path workingDirectory;
        private final Map<String, String> buildVariables = new HashMap<>();
        private final Map<String, String> environmentVariables = new HashMap<>();

        public ToolchainConfig(String buildCommand, Path workingDirectory, Map<String, String> buildVariables, Map<String, String> environmentVariables) {
            this.buildCommand = buildCommand;
            this.workingDirectory = workingDirectory;
            this.buildVariables.putAll(buildVariables);
            this.environmentVariables.putAll(environmentVariables);
        }

        public static ToolchainConfig defaultConfig() {
            final var buildCommand = NativesLoader.NATIVES_OS.equals("windows") ? "vasm6502_oldstyle -Fbin -dotdir ${input} -o ${output}"
                    : "./vasm6502_oldstyle -Fbin -dotdir ${input} -o ${output}";


            final var buildVariables = Map.of(
                    "input", "temp/program.s",
                    "output", "temp/output.bin");

            return new ToolchainConfig(buildCommand, TOOLCHAIN_PATH, buildVariables, Map.of());
        }

        public CompletableFuture<Void> save() {
            final var element = CODEC.encodeStart(JsonOps.INSTANCE, this).getOrThrow();

            return CompletableFuture.runAsync(() -> {
                try(var writer = new JsonWriter(new OutputStreamWriter(Files.newOutputStream(CONFIG_PATH)))) {
                    writer.setFormattingStyle(FormattingStyle.PRETTY);
                    GsonHelper.writeValue(writer, element, null);
                } catch (IOException e) {
                    LOGGER.error("Failed to save config file", e);
                }
            }, Util.backgroundExecutor());
        }
        public static ToolchainConfig load() {
            final JsonElement element;
            try(var reader = new JsonReader(new InputStreamReader(Files.newInputStream(CONFIG_PATH)))) {
                element = JsonParser.parseReader(reader);
            } catch (Exception e) {
                LOGGER.error("Failed to load config file", e);
                return defaultConfig();
            }

            final var result = CODEC.parse(JsonOps.INSTANCE, element);
            return result.mapOrElse(x -> x,
                    x -> defaultConfig());
        }

    }
}
