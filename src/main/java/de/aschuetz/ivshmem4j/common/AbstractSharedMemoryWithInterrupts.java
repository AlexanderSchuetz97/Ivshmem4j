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

package de.aschuetz.ivshmem4j.common;

import de.aschuetz.ivshmem4j.api.InterruptServiceRoutine;
import de.aschuetz.ivshmem4j.api.SharedMemoryException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract Internal Impl for Shared Memory that has interrupt capability.
 */
public abstract class AbstractSharedMemoryWithInterrupts extends AbstractSharedMemoryWithPeerID {

    protected final List<InterruptServiceRoutine>[] interruptServiceRoutines;

    protected final int vectors;

    /**
     * Atomic flag to prevent more than one thread from polling interrupts at the same time.
     */
    private AtomicBoolean isPollingInterrupts = new AtomicBoolean(false);

    protected AbstractSharedMemoryWithInterrupts(long aSize, int aPeerId, int aVectorCount) {
        super(aSize, aPeerId);
        this.interruptServiceRoutines = new List[aVectorCount];
        vectors = aVectorCount;
    }


    @Override
    public int getOwnVectors() {
        return vectors;
    }

    @Override
    public boolean supportsInterrupts() {
        return vectors != 0;
    }

    protected void checkInterruptSupport() {
        if (vectors == 0) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void registerInterruptServiceRoutine(int aVector, InterruptServiceRoutine isr) {
        checkInterruptSupport();

        if (aVector >= vectors) {
            throw new IllegalArgumentException("Vector Too Big");
        }

        if (isr == null) {
            throw new NullPointerException("Isr is null");
        }

        synchronized (interruptServiceRoutines) {
            List<InterruptServiceRoutine> tempList = interruptServiceRoutines[aVector];
            if (tempList == null) {
                tempList = new ArrayList<>();
                interruptServiceRoutines[aVector] = tempList;
            }
            tempList.add(isr);
        }
    }

    @Override
    public void removeInterruptServiceRoutine(int aVector, InterruptServiceRoutine isr) {
        checkInterruptSupport();

        if (aVector >= vectors) {
            throw new IllegalArgumentException("Vector Too Big");
        }

        if (isr == null) {
            throw new NullPointerException("Isr is null");
        }

        synchronized (interruptServiceRoutines) {
            List<InterruptServiceRoutine> tempList = interruptServiceRoutines[aVector];
            if (tempList == null) {
                return;
            }
            tempList.remove(isr);
        }
    }


    @Override
    public void receiveInterrupts() throws InterruptedException, SharedMemoryException {
        checkInterruptSupport();


        if (isClosed()) {
            throw new IllegalStateException("Shared memory is closed!");
        }

        if (!isPollingInterrupts.compareAndSet(false, true)) {
            throw new IllegalStateException("More than one thread cannot be polling interrupts at the same time.");
        }


        readLock.lock();
        try {
            int[] tempResult = new int[vectors + 1];
            while (!closeStarted) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                if (pollInterrupt0(tempResult)) {
                    continue;
                }

                synchronized (interruptServiceRoutines) {
                    for (int i = 0; i < tempResult[0]; i++) {
                        List<InterruptServiceRoutine> tempList = interruptServiceRoutines[tempResult[i + 1]];
                        if (tempList == null) {
                            continue;
                        }

                        for (InterruptServiceRoutine tempIsr : tempList) {
                            tempIsr.onInterrupt(tempResult[i + 1]);
                        }
                    }
                }
            }
        } finally {
            isPollingInterrupts.set(false);
            readLock.unlock();
        }
    }

    protected abstract boolean pollInterrupt0(int[] buffer) throws SharedMemoryException;
}
