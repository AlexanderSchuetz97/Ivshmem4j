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

package de.aschuetz.ivshmem4j.windows;

import de.aschuetz.ivshmem4j.api.SharedMemoryException;
import de.aschuetz.ivshmem4j.common.AbstractSharedMemoryWithInterrupts;


/**
 * Windows PCI Device impl of Shared Memory.
 * May support interrupts if using ivshmem-doorbell.
 */
public class IvshmemMappedWindowsDevice extends AbstractSharedMemoryWithInterrupts {

    private final IvshmemWindowsDevice device;

    IvshmemMappedWindowsDevice(IvshmemWindowsDevice device, long nativeHandle, int aPeer, int aVectors) {
        super(device.getSharedMemorySize(), aPeer, aVectors);
        this.device = device;
        this.nativePointer = nativeHandle;
    }

    public IvshmemWindowsDevice getDevice() {
        return device;
    }

    @Override
    public void sendInterrupt(int aPeer, int aVector) throws SharedMemoryException {
        checkInterruptSupport();
        readLock.lock();
        try {
            WindowsErrorCodeUtil.checkCodeOK(WindowsSharedMemory.sendInterrupt(nativePointer, aVector, aPeer));
        } finally {
            readLock.unlock();
        }
    }

    @Override
    protected boolean pollInterrupt0(int[] buffer) throws SharedMemoryException {
        return WindowsErrorCodeUtil.checkCodePollInterrupt(WindowsSharedMemory.pollInterrupt(nativePointer, buffer));
    }

    @Override
    protected void close0() {
        if (nativePointer == 0) {
            return;
        }
        WindowsSharedMemory.close(nativePointer);
        nativePointer = 0;
    }
}
