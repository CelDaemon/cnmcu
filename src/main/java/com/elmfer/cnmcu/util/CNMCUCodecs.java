package com.elmfer.cnmcu.util;

import com.mojang.serialization.Codec;

import java.nio.file.Path;

public class CNMCUCodecs {
    public static final Codec<Path> PATH = Codec.STRING.xmap(Path::of, Path::toString);
}
