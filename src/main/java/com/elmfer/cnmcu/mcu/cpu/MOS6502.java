package com.elmfer.cnmcu.mcu.cpu;

import com.elmfer.cnmcu.cpp.WeakNativeObject;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Reference to a MOS 6502 CPU object
 * it is a weak reference, so it will be invalidated if the native
 * object is deleted.
 */
public class MOS6502 extends WeakNativeObject {

    /**
     * Called in the mod's native code, do not call directly.
     */
    protected MOS6502(long ptr) {
        super(ptr);
    }
    
    public void NMI() {
        NMI(getNativePtr().orElseThrow());
    }
    
    public void IRQ() {
        IRQ(getNativePtr().orElseThrow());
    }
    
    public void Reset() {
        Reset(getNativePtr().orElseThrow());
    }
    
    public int getPC() {
        return GetPC(getNativePtr().orElseThrow());
    }
    
    public int getS() {
        return GetS(getNativePtr().orElseThrow());
    }
    
    public int getP() {
        return GetP(getNativePtr().orElseThrow());
    }
    
    public int getA() {
        return GetA(getNativePtr().orElseThrow());
    }
    
    public int getX() {
        return GetX(getNativePtr().orElseThrow());
    }
    
    public int getY() {
        return GetY(getNativePtr().orElseThrow());
    }
    
    public State getState() {
        assert isNotNull();

        return new State(
                getPC(),
                getS(),
                getP(),
                getA(),
                getX(),
                getY()
        );
    }
    
    public void setState(State state) {
        int[] stateArray = new int[] { state.pc, state.s, state.p, state.a,
                state.x, state.y };

        SetState(getNativePtr().orElseThrow(), stateArray);
    }

    public record State(
            int pc,
            int s,
            int p,
            int a,
            int x,
            int y
    ) {
        public static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("pc").forGetter(State::pc),
                Codec.INT.fieldOf("s").forGetter(State::s),
                Codec.INT.fieldOf("p").forGetter(State::p),
                Codec.INT.fieldOf("a").forGetter(State::a),
                Codec.INT.fieldOf("x").forGetter(State::x),
                Codec.INT.fieldOf("y").forGetter(State::y)
        ).apply(instance, State::new));
    }
    
    // @formatter:off
    
    /*JNI
        #include "mos6502.hpp"
    */
    
    private static native void NMI(long ptr); /*
        mos6502* cpu = reinterpret_cast<mos6502*>(ptr);
        cpu->NMI();
    */
    
    private static native void IRQ(long ptr);/*
        mos6502* cpu = reinterpret_cast<mos6502*>(ptr);
        cpu->IRQ();
    */
    
    private static native void Reset(long ptr); /*
        mos6502* cpu = reinterpret_cast<mos6502*>(ptr);
        cpu->Reset();
    */ 
    
    private static native int GetPC(long ptr); /*
        mos6502* cpu = reinterpret_cast<mos6502*>(ptr);
        return static_cast<jint>(cpu->GetPC());
    */
    
    private static native int GetS(long ptr); /*
        mos6502* cpu = reinterpret_cast<mos6502*>(ptr);
        return static_cast<jint>(cpu->GetS());
    */
    
    private static native int GetP(long ptr); /*
        mos6502* cpu = reinterpret_cast<mos6502*>(ptr);
        return static_cast<jint>(cpu->GetP());
    */
    
    private static native int GetA(long ptr);/*
        mos6502* cpu = reinterpret_cast<mos6502*>(ptr);
        return static_cast<jint>(cpu->GetA());
    */

    private static native int GetX(long ptr); /*
        mos6502* cpu = reinterpret_cast<mos6502*>(ptr);
        return static_cast<jint>(cpu->GetX());
    */
    
    private static native int GetY(long ptr); /*
        mos6502* cpu = reinterpret_cast<mos6502*>(ptr);
        return static_cast<jint>(cpu->GetY());
    */
    
    private static native void SetState(long ptr, int[] state); /*
        mos6502* cpu = reinterpret_cast<mos6502*>(ptr);
        cpu->SetState(state);
    */
}
