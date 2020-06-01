/*
 * Copyright Alexander Schütz, 2020
 *
 * This file is part of Ivshmem4j.
 *
 * Ivshmem4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Ivshmem4j is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * A copy of the GNU General Public License should be provided
 * in the COPYING file in top level directory of Ivshmem4j.
 * If not, see <https://www.gnu.org/licenses/>.
 */


#ifndef UTIL_ATOMICS_H_
#define UTIL_ATOMICS_H_
#include <stdint.h>
#include <stdbool.h>


uint8_t xadd1b(uint8_t* ptr, uint8_t value);

uint16_t xadd2b(uint16_t* ptr, uint16_t value);

uint32_t xadd4b(uint32_t* ptr, uint32_t value);

uint64_t xadd8b(uint64_t* ptr, uint64_t value);

bool cmpxchg8b(uint64_t* ptr, uint64_t expect, uint64_t update);

bool cmpxchg4b(uint32_t* ptr, uint32_t expect, uint32_t update);

bool cmpxchg2b(uint16_t* ptr, uint16_t expect, uint16_t update);

bool cmpxchg1b(uint8_t* ptr, uint8_t expect, uint8_t update);

bool cmpxchg16b(void* ptr, uint64_t* value);

uint8_t xchg1b(uint8_t* ptr, uint8_t value);

uint16_t xchg2b(uint16_t* ptr, uint16_t value);

uint32_t xchg4b(uint32_t* ptr, uint32_t value);

uint64_t xchg8b(uint64_t* ptr, uint64_t value);

#endif /* UTIL_ATOMICS_H_ */
