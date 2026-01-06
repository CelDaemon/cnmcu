package com.elmfer.cnmcu.mcu.modules;

import java.nio.ByteBuffer;

import com.elmfer.cnmcu.cpp.WeakNativeObject;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Reference to a CNEL object
 * 
 * it is a weak reference, so it will be invalidated if the native
 * object is deleted.
 */
public class CNEL extends WeakNativeObject {
    
    public enum EventType {
        GAME_TICK(0);

        private final int value;

        EventType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
    
    private final long size;
    
    /**
     * Constructor
     * 
     * Called in the mod's native code, do not call directly.
     */
    protected CNEL(long ptr) {
        super(ptr);
        
        size = size(getNativePtr());
    }
    
    public long getSize() {
        assert isNotNull();

        return size;
    }
    
    public ByteBuffer getICLRegistersData() {
        assert isNotNull();

        return iclRegistersData(getNativePtr());
    }
    
    public ByteBuffer getIFLRegistersData() {
        assert isNotNull();

        return iflRegistersData(getNativePtr());
    }
    
    public void triggerEvent(EventType event) {
        assert isNotNull();

        triggerEvent(getNativePtr(), event.value);
    }
    
    public boolean shouldInterrupt() {
        assert isNotNull();

        return shouldInterrupt(getNativePtr());
    }
    
    public int read(int address) {
        assert isNotNull();

        return read(address);
    }
    
    public void write(int address, int data) {
        assert isNotNull();

        write(address, data);
    }
    
    public State getState() {
        assert isNotNull();
        
        ByteBuffer iclRegistersData = getICLRegistersData();
        ByteBuffer iflRegistersData = getIFLRegistersData();

        return new State(
                iclRegistersData,
                iflRegistersData
        );
    }
    
    public void setState(State state) {
        assert isNotNull();
        
        var iclRegistersData = getICLRegistersData();
        var iflRegistersData = getIFLRegistersData();

        iclRegistersData.put(state.iclData);
        iflRegistersData.put(state.iflData);
    }

    public record State(
        ByteBuffer iclData,
        ByteBuffer iflData
    ) {
        public static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BYTE_BUFFER.fieldOf("iclData").forGetter(State::iclData),
                Codec.BYTE_BUFFER.fieldOf("iflData").forGetter(State::iflData)
        ).apply(instance, State::new));
    }
    
    // @formatter:off
    
    /*JNI
         #include "Nano.hpp"
     */
    
    private static native long size(long ptr); /*
        CNEL<CodeNodeNano::EL_SIZE>* cnel = reinterpret_cast<CNEL<CodeNodeNano::EL_SIZE>*>(ptr);
        return cnel->size();
    */
    
    private static native ByteBuffer iclRegistersData(long ptr); /*
        CNEL<CodeNodeNano::EL_SIZE>* cnel = reinterpret_cast<CNEL<CodeNodeNano::EL_SIZE>*>(ptr);
        return env->NewDirectByteBuffer(cnel->iclRegistersData(), CodeNodeNano::EL_SIZE);
    */
    
    private static native ByteBuffer iflRegistersData(long ptr); /*
        CNEL<CodeNodeNano::EL_SIZE>* cnel = reinterpret_cast<CNEL<CodeNodeNano::EL_SIZE>*>(ptr);
        return env->NewDirectByteBuffer(cnel->iflRegistersData(), CodeNodeNano::EL_SIZE);
    */
    
    private static native void triggerEvent(long ptr, int event); /*
        CNEL<CodeNodeNano::EL_SIZE>* cnel = reinterpret_cast<CNEL<CodeNodeNano::EL_SIZE>*>(ptr);
        cnel->triggerEvent(event);
    */
    
    private static native boolean shouldInterrupt(long ptr); /*
        CNEL<CodeNodeNano::EL_SIZE>* cnel = reinterpret_cast<CNEL<CodeNodeNano::EL_SIZE>*>(ptr);
        return cnel->shouldInterrupt();
    */
    
    private static native int read(long ptr, int address); /*
        CNEL<CodeNodeNano::EL_SIZE>* cnel = reinterpret_cast<CNEL<CodeNodeNano::EL_SIZE>*>(ptr);
        return static_cast<jint>(cnel->read(address));
    */
    
    private static native void write(long ptr, int address, int data); /*
        CNEL<CodeNodeNano::EL_SIZE>* cnel = reinterpret_cast<CNEL<CodeNodeNano::EL_SIZE>*>(ptr);
        uint16_t addr = static_cast<uint16_t>(address);
        uint8_t dat = static_cast<uint8_t>(data);
        cnel->write(addr, dat);
    */
}
