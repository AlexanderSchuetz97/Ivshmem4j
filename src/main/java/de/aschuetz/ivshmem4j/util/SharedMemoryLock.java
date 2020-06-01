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

package de.aschuetz.ivshmem4j.util;

import de.aschuetz.ivshmem4j.api.*;
import de.aschuetz.ivshmem4j.common.ErrorCodeEnum;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Rentrant lock that resides within shared memory.
 * This lock uses a spin lock implementation with the optional support for interrupts if the shared memory supports it
 * and the user desires it by using the proper constructor.
 * The data structure uses 4 bytes at its given address.
 */
public class SharedMemoryLock implements Lock {

    private final SharedMemory memory;

    private final long address;

    /**
     * Flag if our "application/instance" has locked the lock.
     */
    private boolean lockedByUs;

    /**
     * flag if the lock is broken due to SharedMemoryException.
     */
    private boolean broken = false;

    /**
     * Delegate lock for in Application Thread Synchronization.
     */
    private ReentrantLock delegate = new ReentrantLock();

    /**
     * Lock for synchronising interrupts. may be held while delegate lock is also held.
     */
    private ReentrantLock interruptLock = new ReentrantLock();

    /**
     * Condition to signal on whenever the lock changes.
     */
    private Condition interruptCondition = interruptLock.newCondition();

    /**
     * Spin time. Not used for active spinning unlike in the SpinLock but acts as a fallback when the interrupts are
     * unreliable. If no interrupts arrives for the spinTimeWithoutInterrupts check again if its possible to lock the lock but if a
     * interrupt arrives check again immediately.
     */
    private final long spinTime;

    /**
     * interrupt vector used for communication.
     */
    private final int vector;

    /**
     * Collection which peers should receive an interrupt when this application unlocks the lock.
     */
    private final Collection<Integer> peers;

    private final InterruptServiceRoutine interruptServiceRoutine = new InterruptServiceRoutine() {
        @Override
        public void onInterrupt(int aInterrupt) {
            interruptLock.lock();
            interruptCondition.signalAll();
            interruptLock.unlock();
        }
    };

    private final boolean useInterrupts;

    /**
     * Creates a new shared memory lock that is just a pure spin lock and doesnt use interrupts.
     */
    public SharedMemoryLock(SharedMemory aMemory, long aAddress, long aSpinTime) {
        this(aMemory, aAddress, -1, Collections.<Integer>emptyList(), aSpinTime);
    }

    /**
     * Creates a new Shared Memory Lock at the given address. If the aVector parameter is -1 or the memory doesnt support interrupts
     * then this lock will be a pure spin lock otherwise in addition to beeing a spin lock this lock will also use interrupts.
     * Generally if a SharedMemory supports interrupts then the spin time can be set much high (to avoid wasting cpu time).
     * If Interrupts are used then new peers that interacts with this lock should/removed be added via addPeer/removePeer or they
     * will not receive interrupts causing them to react slower.
     */
    public SharedMemoryLock(SharedMemory aMemory, long aAddress, int aVector, Collection<Integer> somePeers, long aSpinTime) {
        if (!aMemory.isAddressRangeValid(aAddress, 4)) {
            throw new IllegalArgumentException("address is out of bounds!");
        }
        memory = aMemory;
        address = aAddress;
        spinTime = aSpinTime;
        vector = aVector;
        peers = somePeers;
        useInterrupts = vector != -1 && memory.supportsInterrupts();
    }

    private void sendInterrupt() throws SharedMemoryException {
        if (!useInterrupts) {
            return;
        }
        synchronized (peers) {
            for (int i : peers) {
                try {
                    memory.sendInterrupt(i, vector);
                } catch (SharedMemoryException exc) {
                    ErrorCode tempCode = exc.getCode();
                    //One of the other peers disconnected. We expect to be updated soon by removePeer...
                    if (tempCode == ErrorCodeEnum.PEER_NOT_FOUND) {
                        continue;
                    }
                    throw exc;
                }
            }
        }
    }

    private void registerISR() {
        if (!useInterrupts) {
            return;
        }

        memory.registerInterruptServiceRoutine(vector, interruptServiceRoutine);
    }

    private void removeISR() {
        if (!useInterrupts) {
            return;
        }
        memory.removeInterruptServiceRoutine(vector, interruptServiceRoutine);
    }

    /**
     * Add a peer to be notified when the lock is locked. (This may be called asynchrounsly)
     */
    public void addPeer(int peer) {
        if (!useInterrupts) {
            return;
        }
        synchronized (peers) {
            peers.add(peer);
        }
    }

    /**
     * Remove a peer to be notified when the lock is locked. (This may be called asynchrounsly)
     */
    public void removePeer(int peer) {
        if (!useInterrupts) {
            return;
        }
        synchronized (peers) {
            peers.remove(peer);
        }
    }

