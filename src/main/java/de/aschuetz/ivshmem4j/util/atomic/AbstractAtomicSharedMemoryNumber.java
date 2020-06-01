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

package de.aschuetz.ivshmem4j.util.atomic;

import de.aschuetz.ivshmem4j.api.SharedMemory;

/**
 * Abstract superclass for all numbers (different sizes) that may reside in the shared memory that can be interacted with in an atomic way similar to the java.util.atomic package.
 */
public abstract class AbstractAtomicSharedMemoryNumber extends Number {
    protected final SharedMemory memory;

    protected final long address;

    protected AbstractAtomicSharedMemoryNumber(SharedMemory memory, long address) {
        this.memory = memory;
        this.address = address;

        if (memory.getSharedMemorySize() < address + getSize()) {
            throw new IllegalArgumentException("shared memory is too small or offset is too big to store this at the given address");
        }
    }

    public SharedMemory getMemory() {
        return memory;
    }

    public long getAddress() {
        return address;
    }

    /**
     * returns the size in byte that this atomic number will require in the shared memory.
     */
    public abstract int getSize();
}
