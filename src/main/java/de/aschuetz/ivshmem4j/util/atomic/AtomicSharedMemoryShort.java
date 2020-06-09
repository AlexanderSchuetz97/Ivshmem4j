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
 * Atomic Short that resides Shared Memory.
 * All operations are guaranteed atomic even with other Applications modifying the same Short.
 */
public class AtomicSharedMemoryShort extends AbstractAtomicSharedMemoryNumber {

    /**
     * The shared memory must be big enough to hold 2 bytes at the given address.
     */
    public AtomicSharedMemoryShort(SharedMemory memory, long address) {
        super(memory, address);
    }

    @Override
    public int getSize() {
        return 2;
    }

    public void set(int aValue) {
        try {
            memory.write(address, aValue);
        } catch (SharedMemoryException e) {
            throw new SharedMemoryRuntimeException(e);
        }
    }

    public short get() {
        try {
            return memory.readShort(address);
        } catch (SharedMemoryException e) {
            throw new SharedMemoryRuntimeException(e);
        }
    }

    public short getAndSet(short val) {
        try {
            return memory.getAndSet(address, val);
        } catch (SharedMemoryException e) {
            throw new SharedMemoryRuntimeException(e);
        }
    }

    public int getUnsigned() {
        try {
            return memory.readUnsignedShort(address);
        } catch (SharedMemoryException e) {
            throw new SharedMemoryRuntimeException(e);
        }
    }

    public short decrementAndGet() {
        try {
            return (short) (memory.getAndAdd(address, (short) -1) - 1);
        } catch (SharedMemoryException e) {
            throw new SharedMemoryRuntimeException(e);
        }
    }

    public short getAndDecrement() {
        try {
            return memory.getAndAdd(address, (short) -1);
        } catch (SharedMemoryException e) {
            throw new SharedMemoryRuntimeException(e);
        }
    }

    public short incrementAndGet() {
        try {
            return (short) (memory.getAndAdd(address, (short) 1) + 1);
        } catch (SharedMemoryException e) {
            throw new SharedMemoryRuntimeException(e);
        }
    }

    public short getAndIncrement() {
        try {
            return memory.getAndAdd(address, (short) 1);
        } catch (SharedMemoryException e) {
            throw new SharedMemoryRuntimeException(e);
        }
    }

    public boolean compareAndSwap(short expect, short update) {
        try {
            return memory.compareAndSet(address, expect, update);
        } catch (SharedMemoryException e) {
            throw new SharedMemoryRuntimeException(e);
        }
    }

    public short addAndGet(short delta) {
        try {
            return (short) (memory.getAndAdd(address, delta) + delta);
        } catch (SharedMemoryException e) {
            throw new SharedMemoryRuntimeException(e);
        }
    }

    public short getAndAdd(short delta) {
        try {
            return memory.getAndAdd(address, delta);
        } catch (SharedMemoryException e) {
            throw new SharedMemoryRuntimeException(e);
        }
    }

    @Override
    public int intValue() {
        return get();
    }

    @Override
    public long longValue() {
        return get();
    }

    @Override
    public float floatValue() {
        return (float) get();
    }

    @Override
    public double doubleValue() {
        return (double) get();
    }

    @Override
    public String toString() {
        return String.valueOf(get());
    }
}
