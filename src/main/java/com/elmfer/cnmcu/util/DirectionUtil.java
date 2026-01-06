package com.elmfer.cnmcu.util;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;

public final class DirectionUtil {
    private DirectionUtil() {}

    public static Direction rotate(@NotNull Direction origin, @NotNull Direction subject) {
        return switch (origin) {
            case NORTH -> subject;
            case EAST -> subject.getClockWise();
            case SOUTH -> subject.getOpposite();
            case WEST -> subject.getCounterClockWise();
            default -> throw new UnsupportedOperationException();
        };
    }

    public static Direction rotateInverse(@NotNull Direction origin, @NotNull Direction subject) {
        return switch (origin) {
            case NORTH -> subject;
            case EAST -> subject.getCounterClockWise();
            case SOUTH -> subject.getOpposite();
            case WEST -> subject.getClockWise();
            default -> throw new UnsupportedOperationException();
        };
    }
}
