package com.elmfer.cnmcu.cpp;

import java.util.OptionalLong;

public abstract class NativeObject {

    private long nativePtr;
    protected NativeObject(long nativePtr) {
        this.nativePtr = nativePtr;
    }

    protected OptionalLong getNativePtr() {
        return nativePtr == 0 ? OptionalLong.empty() : OptionalLong.of(nativePtr);
    }
    protected void clear() {
        nativePtr = 0;
    }
    public boolean isNotNull() {
        return nativePtr != 0;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(getNativePtr().orElse(0));
    }
}
