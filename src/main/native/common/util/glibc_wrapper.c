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

#include "glibc_wrapper.h"
#include <string.h>
#include "inline.h"



//Prevents a dependancy to GLIBC_2.14...
#if defined(linux) && (defined(__amd64__))
asm (".symver memcpy, memcpy@GLIBC_2.2.5");
#endif
FFINLINE void* wrap_memcpy(void *destination, const void *source, size_t len) {
    return memcpy(destination, source, len);
}
