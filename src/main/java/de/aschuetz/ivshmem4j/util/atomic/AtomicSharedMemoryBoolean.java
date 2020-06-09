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
import de.aschuetz.ivshmem4j.api.SharedMemoryException;
import de.aschuetz.ivshmem4j.api.SharedMemoryRuntimeException;

/**
 * Atomic Boolean that resides Shared Memory.
 * All operations are guaranteed atomic even with other Applications modifying the same Boolean.
 * If an external write writes anything but a 0 or 1 to the address then this AtomicBoolean will be false.
 */
public class AtomicSharedMemoryBoolean {


    protected final SharedMemory memory;

    protected final long address;

    /**
     * The shared memory must be big enough to hold 1 bytes at the given address.
     */
    public AtomicSharedMemoryBoolean(SharedMemory memory, long address) {
        this.memory = memory;
        this.address = address;

        if (memory.getSharedMemorySize() < address + 1) {
            throw new IllegalArgumentException("shared memory is too small or offset is too big to store this at the given address");
        }
    }

    public void set(boolean aValue) {
        try {
            memory.write(address, btn(aValue));
        } catch (SharedMemoryException e) {
            throw new SharedMemoryRuntimeException(e);
        }
    }

    public boolean get() {
        try {
            return ntb(memory.read(address));
        } catch (SharedMemoryException e) {
            throw new SharedMemoryRuntimeException(e);
        }
    }

    public boolean getAndSet(boolean val) {
        try {
            return ntb(memory.getAndSet(address, btn(val)));
        } catch (SharedMemoryException e) {
            throw new SharedMemoryRuntimeException(e);
        }
    }

    public boolean compareAndSwap(boolean expect, boolean update) {
        try {
            if (!memory.compareAndSet(address, btn(expect), btn(update))) {
                if (expect) {
                    return false;
                }

                byte tempVal = memory.read(address);
                if (tempVal == 0 || tempVal == 1) {
                    return false;
                }

                return memory.compareAndSet(address, tempVal, (byte) 0);
            }
            return true;
        } catch (SharedMemoryException e) {
            throw new SharedMemoryRuntimeException(e);
        }
    }

    private boolean ntb(byte num) {
        return num == 1 ? true : false;
    }

    private byte btn(boolean bool) {
        return bool ? (byte) 1 : (byte) 0;
    }

    @Override
    public String toString() {
        return String.valueOf(get());
    }
}