    private void incrementRecursive() throws SharedMemoryException {
        int tempCount = delegate.getHoldCount();
        if (!memory.compareAndSwap(address, tempCount - 1, tempCount)) {
            fullyUnlockDelegate();
            throw new SharedMemoryRuntimeException("Memory at address" + address + " is corrupt! This lock is now broken");
        }
    }

    @Override
    public void lock() {
        try {
            if (broken) {
                throw new SharedMemoryRuntimeException("Lock is broken!");
            }

            delegate.lock();
            if (lockedByUs) {
                incrementRecursive();
                return;
            }

            boolean tempInterrupted = false;
            while (!memory.compareAndSwap(address, 0, 1)) {
                try {

                    interruptLock.lock();
                    try {
                        registerISR();
                        if (memory.compareAndSwap(address, 0, 1)) {
                            break;
                        }
                        interruptCondition.await(spinTime, TimeUnit.MILLISECONDS);
                    } finally {
                        removeISR();
                        interruptLock.unlock();

                    }

                } catch (InterruptedException e) {
                    tempInterrupted = true;
                    continue;
                }
            }

            if (tempInterrupted) {
                Thread.currentThread().interrupt();
            }

            lockedByUs = true;

        } catch (SharedMemoryException e) {
            fullyUnlockDelegate();
            throw new SharedMemoryRuntimeException(e);
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        try {
            if (broken) {
                throw new SharedMemoryRuntimeException("Lock is broken!");
            }

            delegate.lockInterruptibly();
            if (lockedByUs) {
                incrementRecursive();
                return;
            }

            memory.spinAndSet(address, 0, 1, spinTime, -1, TimeUnit.MILLISECONDS);


            lockedByUs = true;

        } catch (SharedMemoryException e) {
            fullyUnlockDelegate();
            throw new SharedMemoryRuntimeException(e);
        }
    }

    @Override
    public boolean tryLock() {
        try {
            if (broken) {
                throw new SharedMemoryRuntimeException("Lock is broken!");
            }

            if (!delegate.tryLock()) {
                return false;
            }

            if (lockedByUs) {
                incrementRecursive();
                return true;
            }

            if (!memory.compareAndSwap(address, 0, 1)) {
                delegate.unlock();
                return false;
            }


            lockedByUs = true;
            return true;
        } catch (SharedMemoryException e) {
            fullyUnlockDelegate();
            throw new SharedMemoryRuntimeException(e);
        }
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {

        try {
            if (broken) {
                throw new SharedMemoryRuntimeException("Lock is broken!");
            }

            long tempStart = System.currentTimeMillis();

            if (!delegate.tryLock(time, unit)) {
                return false;
            }

            if (lockedByUs) {
                incrementRecursive();
                return true;
            }

            long tempTime = TimeUnit.MILLISECONDS.convert(time, unit);

            boolean tempInterrupted = false;
            while (!memory.compareAndSwap(address, 0, 1)) {
                long tempDuration = System.currentTimeMillis() - tempStart;
                if (tempTime < tempDuration) {
                    delegate.unlock();
                    return false;
                }
                try {
                    interruptLock.lock();
                    try {
                        registerISR();
                        if (memory.compareAndSwap(address, 0, 1)) {
                            break;
                        }
                        interruptCondition.await(Math.min(spinTime, (tempTime - tempDuration)), TimeUnit.MILLISECONDS);
                    } finally {
                        removeISR();
                        interruptLock.unlock();
                    }

                } catch (InterruptedException e) {
                    tempInterrupted = true;
                    continue;
                }
            }

            if (tempInterrupted) {
                Thread.currentThread().interrupt();
            }


            lockedByUs = true;
            return true;
        } catch (SharedMemoryException e) {
            fullyUnlockDelegate();
            throw new SharedMemoryRuntimeException(e);
        }
    }

    @Override
    public void unlock() {
        if (broken) {
            return;
        }

        if (!lockedByUs || !delegate.isHeldByCurrentThread()) {
            throw new IllegalStateException("Not locked");
        }

        int tempOld;
        try {
            tempOld = memory.getAndAdd(address, -1);
        } catch (SharedMemoryException e) {
            fullyUnlockDelegate();
            //Silently eat this to not cause errors in finally blocks...
            return;
        }

        if (tempOld == 1) {
            lockedByUs = false;
            try {
                sendInterrupt();
            } catch (SharedMemoryException e) {
                fullyUnlockDelegate();
                return;
            }
        }

        delegate.unlock();
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }

    private void fullyUnlockDelegate() {
        broken = true;
        while (delegate.isHeldByCurrentThread()) {
            delegate.unlock();
        }
        try {
            memory.write(address, -1);
            sendInterrupt();
        } catch (SharedMemoryException e) {
            //valiant effort was made...
        }
    }
}
