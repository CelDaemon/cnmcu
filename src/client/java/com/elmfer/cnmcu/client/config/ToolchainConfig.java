package com.elmfer.cnmcu.client.config;

import com.elmfer.cnmcu.client.toolchain.Toolchain;
import com.elmfer.cnmcu.cpp.NativesLoader;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ToolchainConfig {

    private final Map<String, String> variables = new HashMap<>();
    private final Map<String, String> environment = new HashMap<>();
    private Path executable;
    private String arguments;
    private Path workingDirectory;
    private Path inputPath;
    private Path outputPath;

    public ToolchainConfig() {
        reset();
    }

    public Path getExecutable() {
        return executable;
    }

    public void setExecutable(Path executable) {
        this.executable = executable;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    public Path getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public Path getInputPath() {
        return inputPath;
    }

    public void setInputPath(Path inputPath) {
        this.inputPath = inputPath;
    }

    public Path getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(Path outputPath) {
        this.outputPath = outputPath;
    }

    public Map<String, String> getVariables() {
        return variables;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void reset() {
        executable = NativesLoader.getExecutablePath("vasm6502_oldstyle");

        arguments = "-Fbin -dotdir ${input} -o ${output}";

        workingDirectory = Toolchain.TOOLCHAIN_PATH;

        inputPath = Path.of("temp/program.s");
        outputPath = Path.of("temp/output.bin");

        variables.clear();

        environment.clear();
    }

    public void load(ToolchainConfigRecord record) {
        executable = record.executable();
        arguments = record.arguments();
        workingDirectory = record.workingDirectory();
        variables.clear();
        variables.putAll(record.variables());
        environment.clear();
        environment.putAll(record.environment());
    }

    public ToolchainConfigRecord getRecord() {
        return new ToolchainConfigRecord(
                executable,
                arguments,
                workingDirectory,
                variables,
                environment
        );
    }
}
