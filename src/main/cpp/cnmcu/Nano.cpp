#include "Nano.hpp"

CodeNodeNano::CodeNodeNano()
    , cyclesCounter(0)
    , cyclesTarget(0)
    , m_busAddress(0)
    , m_busData(0)
    , m_busRw(false)
    , poweredOn(false)
    , clockPaused(false)
    : cpu(
        [this](uint16_t address) {
            return read(address);
        },
        [this](uint16_t address, uint8_t value) {
            return write(address, value);
        },
        [this](mos6502* cpu) {
            return cycle(cpu);
        }
      )
{
    memset(pinOutputs, 0, GPIO_NUM_PINS);
}

void CodeNodeNano::tick()
{
    if(!poweredOn || clockPaused)
        return;

    cyclesTarget += CLOCK_FREQUENCY / GAME_TICK_RATE;

    gpio.tickInterrupts();
    gpio.copyBuffers();

    if (gpio.isInput(uart.rxPin()))
        uart.rxIn(gpio.read(uart.rxPin())); // RX

    uart.tick();

    cpu.Run(cyclesTarget - cyclesCounter, cyclesCounter);

    memcpy(pinOutputs, gpio.pvFrontData(), GPIO_NUM_PINS);
    if (!gpio.isInput(uart.txPin()))
        pinOutputs[uart.txPin()] = uart.txOut(); // TX
}

void CodeNodeNano::cycle()
{
    if(!poweredOn || !clockPaused)
        return;

    gpio.tickInterrupts();
    gpio.copyBuffers();

    cpu.Run(1, cyclesCounter);
    cyclesTarget = cyclesCounter;

    memcpy(pinOutputs, gpio.pvFrontData(), GPIO_NUM_PINS);
}

void CodeNodeNano::reset()
{
    ram.reset();
    // rom.reset();
    gpio.reset();
    el.reset();
    uart.reset();
    memset(pinOutputs, 0, GPIO_NUM_PINS);
    cpu.Reset();
    cyclesCounter = 0;
    cyclesTarget = 0;
}

void CodeNodeNano::powerOn()
{
    if(poweredOn)
        return;

    poweredOn = true;
    reset();
}

void CodeNodeNano::powerOff()
{
    if(!poweredOn)
        return;

    poweredOn = false;
}

bool CodeNodeNano::isPoweredOn() const
{
    return poweredOn;
}

mos6502& CodeNodeNano::CPU()
{
    return cpu;
}

CNGPIO<CodeNodeNano::GPIO_NUM_PINS>& CodeNodeNano::GPIO()
{
    return gpio;
}

CNRAM<CodeNodeNano::RAM_SIZE>& CodeNodeNano::RAM()
{
    return ram;
}

CNROM<CodeNodeNano::ROM_SIZE>& CodeNodeNano::ROM()
{
    return rom;
}

CNEL<CodeNodeNano::EL_SIZE>& CodeNodeNano::EL()
{
    return el;
}

CNUART& CodeNodeNano::UART()
{
    return uart;
}

uint8_t CodeNodeNano::read(uint16_t address)
{

    m_busAddress = address;
    m_busData = 0;
    m_busRw = false;

    if(0xFFFF - ROM_SIZE < address)
        return m_busData = rom.read(address - (0x10000 - ROM_SIZE));
    else if(0x7000 <= address && address < (0x7000 + gpio.size()))
        return m_busData = gpio.read(address - 0x7000);
    else if(0x7100 <= address && address < (0x7100 + el.size()))
        m_busData = el.read(address - 0x7100);
    else if(0x7200 <= address && address < (0x7200 + uart.size()))
        return m_busData = uart.read(address - 0x7200);

    return m_busData = ram.read(address);
}

void CodeNodeNano::write(uint16_t address, uint8_t value)
{

    m_busAddress = address;
    m_busData = value;
    m_busRw = true;

    if(0xFFFF - ROM_SIZE < address)
    {
        rom.write(address - (0x10000 - ROM_SIZE), value);
        return;
    }
    else if(0x7000 <= address && address < (0x7000 + gpio.size()))
    {
        gpio.write(address - 0x7000, value);
        return;
    }
    else if(0x7100 <= address && address < (0x7100 + el.size()))
    {
        currentInstance->el.write(address - 0x7100, value);
        return;
    }
    else if(0x7200 <= address && address < (0x7200 + uart.size()))
    {
        uart.write(address - 0x7200, value);
        return;
    }

    ram.write(address, value);
}

void CodeNodeNano::cycle(mos6502* cpu)
{
    bool shouldInterrupt = gpio.shouldInterrupt();
    shouldInterrupt |= el.shouldInterrupt();
    shouldInterrupt |= uart.shouldInterrupt();

    if(shouldInterrupt)
        cpu->IRQ();
}