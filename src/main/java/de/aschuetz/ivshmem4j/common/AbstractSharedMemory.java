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
import de.aschuetz.ivshmem4j.api.PeerConnectionListener;
import de.aschuetz.ivshmem4j.api.SharedMemory;
import de.aschuetz.ivshmem4j.api.SharedMemoryException;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static de.aschuetz.ivshmem4j.common.CommonErrorCodeUtil.checkCodeCMPXCHG;
import static de.aschuetz.ivshmem4j.common.CommonErrorCodeUtil.checkCodeOK;

public abstract class AbstractSharedMemory implements SharedMemory {

    protected final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    protected final ReentrantReadWriteLock.ReadLock readLock = readWriteLock.readLock();

    protected final ReentrantReadWriteLock.WriteLock writeLock = readWriteLock.writeLock();

    protected volatile boolean closeStarted = false;

    protected final long size;

    /**
     * Native void* to the native data struct.
     */
    protected volatile long nativePointer;

    public AbstractSharedMemory(long size) {
        this.size = size;
    }

    @Override
    public long getSharedMemorySize() {
        return size;
    }


    @Override
    public boolean isAddressValid(long address) {
        return address >= 0 && address < getSharedMemorySize();
    }

    @Override
    public boolean isAddressRangeValid(long address, long size) {
        if (size <= 0) {
            return false;
        }

        if (size == 1) {
            return isAddressValid(address);
        }

        return isAddressValid(address) && isAddressValid(address + (size - 1));
    }

