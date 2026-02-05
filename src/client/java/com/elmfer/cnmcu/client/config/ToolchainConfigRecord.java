package com.elmfer.cnmcu.client.config;

import com.elmfer.cnmcu.util.CNMCUCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.nio.file.Path;
import java.util.Map;

public record ToolchainConfigRecord(
        Path executable,
        String arguments,
        Path workingDirectory,
        Map<String, String> variables,
        Map<String, String> environment
) {
    public static final Codec<ToolchainConfigRecord> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    CNMCUCodecs.PATH.fieldOf("executable").forGetter(ToolchainConfigRecord::executable),
                    Codec.STRING.fieldOf("arguments").forGetter(ToolchainConfigRecord::arguments),
                    CNMCUCodecs.PATH.fieldOf("working_directory").forGetter(ToolchainConfigRecord::workingDirectory),
                    Codec.unboundedMap(Codec.STRING, Codec.STRING).validate(ToolchainConfigRecord::validateVariables)
                            .fieldOf("variables").forGetter(ToolchainConfigRecord::variables),
                    Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("environment").forGetter(ToolchainConfigRecord::environment)
            ).apply(instance, ToolchainConfigRecord::new)
    );

    private static DataResult<Map<String, String>> validateVariables(Map<String, String> input) {
        for (final var key : new String[]{"input", "output"}) {
            if (!input.containsKey(key))
                continue;
            return DataResult.error(() -> "Reserved variable present: " + key, input);
        }
        return DataResult.success(input);
    }
}
