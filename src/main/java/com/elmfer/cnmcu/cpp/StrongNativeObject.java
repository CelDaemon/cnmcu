package com.elmfer.cnmcu.cpp;

/**
 * This class is used to store the native pointer of a native object.
 * It is used to track the native object's life cycle. Java code is
 * responsible for the life cycle of the native object.
 */
public abstract class StrongNativeObject extends NativeObject {

    protected StrongNativeObject(long nativePtr) {
        super(nativePtr);
    }

    public void deleteNativeObject() {
        if(!isNotNull())
            return;

        deleteNative();

        clear();
    }

    protected abstract void deleteNative();
}
