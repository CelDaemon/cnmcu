package com.elmfer.cnmcu.client.toolchain;


import com.elmfer.cnmcu.CNMCU;
import com.elmfer.cnmcu.client.config.ToolchainConfig;
import imgui.ImGui;
import imgui.type.ImString;

import java.nio.file.Path;

public class Toolchain {

	public static final Path TOOLCHAIN_PATH = CNMCU.DATA_PATH
            .resolve("toolchain");
    public static final Path BUILD_PATH = TOOLCHAIN_PATH.resolve("build");

	private final StringBuffer buildStdout = new StringBuffer();
    private final ToolchainConfig config;
    private final ImString executable;
    private final ImString arguments;
	public BuildProcess build(String code) {
        final var process = new BuildProcess(buildStdout, config);
        process.start(code);
        return process;
	}

	public String getBuildStdout() {
		return buildStdout.toString();
	}

	public void appendBuildStdout(String module, String output) {
		buildStdout.append("[").append(module).append("] ").append(output).append("\n");
	}

	public void clearBuildStdout() {
		buildStdout.setLength(0);
	}
    private final ImString workingDirectory;
    private final ImString inputPath;
    private final ImString outputPath;
    private ImString[] envVariablesInputs;
    public Toolchain(ToolchainConfig config) {
        this.config = config;

        executable = new ImString(config.getExecutable().toString());
        executable.inputData.isResizable = true;
        arguments = new ImString(config.getArguments());
        arguments.inputData.isResizable = true;
        workingDirectory = new ImString(config.getWorkingDirectory().toString());
        workingDirectory.inputData.isResizable = true;
        inputPath = new ImString(config.getInputPath().toString());
        inputPath.inputData.isResizable = true;
        outputPath = new ImString(config.getOutputPath().toString());
        outputPath.inputData.isResizable = true;

        envVariablesInputs = new ImString[config.getEnvironment().size()];
    }

    public Path getInputPath() {
        return config.getInputPath();
    }
    
    private final ImString newBuildVariableName = new ImString("", 64);
    private ImString[] buildVariablesInputs = new ImString[0];
    private boolean showBuildVariableWarning = false;

    public void genToolchainConfigUI() {
        float windowWidth = ImGui.getContentRegionAvailX();
        executable.set(config.getExecutable().toString());
        arguments.set(config.getArguments());
        workingDirectory.set(config.getWorkingDirectory().toString());

        ImGui.text("Executable");
        ImGui.setNextItemWidth(windowWidth);
        if (ImGui.inputText("##Executable", executable))
            config.setExecutable(Path.of(executable.get()));

        ImGui.text("Arguments");
        ImGui.setNextItemWidth(windowWidth);
        if (ImGui.inputText("##Arguments", arguments))
            config.setArguments(arguments.get());

        ImGui.text("Working Directory");
        ImGui.setNextItemWidth(windowWidth);

        if (ImGui.inputText("##Working Directory", workingDirectory))
            config.setWorkingDirectory(Path.of(workingDirectory.get()));

        ImGui.text("Input Path");

        ImGui.setNextItemWidth(ImGui.getContentRegionAvailX());
        if (ImGui.inputText("##Input Path", inputPath))
            config.setInputPath(Path.of(inputPath.get()));

        ImGui.text("Output Path");

        ImGui.setNextItemWidth(ImGui.getContentRegionAvailX());
        if (ImGui.inputText("##Output Path", outputPath))
            config.setOutputPath(Path.of(outputPath.get()));

        ImGui.newLine();

        if (ImGui.collapsingHeader("Build Variables"))
            genBuildVariables();

        if (ImGui.collapsingHeader("Environment Variables"))
            genEnvVariables();
    }
    
    private final ImString newEnvVariableName = new ImString("", 64);

    private void genBuildVariables() {
        final var variables = config.getVariables();
        float windowWidth = ImGui.getContentRegionAvailX();

        ImGui.indent();

        ImGui.textWrapped("Create and use build variables for them to be use in your build command."
                + " You can use the variables in your build command by wrapping the variable name in ${}.");
        ImGui.newLine();

        if (buildVariablesInputs.length != variables.size())
            buildVariablesInputs = new ImString[variables.size()];

        int i = 0;
        final var buildVariableIterator = variables.entrySet().iterator();
        while (buildVariableIterator.hasNext()) {
            final var entry = buildVariableIterator.next();

            if (buildVariablesInputs[i] == null)
                buildVariablesInputs[i] = new ImString(entry.getValue(), 1024);
            buildVariablesInputs[i].set(entry.getValue());

            if (ImGui.inputText(entry.getKey() + "##BuildVar" + i, buildVariablesInputs[i]))
                variables.put(entry.getKey(), buildVariablesInputs[i].get());
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
        ImGui.beginDisabled(newBuildVariableName.isEmpty() || variables.containsKey(newBuildVariableName.get()));
        if (ImGui.button("+##BuildVar")) {
            variables.put(newBuildVariableName.get(), "");
            newBuildVariableName.set("");
            showBuildVariableWarning = false;
        }
        ImGui.endDisabled();
        if (showBuildVariableWarning) {
            if (newBuildVariableName.isEmpty())
                ImGui.textColored(0xFF8888FF, "Name cannot be empty");
            else if (variables.containsKey(newBuildVariableName.get()))
                ImGui.textColored(0xFF8888FF, "Name already exists");
            else
                ImGui.newLine();
        } else
            ImGui.newLine();

        ImGui.unindent();
    }
    private boolean showEnvVariableWarning = false;

    private void genEnvVariables() {
        final var environment = config.getEnvironment();
        float windowWidth = ImGui.getContentRegionAvailX();

        ImGui.indent();

        ImGui.textWrapped("Create and use environment variables for your command's process."
                + " They will also apply the child processes of the command.");
        ImGui.newLine();

        if (envVariablesInputs.length != environment.size())
            envVariablesInputs = new ImString[environment.size()];

        int i = 0;
        for (final var entry : environment.entrySet()) {
            if (envVariablesInputs[i] == null)
                envVariablesInputs[i] = new ImString(entry.getValue(), 1024);

            if (ImGui.inputText(entry.getKey() + "##EnvVar" + i, envVariablesInputs[i]))
                environment.put(entry.getKey(), envVariablesInputs[i].get());
            ImGui.sameLine();
            ImGui.setCursorPosX(windowWidth - 7);
            if (ImGui.button("x##EnvVar" + entry.getKey()))
                environment.remove(entry.getKey());

            i++;
        }

        ImGui.text("New Environment Variable");
        if (ImGui.inputText("Name##EnvVar", newEnvVariableName))
            showEnvVariableWarning = true;
        ImGui.sameLine();
        ImGui.setCursorPosX(windowWidth - 7);
        ImGui.beginDisabled(newEnvVariableName.isEmpty() || environment.containsKey(newEnvVariableName.get()));
        if (ImGui.button("+##EnvVar")) {
            environment.put(newEnvVariableName.get(), "");
            newEnvVariableName.set("");
            showEnvVariableWarning = false;
        }
        ImGui.endDisabled();
        if (showEnvVariableWarning) {
            if (newEnvVariableName.isEmpty())
                ImGui.textColored(0xFF8888FF, "Name cannot be empty");
            else if (environment.containsKey(newEnvVariableName.get()))
                ImGui.textColored(0xFF8888FF, "Name already exists");
            else
                ImGui.newLine();
        } else
            ImGui.newLine();

        ImGui.unindent();
    }


}
