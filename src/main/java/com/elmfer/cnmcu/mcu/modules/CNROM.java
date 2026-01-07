package com.elmfer.cnmcu.mcu.modules;

import java.nio.ByteBuffer;

import com.elmfer.cnmcu.cpp.WeakNativeObject;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Reference to a CNROM object. It is a weak reference, so it will be invalidated
 * if the native object is deleted.
 */
public class CNROM extends WeakNativeObject {  
    private final long size;
    
    private boolean writeProtected;
    
    /**
     * Called in the mod's native code, do not call directly.
     */
    public CNROM(long ptr) {
        super(ptr);
        
        size = size(getNativePtr().orElseThrow());
        writeProtected = isWriteProtected(getNativePtr().orElseThrow());
    }
    
    public long getSize() {
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
    
    public void setWriteProtected(boolean writeProtected) {
        setWriteProtected(getNativePtr().orElseThrow(), writeProtected);
        this.writeProtected = writeProtected;
    }
    
    public boolean isWriteProtected() {
        return writeProtected;
    }
    
    public State getState() {
        ByteBuffer data = getData();
        return new State(
                data,
                isWriteProtected()
        );
    }
    
    public void setState(State state) {
        ByteBuffer data = getData();
        data.put(state.data());
        setWriteProtected(state.writeProtected());
    }

    public record State(
            ByteBuffer data,
            boolean writeProtected
    ) {
        public static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BYTE_BUFFER.fieldOf("data").forGetter(State::data),
                Codec.BOOL.fieldOf("writeProtected").forGetter(State::writeProtected)
        ).apply(instance, State::new));
    }
    // @formatter:off
    
    /*JNI
        #include "Nano.hpp"
     */
    
    private static native long size(long ptr); /*
        return static_cast<jlong>(CodeNodeNano::ROM_SIZE);
    */
    
    private static native ByteBuffer data(long ptr); /*
        CNROM<CodeNodeNano::ROM_SIZE>* rom = reinterpret_cast<CNROM<CodeNodeNano::ROM_SIZE>*>(ptr);
        return env->NewDirectByteBuffer(rom->data(), CodeNodeNano::ROM_SIZE);
    */
    
    private static native byte read(long ptr, int address); /*
        CNROM<CodeNodeNano::ROM_SIZE>* rom = reinterpret_cast<CNROM<CodeNodeNano::ROM_SIZE>*>(ptr);
        uint16_t addr = static_cast<uint16_t>(address);
        return static_cast<jbyte>(rom->read(addr));
    */
    
    private static native void write(long ptr, int address, byte value); /*
        CNROM<CodeNodeNano::ROM_SIZE>* rom = reinterpret_cast<CNROM<CodeNodeNano::ROM_SIZE>*>(ptr);
        uint16_t addr = static_cast<uint16_t>(address);
        uint8_t val = static_cast<uint8_t>(value);
        rom->write(addr, val);
    */
    
    private static native boolean isWriteProtected(long ptr); /*
        CNROM<CodeNodeNano::ROM_SIZE>* rom = reinterpret_cast<CNROM<CodeNodeNano::ROM_SIZE>*>(ptr);
        return static_cast<jboolean>(rom->isWriteProtected());
    */
    private static native void setWriteProtected(long ptr, boolean writeProtected); /*
        CNROM<CodeNodeNano::ROM_SIZE>* rom = reinterpret_cast<CNROM<CodeNodeNano::ROM_SIZE>*>(ptr);
        rom->setWriteProtect(static_cast<bool>(writeProtected));
    */
}
