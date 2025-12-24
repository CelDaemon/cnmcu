package com.elmfer.cnmcu.mcu.modules;

import java.nio.ByteBuffer;

import com.elmfer.cnmcu.cpp.WeakNativeObject;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Reference to a CNGPIO object
 * 
 * it is a weak reference, so it will be invalidated if the native
 * object is deleted.
 */
public class CNGPIO extends WeakNativeObject {

    private final long size;
    
    /**
     * Constructor
     * 
     * Called in the mod's native code, do not call directly.
     */
    protected CNGPIO(long ptr) {
        setNativePtr(ptr);
        
        size = size(getNativePtr());
    }
    
    public long getSize() {
        assert isNativeObjectValid();

        return size;
    }
    
    public ByteBuffer getPVFrontData() {
        assert isNativeObjectValid();

        return pvFrontData(getNativePtr());
    }
    
    public ByteBuffer getPVBackData() {
        assert isNativeObjectValid();

        return pvBackData(getNativePtr());
    }
    
    public ByteBuffer getDirData() {
        assert isNativeObjectValid();

        return dirData(getNativePtr());
    }
    
    public ByteBuffer getIntData() {
        assert isNativeObjectValid();

        return intData(getNativePtr());
    }
    
    public ByteBuffer getIFLData() {
        assert isNativeObjectValid();

        return iflData(getNativePtr());
    }
    
    public int read(int address) {
        assert isNativeObjectValid();

        return read(getNativePtr(), address);
    }
    
    public void write(int address, int value) {
        assert isNativeObjectValid();

        write(getNativePtr(), address, value);
    }
    
    public boolean shouldInterrupt() {
        assert isNativeObjectValid();

        return shouldInterrupt(getNativePtr());
    }
    
    public State getState() {
        assert isNativeObjectValid();

        var pvFrontData = getPVFrontData();

        var pvBackData = getPVBackData();

        var dirData = getDirData();

        var intData = getIntData();

        var iflData = getIFLData();

        return new State(
                pvFrontData,
                pvBackData,
                dirData,
                intData,
                iflData
        );
    }
    
    public void setState(State state) {
        assert isNativeObjectValid();

        ByteBuffer buffer = getPVFrontData();
        buffer.put(state.pvFrontData);

        buffer = getPVBackData();
        buffer.put(state.pvBackData);

        buffer = getDirData();
        buffer.put(state.dirData);

        buffer = getIntData();
        buffer.put(state.intData);

        buffer = getIFLData();
        buffer.put(state.iflData);
    }

    public record State(
            ByteBuffer pvFrontData,
            ByteBuffer pvBackData,
            ByteBuffer dirData,
            ByteBuffer intData,
            ByteBuffer iflData
    ) {
        public static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BYTE_BUFFER.fieldOf("pvFrontData").forGetter(State::pvFrontData),
                Codec.BYTE_BUFFER.fieldOf("pvBackData").forGetter(State::pvBackData),
                Codec.BYTE_BUFFER.fieldOf("dirData").forGetter(State::dirData),
                Codec.BYTE_BUFFER.fieldOf("intData").forGetter(State::intData),
                Codec.BYTE_BUFFER.fieldOf("iflData").forGetter(State::iflData)
        ).apply(instance, State::new));
    }
    
    // @formatter:off
    
    /*JNI
        #include "cnmcuJava.h"
        #include "Nano.hpp"
    */
    
    private static native long size(long ptr); /*
        CNGPIO<CodeNodeNano::GPIO_NUM_PINS>* gpio = reinterpret_cast<CNGPIO<CodeNodeNano::GPIO_NUM_PINS>*>(ptr);
        return static_cast<jlong>(gpio->size());
    */
    
    private static native ByteBuffer pvFrontData(long ptr); /*
        CNGPIO<CodeNodeNano::GPIO_NUM_PINS>* gpio = reinterpret_cast<CNGPIO<CodeNodeNano::GPIO_NUM_PINS>*>(ptr);
        return env->NewDirectByteBuffer(gpio->pvFrontData(), CodeNodeNano::GPIO_NUM_PINS);
    */
    
    private static native ByteBuffer pvBackData(long ptr); /*
        CNGPIO<CodeNodeNano::GPIO_NUM_PINS>* gpio = reinterpret_cast<CNGPIO<CodeNodeNano::GPIO_NUM_PINS>*>(ptr);
        return env->NewDirectByteBuffer(gpio->pvBackData(), CodeNodeNano::GPIO_NUM_PINS);
    */
    
    private static native ByteBuffer dirData(long ptr); /*
        CNGPIO<CodeNodeNano::GPIO_NUM_PINS>* gpio = reinterpret_cast<CNGPIO<CodeNodeNano::GPIO_NUM_PINS>*>(ptr);
        return env->NewDirectByteBuffer(gpio->dirData(), CodeNodeNano::GPIO_NUM_PINS / 8);
    */
    
    private static native ByteBuffer intData(long ptr); /*
        CNGPIO<CodeNodeNano::GPIO_NUM_PINS>* gpio = reinterpret_cast<CNGPIO<CodeNodeNano::GPIO_NUM_PINS>*>(ptr);
        return env->NewDirectByteBuffer(gpio->intData(), CodeNodeNano::GPIO_NUM_PINS / 2);
    */
    
    private static native ByteBuffer iflData(long ptr); /*
        CNGPIO<CodeNodeNano::GPIO_NUM_PINS>* gpio = reinterpret_cast<CNGPIO<CodeNodeNano::GPIO_NUM_PINS>*>(ptr);
        return env->NewDirectByteBuffer(gpio->iflData(), CodeNodeNano::GPIO_NUM_PINS / 8);
    */
    
    private static native int read(long ptr, int address); /*
        CNGPIO<CodeNodeNano::GPIO_NUM_PINS>* gpio = reinterpret_cast<CNGPIO<CodeNodeNano::GPIO_NUM_PINS>*>(ptr);
        uint16_t addr = static_cast<uint16_t>(address);
        uint8_t val = gpio->read(address);    
        return static_cast<jint>(val);
    */
    
    private static native void write(long ptr, int address, int value); /*
        CNGPIO<CodeNodeNano::GPIO_NUM_PINS>* gpio = reinterpret_cast<CNGPIO<CodeNodeNano::GPIO_NUM_PINS>*>(ptr);
        uint16_t addr = static_cast<uint16_t>(address);
        uint8_t val = static_cast<uint8_t>(value);
        gpio->write(addr, val);
    */
    
    private static native boolean shouldInterrupt(long ptr); /*
        CNGPIO<CodeNodeNano::GPIO_NUM_PINS>* gpio = reinterpret_cast<CNGPIO<CodeNodeNano::GPIO_NUM_PINS>*>(ptr);
        return static_cast<jboolean>(gpio->shouldInterrupt());
    */
}
