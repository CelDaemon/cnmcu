package com.elmfer.cnmcu.cpp;

public abstract class NativeObject {

    private long nativePtr;
    protected NativeObject(long nativePtr) {
        this.nativePtr = nativePtr;
    }

    protected long getNativePtr() {
        return nativePtr;
    }
    protected void clear() {
        nativePtr = 0;
    }
    public boolean isNotNull() {
        return nativePtr != 0;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(getNativePtr());
    }
}
