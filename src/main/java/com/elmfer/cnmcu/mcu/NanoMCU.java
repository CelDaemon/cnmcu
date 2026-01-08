package com.elmfer.cnmcu.mcu;

import com.elmfer.cnmcu.cpp.StrongNativeObject;
import com.elmfer.cnmcu.mcu.cpu.MOS6502;
import com.elmfer.cnmcu.mcu.modules.CNEL;
import com.elmfer.cnmcu.mcu.modules.CNEL.EventType;
import com.elmfer.cnmcu.mcu.modules.CNGPIO;
import com.elmfer.cnmcu.mcu.modules.CNRAM;
import com.elmfer.cnmcu.mcu.modules.CNROM;
import com.elmfer.cnmcu.mcu.modules.CNUART;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;

import java.nio.ByteBuffer;

public class NanoMCU extends StrongNativeObject {
    public int frontInput, rightInput, backInput, leftInput;
    public int frontOutput, rightOutput, backOutput, leftOutput;
    private boolean frontOutputChanged, rightOutputChanged, backOutputChanged, leftOutputChanged;

    private final MOS6502 cpu;
    private final CNGPIO gpio;
    private final CNRAM ram;
    private final CNROM rom;
    private final CNEL el;
    private final CNUART uart;

    public NanoMCU() {
        super(createMCU());

        var ptr = getNativePtr().orElseThrow();
        cpu = new MOS6502(CPU(ptr));
        gpio = new CNGPIO(GPIO(ptr));
        ram = new CNRAM(RAM(ptr));
        rom = new CNROM(ROM(ptr));
        el = new CNEL(EL(ptr));
        uart = new CNUART(UART(ptr));
    }

    public void tick() {

        updateInputs();
        el.triggerEvent(EventType.GAME_TICK);
        tick(getNativePtr().orElseThrow());
        updateOutputs();
    }

    public boolean outputHasChanged(Direction direction) {
        return switch (direction) {
            case NORTH -> frontOutputChanged;
            case EAST -> rightOutputChanged;
            case SOUTH -> backOutputChanged;
            case WEST -> leftOutputChanged;
            default -> false;
        };
    }

    private void updateInputs() {
        var inputs = new int[] {
                frontInput,
                rightInput,
                backInput,
                leftInput
        };

        setInputs(inputs);
    }

    private void updateOutputs() {
        var outputs = getOutputs();

        frontOutputChanged = frontOutput != outputs[0];
        rightOutputChanged = rightOutput != outputs[1];
        backOutputChanged = backOutput != outputs[2];
        leftOutputChanged = leftOutput != outputs[3];

        frontOutput = outputs[0];
        rightOutput = outputs[1];
        backOutput = outputs[2];
        leftOutput = outputs[3];
    }

    public void cycle() {
        updateInputs();
        cycle(getNativePtr().orElseThrow());
        updateOutputs();
    }

    private void resetIO() {
        frontInput = rightInput = backInput = leftInput = 0;

        frontOutputChanged = frontOutput != 0;
        rightOutputChanged = rightOutput != 0;
        backOutputChanged = backOutput != 0;
        leftOutputChanged = leftOutput != 0;

        frontOutput = rightOutput = backOutput = leftOutput = 0;
    }

    public void reset() {
        resetIO();
        reset(getNativePtr().orElseThrow());
    }

    public void setPowered(boolean powered) {
        resetIO();
        setPowered(getNativePtr().orElseThrow(), powered);
    }

    public boolean isPowered() {
        return isPowered(getNativePtr().orElseThrow());
    }

    public void setClockPause(boolean paused) {
        setClockPause(getNativePtr().orElseThrow(), paused);
    }

    public boolean isClockPaused() {
        return isClockPaused(getNativePtr().orElseThrow());
    }

    public long numCycles() {
        return numCycles(getNativePtr().orElseThrow());
    }

    public int busAddress() {
        return busAddress(getNativePtr().orElseThrow());
    }

    public int busData() {
        return busData(getNativePtr().orElseThrow());
    }

    public boolean busRW() {
        return busRW(getNativePtr().orElseThrow());
    }

    protected void deleteNative() {
        cpu.invalidateNativeObject();
        gpio.invalidateNativeObject();
        ram.invalidateNativeObject();
        rom.invalidateNativeObject();
        el.invalidateNativeObject();
        uart.invalidateNativeObject();

        deleteMCU(getNativePtr().orElseThrow());
    }

    /*
     * Get the CPU of the MCU which is a MOS6502
     */
    public MOS6502 getCPU() {
        return cpu;
    }

    /*
     * Get the GPIO of the MCU.
     * 
     * General Purpose Input/Output
     */
    public CNGPIO getGPIO() {
        return gpio;
    }

    /*
     * Get the RAM of the MCU.
     * 
     * Random Access Memory
     */
    public CNRAM getRAM() {
        return ram;
    }

