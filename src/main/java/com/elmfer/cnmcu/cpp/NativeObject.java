package com.elmfer.cnmcu.cpp;

import java.util.OptionalLong;

public abstract class NativeObject {
    private long nativePtr;
    protected NativeObject(long nativePtr) {
        if(nativePtr == 0)
            throw new IllegalArgumentException("May not create a native object using a null pointer");
        this.nativePtr = nativePtr;
    }

    protected OptionalLong getNativePtr() {
        return nativePtr == 0 ? OptionalLong.empty() : OptionalLong.of(nativePtr);
    }
    protected void clear() {
        nativePtr = 0;
    }
    public boolean isValid() {
        return nativePtr != 0;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(nativePtr);
    }
}
