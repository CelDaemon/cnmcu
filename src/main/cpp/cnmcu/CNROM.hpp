#pragma once

#include <cstddef>
#include <cstdint>
#include <algorithm>

template <size_t N>
class CNROM
{
public:
    CNROM() {}

    CNROM(CNROM&& other)
    {
        *this = std::move(other);
    }

    CNROM& operator=(CNROM&& other)
    {
        rom = other.rom;
        writeProtect = other.writeProtect;
        return *this;
    }

    size_t size() { return N; }
    std::array<uint8_t, N>& data() { return rom; }

    uint8_t read(uint16_t address) const
    {
        return address < N ? rom[address] : 0;
    }
    
    void write(uint16_t address, uint8_t value)
    {
        if(writeProtect)
            return;

        if(address < N)
            rom[address] = value;
    }

    void setWriteProtect(bool writeProtect) { this->writeProtect = writeProtect; }
    bool isWriteProtected() const { return writeProtect; }
private:
    std::array<uint8_t, N> rom{};
    bool writeProtect = true;
};