package com.elmfer.cnmcu.client.config;

import com.elmfer.cnmcu.util.CNMCUCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

import java.nio.file.Path;

public record ConfigRecord(
        boolean hexRegisters,
        boolean showDocs,
        int maxBackups,
        Path lastSaved,
        ToolchainConfigRecord toolchain
) {
    public static final Codec<ConfigRecord> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.fieldOf("hex_registers").forGetter(ConfigRecord::hexRegisters),
                    Codec.BOOL.fieldOf("show_docs").forGetter(ConfigRecord::showDocs),
                    ExtraCodecs.NON_NEGATIVE_INT.fieldOf("max_backups").forGetter(ConfigRecord::maxBackups),
                    CNMCUCodecs.PATH.fieldOf("last_saved").forGetter(ConfigRecord::lastSaved),
                    ToolchainConfigRecord.CODEC.fieldOf("toolchain").forGetter(ConfigRecord::toolchain)
            ).apply(instance, ConfigRecord::new)
    );
}