    @Override
    public void close() {
        closeStarted = true;
        writeLock.lock();
        try {
            close0();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean isClosed() {
        return closeStarted;
    }

    protected abstract void close0();

    @Override
    public boolean hasOwnPeerID() {
        return false;
    }

    @Override
    public int getOwnPeerID() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean supportsInterrupts() {
        return false;
    }

    @Override
    public void sendInterrupt(int aPeer, int aVector) throws SharedMemoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getOwnVectors() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isVectorValid(int vector) {
        if (!this.supportsInterrupts()) {
            return false;
        }

        if (vector < 0) {
            return false;
        }

        if (vector < this.getOwnVectors()) {
            return true;
        }

        return false;
    }

    protected void checkInterruptSupport() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerInterruptServiceRoutine(int aVector, InterruptServiceRoutine isr) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeInterruptServiceRoutine(int aVector, InterruptServiceRoutine isr) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void receiveInterrupts() throws InterruptedException, SharedMemoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean supportsConnectionListening() {
        return false;
    }

    @Override
    public boolean knowsOtherPeerVectors() {
        return false;
    }

    @Override
    public boolean knowsOtherPeers() {
        return false;
    }

    @Override
    public int[] getPeers() throws SharedMemoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getVectors(int peer) throws SharedMemoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void listenForNewConnections() throws InterruptedException, SharedMemoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerPeerConnectionListener(PeerConnectionListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removePeerConnectionListener(PeerConnectionListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOtherPeerConnected(int aPeerId) throws SharedMemoryException {
        if (getOwnPeerID() == aPeerId) {
            return true;
        }
        int[] tempPeers = getPeers();
        for (int i = 0; i < tempPeers.length; i++) {
            if (tempPeers[i] == aPeerId) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int readUnsignedShort(long offset) throws SharedMemoryException {
        return readShort(offset) & 0xFFFF;
    }

    @Override
    public int readUnsignedByte(long offset) throws SharedMemoryException {
        return read(offset) & 0xFF;
    }

    @Override
    public void memset(long offset, byte value, long len) throws SharedMemoryException {
        checkCodeOK(CommonSharedMemory.memset(nativePointer, offset, value, len));
    }

    /**
     * read writes.
     */

    @Override
    public void write(long offset, byte[] buffer, int bufferOffset, int len) throws SharedMemoryException {
        readLock.lock();
        try {
            checkCodeOK(CommonSharedMemory.write(nativePointer, offset, buffer, bufferOffset, len));
        } finally {
            readLock.unlock();
        }
    }


    @Override
    public void write(long offset, byte[] buffer) throws SharedMemoryException {
        write(offset, buffer, 0, buffer.length);
    }

    @Override
    public void write(long offset, byte aByte) throws SharedMemoryException {
        readLock.lock();
        try {
            checkCodeOK(CommonSharedMemory.write(nativePointer, offset, aByte));
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void write(long offset, int aInt) throws SharedMemoryException {
        readLock.lock();
        try {
            checkCodeOK(CommonSharedMemory.write(nativePointer, offset, aInt));
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void write(long offset, long aLong) throws SharedMemoryException {
        readLock.lock();
        try {
            checkCodeOK(CommonSharedMemory.write(nativePointer, offset, aLong));
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void write(long offset, float aFloat) throws SharedMemoryException {
        readLock.lock();
        try {
            checkCodeOK(CommonSharedMemory.write(nativePointer, offset, aFloat));
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void write(long offset, double aDouble) throws SharedMemoryException {
        readLock.lock();
        try {
            checkCodeOK(CommonSharedMemory.write(nativePointer, offset, aDouble));
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void write(long offset, short aShort) throws SharedMemoryException {
        readLock.lock();
        try {
            checkCodeOK(CommonSharedMemory.write(nativePointer, offset, aShort));
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void read(long offset, byte[] buffer, int bufferOffset, int len) throws SharedMemoryException {
        readLock.lock();
        try {
            checkCodeOK(CommonSharedMemory.read(nativePointer, offset, buffer, bufferOffset, len));
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int readInt(long offset) throws SharedMemoryException {
        readLock.lock();
        try {
            int[] tempValue = new int[1];
            checkCodeOK(CommonSharedMemory.read(nativePointer, offset, tempValue));
            return tempValue[0];
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public long readLong(long offset) throws SharedMemoryException {
        readLock.lock();
        try {
            long[] tempValue = new long[1];
            checkCodeOK(CommonSharedMemory.read(nativePointer, offset, tempValue));
            return tempValue[0];
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public float readFloat(long offset) throws SharedMemoryException {
        readLock.lock();
        try {
            float[] tempValue = new float[1];
            checkCodeOK(CommonSharedMemory.read(nativePointer, offset, tempValue));
            return tempValue[0];
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public double readDouble(long offset) throws SharedMemoryException {
        readLock.lock();
        try {
            double[] tempValue = new double[1];
            checkCodeOK(CommonSharedMemory.read(nativePointer, offset, tempValue));
            return tempValue[0];
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public short readShort(long offset) throws SharedMemoryException {
        readLock.lock();
        try {
            short[] tempValue = new short[1];
            checkCodeOK(CommonSharedMemory.read(nativePointer, offset, tempValue));
            return tempValue[0];
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public byte read(long offset) throws SharedMemoryException {
        readLock.lock();
        try {
            byte[] tempValue = new byte[1];
            checkCodeOK(CommonSharedMemory.read(nativePointer, offset, tempValue));
            return tempValue[0];
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public long getAndAdd(long offset, long aLong) throws SharedMemoryException {
        long[] tempParam = {aLong};
        checkCodeOK(CommonSharedMemory.xadd(nativePointer, offset, tempParam));
        return tempParam[0];
    }

    @Override
    public int getAndAdd(long offset, int aInt) throws SharedMemoryException {
        int[] tempParam = {aInt};
        checkCodeOK(CommonSharedMemory.xadd(nativePointer, offset, tempParam));
        return tempParam[0];
    }

    @Override
    public short getAndAdd(long offset, short aShort) throws SharedMemoryException {
        short[] tempParam = {aShort};
        checkCodeOK(CommonSharedMemory.xadd(nativePointer, offset, tempParam));
        return tempParam[0];
    }

    @Override
    public byte getAndAdd(long offset, byte aByte) throws SharedMemoryException {
        byte[] tempParam = {aByte};
        checkCodeOK(CommonSharedMemory.xadd(nativePointer, offset, tempParam));
        return tempParam[0];
    }


    public long getAndSet(long offset, long aLong) throws SharedMemoryException {
        long[] tempParam = {aLong};
        checkCodeOK(CommonSharedMemory.xchg(nativePointer, offset, tempParam));
        return tempParam[0];
    }

    @Override
    public int getAndSet(long offset, int aInt) throws SharedMemoryException {
        int[] tempParam = {aInt};
        checkCodeOK(CommonSharedMemory.xchg(nativePointer, offset, tempParam));
        return tempParam[0];
    }

    @Override
    public short getAndSet(long offset, short aShort) throws SharedMemoryException {
        short[] tempParam = {aShort};
        checkCodeOK(CommonSharedMemory.xchg(nativePointer, offset, tempParam));
        return tempParam[0];
    }

    @Override
    public byte getAndSet(long offset, byte aByte) throws SharedMemoryException {
        byte[] tempParam = {aByte};
        checkCodeOK(CommonSharedMemory.xchg(nativePointer, offset, tempParam));
        return tempParam[0];
    }

    @Override
    public boolean spinAndSet(long offset, long expect, long update, long aSpinTime, long aTimeout, TimeUnit aUnit) throws SharedMemoryException {
        long tempTimeout = Math.max(0, TimeUnit.NANOSECONDS.convert(aTimeout, aUnit));
        long tempSpin = Math.max(0, TimeUnit.MILLISECONDS.convert(aSpinTime, aUnit));
        long tempUntil = System.nanoTime() + tempTimeout;
        boolean tempWasInterrupted = false;
        do {
            if (compareAndSwap(offset, expect, update)) {
                if (tempWasInterrupted) {
                    Thread.currentThread().interrupt();
                }
                return true;
            }
            if (aSpinTime > 0) {
                try {
                    TimeUnit.MILLISECONDS.sleep(tempSpin);
                } catch (InterruptedException e) {
                    tempWasInterrupted = true;
                }
            }
        } while (aTimeout < 0 || System.nanoTime() < tempUntil);

        if (tempWasInterrupted) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    @Override
    public boolean spinAndSet(long offset, int expect, int update, long aSpinTime, long aTimeout, TimeUnit aUnit) throws SharedMemoryException {
        long tempTimeout = Math.max(0, TimeUnit.NANOSECONDS.convert(aTimeout, aUnit));
        long tempSpin = Math.max(0, TimeUnit.MILLISECONDS.convert(aSpinTime, aUnit));
        long tempUntil = System.nanoTime() + tempTimeout;
        boolean tempWasInterrupted = false;
        do {
            if (compareAndSwap(offset, expect, update)) {
                if (tempWasInterrupted) {
                    Thread.currentThread().interrupt();
                }
                return true;
            }
            if (aSpinTime > 0) {
                try {
                    TimeUnit.MILLISECONDS.sleep(tempSpin);
                } catch (InterruptedException e) {
                    tempWasInterrupted = true;
                }
            }
        } while (aTimeout < 0 || System.nanoTime() < tempUntil);

        if (tempWasInterrupted) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    @Override
    public boolean spinAndSet(long offset, short expect, short update, long aSpinTime, long aTimeout, TimeUnit aUnit) throws SharedMemoryException {
        long tempTimeout = Math.max(0, TimeUnit.NANOSECONDS.convert(aTimeout, aUnit));
        long tempSpin = Math.max(0, TimeUnit.MILLISECONDS.convert(aSpinTime, aUnit));
        long tempUntil = System.nanoTime() + tempTimeout;
        boolean tempWasInterrupted = false;
        do {
            if (compareAndSwap(offset, expect, update)) {
                if (tempWasInterrupted) {
                    Thread.currentThread().interrupt();
                }
                return true;
            }
            if (aSpinTime > 0) {
                try {
                    TimeUnit.MILLISECONDS.sleep(tempSpin);
                } catch (InterruptedException e) {
                    tempWasInterrupted = true;
                }
            }
        } while (aTimeout < 0 || System.nanoTime() < tempUntil);

        if (tempWasInterrupted) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    @Override
    public boolean spinAndSet(long offset, byte expect, byte update, long aSpinTime, long aTimeout, TimeUnit aUnit) throws SharedMemoryException {
        long tempTimeout = Math.max(0, TimeUnit.NANOSECONDS.convert(aTimeout, aUnit));
        long tempSpin = Math.max(0, TimeUnit.MILLISECONDS.convert(aSpinTime, aUnit));
        long tempUntil = System.nanoTime() + tempTimeout;
        boolean tempWasInterrupted = false;
        do {
            if (compareAndSwap(offset, expect, update)) {
                if (tempWasInterrupted) {
                    Thread.currentThread().interrupt();
                }
                return true;
            }
            if (aSpinTime > 0) {
                try {
                    TimeUnit.MILLISECONDS.sleep(tempSpin);
                } catch (InterruptedException e) {
                    tempWasInterrupted = true;
                }
            }
        } while (aTimeout < 0 || System.nanoTime() < tempUntil);

        if (tempWasInterrupted) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    @Override
    public boolean compareAndSwap(long offset, long expect, long update) throws SharedMemoryException {
        return checkCodeCMPXCHG(CommonSharedMemory.cmpxchg(nativePointer, offset, expect, update));
    }

    @Override
    public boolean compareAndSwap(long offset, int expect, int update) throws SharedMemoryException {
        return checkCodeCMPXCHG(CommonSharedMemory.cmpxchg(nativePointer, offset, expect, update));
    }

    @Override
    public boolean compareAndSwap(long offset, short expect, short update) throws SharedMemoryException {
        return checkCodeCMPXCHG(CommonSharedMemory.cmpxchg(nativePointer, offset, expect, update));
    }

    @Override
    public boolean compareAndSwap(long offset, byte expect, byte update) throws SharedMemoryException {
        return checkCodeCMPXCHG(CommonSharedMemory.cmpxchg(nativePointer, offset, expect, update));
    }

    @Override
    public boolean compareAndSwap(long offset, byte[] data) throws SharedMemoryException {
        return checkCodeCMPXCHG(CommonSharedMemory.cmpxchg16b(nativePointer, offset, data));
    }

    @Override
    public void startNecessaryThreads(Executor executor) {
        if (supportsInterrupts()) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        AbstractSharedMemory.this.receiveInterrupts();
                    } catch (Exception e) {
                        e.printStackTrace();
                        AbstractSharedMemory.this.close();
                    }
                }
            });
        }

        if (supportsConnectionListening()) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        AbstractSharedMemory.this.listenForNewConnections();
                    } catch (Exception e) {
                        e.printStackTrace();
                        AbstractSharedMemory.this.close();
                    }
                }
            });
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                (hasOwnPeerID() ? "peer=" + getOwnPeerID() : "") +
                (supportsInterrupts() ? "vectors=" + getOwnVectors() : "") +
                "size=" + size +
                " closed=" + isClosed() +
                '}';
    }
}
