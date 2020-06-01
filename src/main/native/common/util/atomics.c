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

#include "atomics.h"
#include "inline.h"

uint8_t FFINLINE xadd1b(uint8_t* ptr, uint8_t value) {
	__asm__ __volatile__ ("LOCK; XADD %[value], %[pointer];"
				: [pointer] "+m" (*ptr), [value] "+r" (value)
				:
				: "memory");
	return value;
}

uint16_t FFINLINE xadd2b(uint16_t* ptr, uint16_t value) {
	__asm__ __volatile__ ("LOCK; XADD %[value], %[pointer];"
				: [pointer] "+m" (*ptr), [value] "+r" (value)
				:
				: "memory");
	return value;
}

uint32_t FFINLINE xadd4b(uint32_t* ptr, uint32_t value) {
	__asm__ __volatile__ ("LOCK; XADD %[value], %[pointer];"
				: [pointer] "+m" (*ptr), [value] "+r" (value)
				:
				: "memory");
	return value;
}

uint64_t FFINLINE xadd8b(uint64_t* ptr, uint64_t value) {
	__asm__ __volatile__ ("LOCK; XADD %[value], %[pointer];"
				: [pointer] "+m" (*ptr), [value] "+r" (value)
				:
				: "memory");
	return value;
}

bool FFINLINE cmpxchg8b(uint64_t* ptr, uint64_t expect, uint64_t update) {
	register uint64_t accumulator asm ("rax") = expect;
	bool flag;
	__asm__ __volatile__ (
			"LOCK; CMPXCHG %[update], %[pointer];"
			"SETZ %[flag];"
				: [pointer] "+m" (*ptr), [flag] "=r" (flag)
				: [expect] "r" (accumulator), [update] "r" (update)
				: "cc", "memory");
	return flag;
}

bool FFINLINE cmpxchg4b(uint32_t* ptr, uint32_t expect, uint32_t update) {
	register uint64_t accumulator asm ("eax") = expect;
	bool flag;
	__asm__ __volatile__ (
			"LOCK; CMPXCHG %[update], %[pointer];"
			"SETZ %[flag];"
				: [pointer] "+m" (*ptr), [flag] "=r" (flag)
				: [expect] "r" (accumulator), [update] "r" (update)
				: "cc", "memory");
	return flag;
}

bool FFINLINE cmpxchg2b(uint16_t* ptr, uint16_t expect, uint16_t update) {
	register uint64_t accumulator asm ("ax") = expect;
	bool flag;
	__asm__ __volatile__ (
			"LOCK; CMPXCHG %[update], %[pointer];"
			"SETZ %[flag];"
				: [pointer] "+m" (*ptr), [flag] "=r" (flag)
				: [expect] "r" (accumulator), [update] "r" (update)
				: "cc", "memory");
	return flag;
}

bool FFINLINE cmpxchg1b(uint8_t* ptr, uint8_t expect, uint8_t update) {
	register uint64_t accumulator asm ("al") = expect;
	bool flag;
	__asm__ __volatile__ (
			"LOCK; CMPXCHG %[update], %[pointer];"
			"SETZ %[flag];"
				: [pointer] "+m" (*ptr), [flag] "=r" (flag)
				: [expect] "r" (accumulator), [update] "r" (update)
				: "cc", "memory");
	return flag;
}

//Exists only as a hint for compiler.
struct uint128 {
	char data[16];
};

bool FFINLINE cmpxchg16b(void* ptr, uint64_t* value) {
	register uint64_t rax asm ("rax") = value[0];
	register uint64_t rdx asm ("rdx") = value[1];
	register uint64_t rbx asm ("rbx") = value[2];
	register uint64_t rcx asm ("rcx") = value[3];
	bool flag;
	__asm__ __volatile__ (
			"LOCK; CMPXCHG16B %[pointer];"
			"SETZ %[flag];"
				: [pointer] "+m" (*((struct uint128*)ptr)), [flag] "=r" (flag)
				: [expect1] "r" (rdx), [expect2] "r" (rax), [update1] "r" (rcx), [update2] "r" (rbx)
				: "cc", "memory");
	return flag;
}



uint8_t FFINLINE xchg1b(uint8_t* ptr, uint8_t value) {
	__asm__ __volatile__ ("LOCK; XCHG %[value], %[pointer];"
				: [pointer] "+m" (*ptr), [value] "+r" (value)
				:
				: "memory");
	return value;
}

uint16_t FFINLINE xchg2b(uint16_t* ptr, uint16_t value) {
	__asm__ __volatile__ ("LOCK; XCHG %[value], %[pointer];"
				: [pointer] "+m" (*ptr), [value] "+r" (value)
				:
				: "memory");
	return value;
}

uint32_t FFINLINE xchg4b(uint32_t* ptr, uint32_t value) {
	__asm__ __volatile__ ("LOCK; XCHG %[value], %[pointer];"
				: [pointer] "+m" (*ptr), [value] "+r" (value)
				:
				: "memory");
	return value;
}

uint64_t FFINLINE xchg8b(uint64_t* ptr, uint64_t value) {
	__asm__ __volatile__ ("LOCK; XCHG %[value], %[pointer];"
				: [pointer] "+m" (*ptr), [value] "+r" (value)
				:
				: "memory");
	return value;
}
