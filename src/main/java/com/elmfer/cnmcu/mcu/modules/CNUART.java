package com.elmfer.cnmcu.mcu.modules;

import java.nio.ByteBuffer;

import com.elmfer.cnmcu.cpp.WeakNativeObject;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/*
 * Reference to a CNUART object
 * 
 * it is a weak reference, so it will be invalidated if the native
 * object is deleted.
 */
public class CNUART extends WeakNativeObject {

    private final long size;
    
    /**
     * Constructor
     * 
     * Called in the mod's native code, do not call directly.
     */
    protected CNUART(long ptr) {
        setNativePtr(ptr);
        
        size = size(getNativePtr());
    }
    
    public long getSize() {
        assert isNativeObjectValid();

        return size;
    }
    
    public void reset() {
        assert isNativeObjectValid();

        reset(getNativePtr());
    }
    
    public boolean shouldInterrupt() {
        assert isNativeObjectValid();

        return shouldInterrupt(getNativePtr());
    }
    
    public ByteBuffer getRegisterData() {
        assert isNativeObjectValid();

        return registerData(getNativePtr());
    }
    
    public void write(int address, int data) {
        assert isNativeObjectValid();

        write(getNativePtr(), address, data);
    }
    
    public int read(int address) {
        assert isNativeObjectValid();

        return read(getNativePtr(), address);
    }
    
    public State getState() {
        assert isNativeObjectValid();
        
        ByteBuffer registerData = getRegisterData();

        return new State(
            registerData
        );
    }
    
    public void setState(State data) {
        assert isNativeObjectValid();

        var registerData = getRegisterData();
        registerData.put(data.registerData);
    }

    public record State(
            ByteBuffer registerData
    ) {
        public static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BYTE_BUFFER.fieldOf("registerData").forGetter(State::registerData)
        ).apply(instance, State::new));
    }
    
    // @formatter:off
    
    /*JNI
         #include "CNUART.hpp"
     */
    
    private static native long size(long ptr); /*
        CNUART* uart = reinterpret_cast<CNUART*>(ptr);
        return uart->size();
    */
    
    private static native void reset(long ptr); /*
        CNUART* uart = reinterpret_cast<CNUART*>(ptr);
        uart->reset();
    */
    
    private static native boolean shouldInterrupt(long ptr); /*
        CNUART* uart = reinterpret_cast<CNUART*>(ptr);
        return uart->shouldInterrupt();
    */
    
    private static native ByteBuffer registerData(long ptr); /*
        CNUART* uart = reinterpret_cast<CNUART*>(ptr);
        return env->NewDirectByteBuffer(uart->registerData(), uart->size());
    */
    
    private static native void write(long ptr, int address, int data); /*
        CNUART* uart = reinterpret_cast<CNUART*>(ptr);
        uint16_t addr = address;
        uint16_t dat = data;
        uart->write(addr, dat);
    */
    
    private static native int read(long ptr, int address); /*
        CNUART* uart = reinterpret_cast<CNUART*>(ptr);
        uint16_t addr = address;
        return static_cast<jint>(uart->read(addr));
    */
}
