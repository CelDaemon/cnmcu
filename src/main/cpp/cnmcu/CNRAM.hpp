#pragma once

#include <cstddef>
#include <cstdint>
#include <algorithm>

template <size_t N>
class CNRAM
{
public:
    void reset()
    {
        ram.fill(0);
    }

    CNRAM() = default;

    CNRAM(CNRAM const&) = delete;
    CNRAM& operator=(CNRAM const&) = delete;

    size_t size() const { return N; }
    std::array<uint8_t, N>& data() { return ram; }

    uint8_t read(uint16_t address) const { return address < N ? ram[address] : 0; }
    void write(uint16_t address, uint8_t value) { if(address < N) ram[address] = value; }
private:
    std::array<uint8_t, N> ram{};
};