    /*
     * Get the ROM of the MCU.
     * 
     * Read Only Memory
     */
    public CNROM getROM() {
        return rom;
    }

    /*
     * Get the EL of the MCU.
     * 
     * Event Listener
     */
    public CNEL getEL() {
        return el;
    }

    /*
     * Get the UART of the MCU.
     * 
     * Universal Asynchronous Receiver-Transmitter
     */
    public CNUART getUART() {
        return uart;
    }

    public State getState() {
        var romState = rom.getState();
        var ramState = ram.getState();
        var gpioState =  gpio.getState();
        var cpuState = cpu.getState();
        var elState = el.getState();
        var uartState = uart.getState();

        return new State(
                frontOutput,
                rightOutput,
                backOutput,
                leftOutput,
                isPowered(),
                isClockPaused(),
                numCycles(),
                getPinOutputDrivers(),
                romState,
                ramState,
                gpioState,
                cpuState,
                elState,
                uartState
        );
    }

    public void setState(State state) {
        setPowered(state.powered);
        setClockPause(state.clockPaused);
        setNumCycles(getNativePtr().orElseThrow(), state.numCycles);

        frontOutput = state.frontOutput;
        rightOutput = state.rightOutput;
        backOutput = state.backOutput;
        leftOutput = state.leftOutput;

        var pinOutputDrivers = getPinOutputDrivers();
        pinOutputDrivers.put(state.pinOutputDrivers);

        rom.setState(state.rom);
        ram.setState(state.ram);
        gpio.setState(state.gpio);
        cpu.setState(state.cpu);
        el.setState(state.el);
        uart.setState(state.uart);
    }

