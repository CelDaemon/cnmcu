#pragma once

#include "mos6502.hpp"
#include "CNGPIO.hpp"
#include "CNRAM.hpp"
#include "CNROM.hpp"
#include "CNEL.hpp"
#include "CNUART.hpp"

#define GAME_TICK_RATE 20 // 20 Hz

class CodeNodeNano
{
public:
    constexpr static size_t GPIO_NUM_PINS = 64; // Supports 64 pins, only 4 of which are used
    constexpr static size_t RAM_SIZE = 512; // 512 bytes
    constexpr static size_t ROM_SIZE = 8192; // 8 KB
    constexpr static size_t EL_SIZE = 8; // 8 bytes
    constexpr static size_t CLOCK_FREQUENCY = GAME_TICK_RATE * 40; // 800 Hz

    CodeNodeNano();

    void tick();
    void cycle();
    void reset();

    void powerOn();
    void powerOff();
    bool isPoweredOn() const;
    void pauseClock() { clockPaused = true; }
    void resumeClock() { clockPaused = false; }
    bool isClockPaused() const { return clockPaused; }
    uint64_t numCycles() const { return cyclesCounter; }
    void setNumCycles(uint64_t cycles) { cyclesCounter = cycles; cyclesTarget = cycles;}

    uint16_t busAddress() const { return m_busAddress; }
    uint8_t busData() const { return m_busData; }
    bool busRw() const { return m_busRw; }

    mos6502& CPU();
    CNGPIO<GPIO_NUM_PINS>& GPIO();
    CNRAM<RAM_SIZE>& RAM();
    CNROM<ROM_SIZE>& ROM();
    CNEL<EL_SIZE>& EL();
    CNUART& UART();
    std::array<uint8_t, GPIO_NUM_PINS>& pinOutputDrivers() { return pinOutputs; }
private:
    mos6502 cpu;
    CNGPIO<GPIO_NUM_PINS> gpio;
    CNRAM<RAM_SIZE> ram;
    CNROM<ROM_SIZE> rom;
    CNEL<EL_SIZE> el;
    CNUART uart;
    uint64_t cyclesCounter = 0;
    uint64_t cyclesTarget = 0;
    std::array<uint8_t, GPIO_NUM_PINS> pinOutputs; // "Pin output drivers"

    uint16_t m_busAddress = 0;
    uint8_t m_busData = 0;
    bool m_busRw = false;

    bool poweredOn = false;
    bool clockPaused = false;

    uint8_t read(uint16_t address);
    void write(uint16_t address, uint8_t value);
    void cycle(mos6502* cpu);
};