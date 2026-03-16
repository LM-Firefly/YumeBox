#include "bridge_helper.h"
#include <cstdint>

constexpr uint64_t GB_THRESH = 1042ull * 1024ull * 1024ull;
constexpr uint64_t MB_THRESH = 1024ull * 1024ull;
constexpr uint64_t KB_THRESH = 1024ull;
constexpr uint64_t SCALE = 100u;
constexpr uint64_t VAL_MASK = 0x3FFFFFFFull;

uint64_t down_scale_traffic(uint64_t value) noexcept {
    if (value > GB_THRESH) return ((value * SCALE / 1024ull / 1024ull / 1024ull) & VAL_MASK) | (3ull << 30);
    if (value > MB_THRESH) return ((value * SCALE / 1024ull / 1024ull) & VAL_MASK) | (2ull << 30);
    if (value > KB_THRESH) return ((value * SCALE / 1024ull) & VAL_MASK) | (1ull << 30);
    return value & VAL_MASK;
}