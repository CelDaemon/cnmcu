package com.elmfer.cnmcu.mcu.modules;

import java.nio.ByteBuffer;

import com.elmfer.cnmcu.cpp.WeakNativeObject;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Reference to a CNGPIO object.
 * it is a weak reference, so it will be invalidated if the native
 * object is deleted.
 */
public class CNGPIO extends WeakNativeObject {

    private final long size;
    
    /**
     * Called in the mod's native code, do not call directly.
     */
    public CNGPIO(long ptr) {
        super(ptr);
        
        size = size(ptr);
    }
    
    public long getSize() {
        return size;
    }
    
    public ByteBuffer getPVFrontData() {
        return pvFrontData(getNativePtr().orElseThrow());
    }
    
    public ByteBuffer getPVBackData() {
        return pvBackData(getNativePtr().orElseThrow());
    }
    
    public ByteBuffer getDirData() {
        return dirData(getNativePtr().orElseThrow());
    }
    
    public ByteBuffer getIntData() {
        return intData(getNativePtr().orElseThrow());
    }
    
    public ByteBuffer getIFLData() {
        return iflData(getNativePtr().orElseThrow());
    }
    
    public int read(int address) {
        return read(getNativePtr().orElseThrow(), address);
    }
    
    public void write(int address, int value) {
        write(getNativePtr().orElseThrow(), address, value);
    }
    
    public boolean shouldInterrupt() {
        return shouldInterrupt(getNativePtr().orElseThrow());
    }
    
    public State getState() {
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
        #include "Nano.hpp"
    */
    
    private static native long size(long ptr); /*
        CNGPIO<CodeNodeNano::GPIO_NUM_PINS>* gpio = reinterpret_cast<CNGPIO<CodeNodeNano::GPIO_NUM_PINS>*>(ptr);
        return static_cast<jlong>(gpio->size());
    */
    
    private static native ByteBuffer pvFrontData(long ptr); /*
        CNGPIO<CodeNodeNano::GPIO_NUM_PINS>* gpio = reinterpret_cast<CNGPIO<CodeNodeNano::GPIO_NUM_PINS>*>(ptr);

        std::array<uint8_t, CodeNodeNano::GPIO_NUM_PINS>& data = gpio->pvFrontData();
        return env->NewDirectByteBuffer(data.data(), data.size());
    */
    
    private static native ByteBuffer pvBackData(long ptr); /*
        CNGPIO<CodeNodeNano::GPIO_NUM_PINS>* gpio = reinterpret_cast<CNGPIO<CodeNodeNano::GPIO_NUM_PINS>*>(ptr);

        std::array<uint8_t, CodeNodeNano::GPIO_NUM_PINS>& data = gpio->pvBackData();
        return env->NewDirectByteBuffer(data.data(), data.size());
    */
    
    private static native ByteBuffer dirData(long ptr); /*
        CNGPIO<CodeNodeNano::GPIO_NUM_PINS>* gpio = reinterpret_cast<CNGPIO<CodeNodeNano::GPIO_NUM_PINS>*>(ptr);

        std::array<uint8_t, CodeNodeNano::GPIO_NUM_PINS / 8>& data = gpio->dirData();
        return env->NewDirectByteBuffer(data.data(), data.size());
    */
    
    private static native ByteBuffer intData(long ptr); /*
        CNGPIO<CodeNodeNano::GPIO_NUM_PINS>* gpio = reinterpret_cast<CNGPIO<CodeNodeNano::GPIO_NUM_PINS>*>(ptr);

        std::array<uint8_t, CodeNodeNano::GPIO_NUM_PINS / 2>& data = gpio->intData();
        return env->NewDirectByteBuffer(data.data(), data.size());
    */
    
    private static native ByteBuffer iflData(long ptr); /*
        CNGPIO<CodeNodeNano::GPIO_NUM_PINS>* gpio = reinterpret_cast<CNGPIO<CodeNodeNano::GPIO_NUM_PINS>*>(ptr);

        std::array<uint8_t, CodeNodeNano::GPIO_NUM_PINS / 8>& data = gpio->iflData();
        return env->NewDirectByteBuffer(data.data(), data.size());
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
