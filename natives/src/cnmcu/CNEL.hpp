#pragma once

#include <cstring>

template<size_t N>
class CNEL
{
public:
    void reset()
    {
        iclRegisters.fill(0);
        iflRegisters.fill(0);
    }

    CNEL() = default;

    CNEL(CNEL const&) = delete;
    CNEL& operator=(CNEL const&) = delete;

private:
    std::array<uint8_t, N> iclRegisters{};
    std::array<uint8_t, N> iflRegisters{};
public:
    size_t size() const { return N * 2; }

    std::array<uint8_t, N>& iclRegistersData() { return iclRegisters; }
    std::array<uint8_t, N>& iflRegistersData() { return iflRegisters; }

    void triggerEvent(int eventId)
    {
        if(N * 8 <= eventId)
            return;

        iflRegisters[eventId / 8] |= 1 << (eventId % 8);
    }

    bool shouldInterrupt() const
    {
        for(size_t i = 0; i < N; i++)
            if(iflRegisters[i] & iclRegisters[i])
                return true;

        return false;
    }

    uint8_t read(uint16_t address) const
    {
        if(N * 2 <= address)
            return 0;

        if(address < N)
            return iclRegisters[address];
        
        return iflRegisters[address - N];
    }

    void write(uint16_t address, uint8_t value)
    {
        if(N * 2 <= address)
            return;

        if(address < N)
            iclRegisters[address] = value;
        else
            iflRegisters[address - N] &= ~value;
    }
};