package com.elmfer.cnmcu.cpp;

/**
 * This class is used to store the native pointer of a native object.
 * It is used to track the native object's life cycle. Java code is
 * not responsible for the life cycle of the native object, therefore
 * it can only borrow the native object (if it is still valid).
 */
public abstract class WeakNativeObject extends NativeObject {

    protected WeakNativeObject(long nativePtr) {
        super(nativePtr);
    }

    public void invalidate() {
        clear();
    }
}
