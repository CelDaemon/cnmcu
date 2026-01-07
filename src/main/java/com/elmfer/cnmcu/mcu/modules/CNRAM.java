package com.elmfer.cnmcu.mcu.modules;

import java.nio.ByteBuffer;

import com.elmfer.cnmcu.cpp.WeakNativeObject;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Reference to a CNRAM object.
 * It is a weak reference, so it will be invalidated if the native
 * object is deleted.
 */
public class CNRAM extends WeakNativeObject {
    
    private final long size;
    
    /**
     * Called in the mod's native code, do not call directly.
     */
    protected CNRAM(long ptr) {
        super(ptr);
        
        size = size(ptr);
    }
    
    public long getSize() {
        assert isNotNull();

        return size;
    }
    
    public ByteBuffer getData() {
        return data(getNativePtr().orElseThrow());
    }
    
    public byte read(int address) {
        return read(getNativePtr().orElseThrow(), address);
    }
    
    public void write(int address, byte value) {
        write(getNativePtr().orElseThrow(), address, value);
    }
    
    public State getState() {
        ByteBuffer buffer = getData();
        return new State(buffer);
    }
    
    public void setState(State state) {
        ByteBuffer buffer = getData();
        buffer.put(state.data);
    }

    public record State(
            ByteBuffer data
    ) {
        public static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BYTE_BUFFER.fieldOf("data").forGetter(State::data)
        ).apply(instance, State::new));
    }
    
    // @formatter:off
    
    /*JNI
         #include "Nano.hpp"
     */
    
    private static native long size(long ptr); /*
        return static_cast<jlong>(CodeNodeNano::RAM_SIZE);
    */
    
    private static native ByteBuffer data(long ptr); /*
        CNRAM<CodeNodeNano::RAM_SIZE>* ram = reinterpret_cast<CNRAM<CodeNodeNano::RAM_SIZE>*>(ptr);
        return env->NewDirectByteBuffer(ram->data(), CodeNodeNano::RAM_SIZE);
    */
    
    private static native byte read(long ptr, int address); /*
        CNRAM<CodeNodeNano::RAM_SIZE>* ram = reinterpret_cast<CNRAM<CodeNodeNano::RAM_SIZE>*>(ptr);
        uint16_t addr = static_cast<uint16_t>(address);
        return static_cast<jbyte>(ram->read(addr));
    */
    
    private static native void write(long ptr, int address, byte value); /*
        CNRAM<CodeNodeNano::RAM_SIZE>* ram = reinterpret_cast<CNRAM<CodeNodeNano::RAM_SIZE>*>(ptr);
        uint16_t addr = static_cast<uint16_t>(address);
        uint8_t val = static_cast<uint8_t>(value);
        ram->write(addr, val);
    */
}