    public record State(
            int frontOutput,
            int rightOutput,
            int backOutput,
            int leftOutput,
            boolean powered,
            boolean clockPaused,
            long numCycles,
            ByteBuffer pinOutputDrivers,
            CNROM.State rom,
            CNRAM.State ram,
            CNGPIO.State gpio,
            MOS6502.State cpu,
            CNEL.State el,
            CNUART.State uart
    ) {
        public static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("frontOutput").forGetter(State::frontOutput),
                Codec.INT.fieldOf("rightOutput").forGetter(State::rightOutput),
                Codec.INT.fieldOf("backOutput").forGetter(State::backOutput),
                Codec.INT.fieldOf("leftOutput").forGetter(State::leftOutput),
                Codec.BOOL.fieldOf("powered").forGetter(State::powered),
                Codec.BOOL.fieldOf("clockPaused").forGetter(State::clockPaused),
                Codec.LONG.fieldOf("numCycles").forGetter(State::numCycles),
                Codec.BYTE_BUFFER.fieldOf("pinOutputDrivers").forGetter(State::pinOutputDrivers),
                CNROM.State.CODEC.fieldOf("rom").forGetter(State::rom),
                CNRAM.State.CODEC.fieldOf("ram").forGetter(State::ram),
                CNGPIO.State.CODEC.fieldOf("gpio").forGetter(State::gpio),
                MOS6502.State.CODEC.fieldOf("cpu").forGetter(State::cpu),
                CNEL.State.CODEC.fieldOf("el").forGetter(State::el),
                CNUART.State.CODEC.fieldOf("uart").forGetter(State::uart)
        ).apply(instance, State::new));
    }

    public ByteBuffer getPinOutputDrivers() {
        return pinOutputDrivers(getNativePtr().orElseThrow());
    }

    public void setInputs(int[] inputs) {
        if(inputs.length != 4)
            throw new IllegalArgumentException();
        setInputs(getNativePtr().orElseThrow(), inputs);
    }

    public int[] getOutputs() {
        var outputs = new int[4];

        getOutputs(getNativePtr().orElseThrow(), outputs);

        return outputs;
    }

    // @formatter:off
    
    /*JNI
        #include <functional>
        #include <array>

        #include "Nano.hpp"
     */ 
    
    private static native long createMCU(); /*
        CodeNodeNano* nano = new CodeNodeNano();
        return reinterpret_cast<jlong>(nano);
    */
    
    private static native void deleteMCU(long ptr); /*
        CodeNodeNano* nano = reinterpret_cast<CodeNodeNano*>(ptr);
        delete nano;
    */

    private static native void setInputs(long ptr, int[] inputs); /*
        CodeNodeNano* nano = reinterpret_cast<CodeNodeNano*>(ptr);

        CNGPIO<CodeNodeNano::GPIO_NUM_PINS>& gpio = nano->GPIO();
        uint8_t* pvFront = gpio.pvFrontData();

        for(int i = 0; i < 4; i++) {
            if(!gpio.isInput(i))
                continue;
            pvFront[i] = static_cast<uint8_t>(inputs[i]);
        }
    */

    private static native void getOutputs(long ptr, int[] outputs); /*
        CodeNodeNano* nano = reinterpret_cast<CodeNodeNano*>(ptr);

        CNGPIO<CodeNodeNano::GPIO_NUM_PINS>& gpio = nano->GPIO();

        uint8_t* outputPinDrivers = nano->pinOutputDrivers();

        for(int i = 0; i < 4; i++) {
            if(gpio.isInput(i)) {
                outputs[i] = 0;
                continue;
            }
            outputs[i] = static_cast<jint>(outputPinDrivers[i]);
        }
    */
    
    private static native void tick(long ptr); /*
        CodeNodeNano* nano = reinterpret_cast<CodeNodeNano*>(ptr);
        
        nano->tick();
    */
    
    private static native void cycle(long ptr); /*
        CodeNodeNano* nano = reinterpret_cast<CodeNodeNano*>(ptr);
        nano->cycle();
    */
    
    private static native void reset(long ptr); /*
        CodeNodeNano* nano = reinterpret_cast<CodeNodeNano*>(ptr);
        nano->reset();
    */
    
    private static native void setPowered(long ptr, boolean powered); /*
        CodeNodeNano* nano = reinterpret_cast<CodeNodeNano*>(ptr);
        if(powered)
            nano->powerOn();
        else
            nano->powerOff();
    */
    
    private static native boolean isPowered(long ptr); /*
        CodeNodeNano* nano = reinterpret_cast<CodeNodeNano*>(ptr);
        return static_cast<jboolean>(nano->isPoweredOn());
    */
    private static native void setClockPause(long ptr, boolean paused); /*
        CodeNodeNano* nano = reinterpret_cast<CodeNodeNano*>(ptr);
        if(paused)
            nano->pauseClock();
        else
            nano->resumeClock();
    */
    
    private static native boolean isClockPaused(long ptr); /*
        CodeNodeNano* nano = reinterpret_cast<CodeNodeNano*>(ptr);
        return static_cast<jboolean>(nano->isClockPaused());
    */
    
    private static native long numCycles(long ptr); /*
        CodeNodeNano* nano = reinterpret_cast<CodeNodeNano*>(ptr);
        return static_cast<jlong>(nano->numCycles());
    */
    
    private static native void setNumCycles(long ptr, long cycles); /*
        CodeNodeNano* nano = reinterpret_cast<CodeNodeNano*>(ptr);
        uint64_t numCycles = static_cast<uint64_t>(cycles);
        nano->setNumCycles(numCycles);
    */
    
    private static native int busAddress(long ptr); /*
        CodeNodeNano* nano = reinterpret_cast<CodeNodeNano*>(ptr);
        return static_cast<jint>(nano->busAddress());
    */
    
    private static native int busData(long ptr); /*
        CodeNodeNano* nano = reinterpret_cast<CodeNodeNano*>(ptr);
        return static_cast<jint>(nano->busData());
    */
    
    private static native boolean busRW(long ptr); /*
        CodeNodeNano* nano = reinterpret_cast<CodeNodeNano*>(ptr);
        return static_cast<jboolean>(nano->busRw());
    */
    private static native ByteBuffer pinOutputDrivers(long ptr); /*
        CodeNodeNano* nano = reinterpret_cast<CodeNodeNano*>(ptr);
        return env->NewDirectByteBuffer(nano->pinOutputDrivers(), CodeNodeNano::GPIO_NUM_PINS);
    */
    
    private static native long CPU(long ptr); /*
        CodeNodeNano* nano = reinterpret_cast<CodeNodeNano*>(ptr);
        mos6502* cpu = &nano->CPU();
        return reinterpret_cast<jlong>(cpu);
    */
    
    private static native long GPIO(long ptr); /*
        CodeNodeNano* nano = reinterpret_cast<CodeNodeNano*>(ptr);
        CNGPIO<CodeNodeNano::GPIO_NUM_PINS>* gpio = &nano->GPIO();
        return reinterpret_cast<jlong>(gpio);
    */
    
    private static native long RAM(long ptr); /*
        CodeNodeNano* nano = reinterpret_cast<CodeNodeNano*>(ptr);
        CNRAM<CodeNodeNano::RAM_SIZE>* ram = &nano->RAM();
        return reinterpret_cast<jlong>(ram);
    */
    private static native long ROM(long ptr); /*
        CodeNodeNano* nano = reinterpret_cast<CodeNodeNano*>(ptr);
        CNROM<CodeNodeNano::ROM_SIZE>* rom = &nano->ROM();
        return reinterpret_cast<jlong>(rom);
    */
    
    private static native long EL(long ptr); /*
        CodeNodeNano* nano = reinterpret_cast<CodeNodeNano*>(ptr);
        CNEL<CodeNodeNano::EL_SIZE>* el = &nano->EL();
        return reinterpret_cast<jlong>(el);
    */
    
    private static native long UART(long ptr); /*
        CodeNodeNano* nano = reinterpret_cast<CodeNodeNano*>(ptr);
        CNUART* uart = &nano->UART();
        return reinterpret_cast<jlong>(uart);
    */
}
