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

package de.aschuetz.ivshmem4j.api;

import java.io.Closeable;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public interface SharedMemory extends Closeable {

    /**
     * Size in bytes of the SharedMemory.
     */
    long getSharedMemorySize();

    /**
     * returns true if the given offset address is valid. false if the address is out of bounds for this shared memory.
     * calling with a negative address will always yield false.
     */
    boolean isAddressValid(long address);

    /**
     * returns true if the given offset address is valid and makes sure that size bytes can be accessed at the given address without going out of bounds.
     * the first byte that is accessed will be accessible directly at address and the last byte is accessible at address+size-1 thus calling this method
     * with a size of 1 is equal to calling #isAddressValid directly. calling with a size below 1 will always yield false.
     */
    boolean isAddressRangeValid(long address, long size);

    /**
     * returns true if this SharedMemory has a peer id. If this returns false then calls to getOwnPeerID will throw an UnsupportedOperationException.
     */
    boolean hasOwnPeerID();

    /**
     * The own Peer ID.
     */
    int getOwnPeerID();

    /**
     * returns true if this SharedMemory knows the other peers that are also accessing the Shared memory. If this returns
     * false then getPeers will throw an UnsupportedOperationException
     */
    boolean knowsOtherPeers();

    /**
     * Returns true if the given peer id is connected to this shared memory.
     */
    boolean isOtherPeerConnected(int aPeerId) throws SharedMemoryException;

    /**
     * returns the other peers that are also accessing the SharedMemory.
     */
    int[] getPeers() throws SharedMemoryException;

    /**
     * returns true if this SharedMemory knows the vectors of other peers.
     * Generally it can be assumed that the vectors of other peers mirror the vectors of this peer.
     * if this method returns false then getVectors will throw an UnsupportedOperationException.
     */
    boolean knowsOtherPeerVectors();

    /**
     * returns the vectors of the peer.
     */
    int getVectors(int peer) throws SharedMemoryException;

    /**
     * The amount of own interrupt vectors that this SharedMemory can receive.
     */
    int getOwnVectors();

    /**
     * returns true if the given vector is valid can be used to send interrupts on to other peers. will always return
     * false if the SharedMemory doesnt support interrupts.
     */
    boolean isVectorValid(int vector);

    /**
     * Closes this shared memory device. Due to synchronization this call might take several seconds before returning.
     */
    void close();

    /**
     * returns true if the shared memory is closed and any further interaction with it will cause an exception.
     */
    boolean isClosed();

    /*
     * Writes len bytes with the given value to the offset.
     */
    void set(long offset, byte value, long len) throws SharedMemoryException;

    /**
     * writes len bytes starting from the index bufferOffset from the buffer to the offset address in the shared memory.
     */
    void write(long offset, byte[] buffer, int bufferOffset, int len) throws SharedMemoryException;

    /**
     * writes all bytes from the buffer to the offset address in the shared memory.
     */
    void write(long offset, byte[] buffer) throws SharedMemoryException;

    /**
     * writes a single byte to the offset address.
     */
    void write(long offset, byte aByte) throws SharedMemoryException;

    /**
     * writes 4 bytes to the offset address.
     */
    void write(long offset, int aInt) throws SharedMemoryException;

    /**
     * writes 8 bytes to the offset address.
     */
    void write(long offset, long aLong) throws SharedMemoryException;

    /**
     * writes 4 bytes to the offset address.
     */
    void write(long offset, float aFloat) throws SharedMemoryException;

    /**
     * writes 8 bytes to the offset address.
     */
    void write(long offset, double aDouble) throws SharedMemoryException;

    /**
     * writes 2 bytes to the offset address.
     */
    void write(long offset, short aShort) throws SharedMemoryException;

    /**
     * reads len bytes from the offset address and stores them into the buffer starting at bufferOffset.
     */
    void read(long offset, byte[] buffer, int bufferOffset, int len) throws SharedMemoryException;

    /**
     * read 4 bytes from the offset address.
     */
    int readInt(long offset) throws SharedMemoryException;

    /**
     * read 8 bytes from the offset address.
     */
    long readLong(long offset) throws SharedMemoryException;

    /**
     * read 4 bytes from the offset address.
     */
    float readFloat(long offset) throws SharedMemoryException;

    /**
     * read 8 bytes from the offset address.
     */
    double readDouble(long offset) throws SharedMemoryException;

    /**
     * read 2 bytes from the offset address.
     */
    short readShort(long offset) throws SharedMemoryException;

    /**
     * reads 2 bytes from the offset address and treats them as a unsigned short.
     * Note this is always equivalent to readShort(offset) & 0xFFFF
     */
    int readUnsignedShort(long offset) throws SharedMemoryException;

    /**
     * read 1 byte from the offset address.
     */
    byte read(long offset) throws SharedMemoryException;

    /**
     * read 1 byte from the offset address and tread it as a unsigned byte.
     * Note this is always equivalent to read(offset) & 0xFF
     */
    int readUnsignedByte(long offset) throws SharedMemoryException;

    /**
     * adds the parameter to the long at offset. returns the previous value. this operation is atomic.
     */
    long getAndAdd(long offset, long aLong) throws SharedMemoryException;

    /**
     * adds the parameter to the int at offset. returns the previous value. this operation is atomic.
     */
    int getAndAdd(long offset, int aInt) throws SharedMemoryException;

    /**
     * adds the parameter to the short at offset. returns the previous value. this operation is atomic.
     */
    short getAndAdd(long offset, short aShort) throws SharedMemoryException;

    /**
     * adds the parameter to the byte at offset. returns the previous value. this operation is atomic.
     */
    byte getAndAdd(long offset, byte aByte) throws SharedMemoryException;

    /**
     * sets the value at offset to the given long. returns the previous value. this operation is atomic.
     */
    long getAndSet(long offset, long aLong) throws SharedMemoryException;

    /**
     * sets the value at offset to the given int. returns the previous value. this operation is atomic.
     */
    int getAndSet(long offset, int aInt) throws SharedMemoryException;

    /**
     * sets the value at offset to the given short. returns the previous value. this operation is atomic.
     */
    short getAndSet(long offset, short aShort) throws SharedMemoryException;

    /**
     * sets the value at offset to the given byte. returns the previous value. this operation is atomic.
     */
    byte getAndSet(long offset, byte aByte) throws SharedMemoryException;

    /*
     * compares the value at offset with expect if equal sets the value at offset to update and returns true otherwise return false.
     * this operation is atomic.
     */
    boolean compareAndSet(long offset, long expect, long update) throws SharedMemoryException;

    /*
     * compares the value at offset with expect if equal sets the value at offset to update and returns true otherwise return false.
     * this operation is atomic.
     */
    boolean compareAndSet(long offset, int expect, int update) throws SharedMemoryException;

    /*
     * compares the value at offset with expect if equal sets the value at offset to update and returns true otherwise return false.
     * this operation is atomic.
     */
    boolean compareAndSet(long offset, short expect, short update) throws SharedMemoryException;

    /*
     * compares the value at offset with expect if equal sets the value at offset to update and returns true otherwise return false.
     * this operation is atomic.
     */
    boolean compareAndSet(long offset, byte expect, byte update) throws SharedMemoryException;

    /*
     * compares the value of offset with the first 16 bytes of data. if equal write the data at offset to the second 16 bytes of data and return true otherwise return false.
     * this operation is atomic. data must be exactly 32 bytes long. CPU Must support CMPXCHG16B (CPU's that can run windows 10 support this).
     */
    boolean compareAndSet(long offset, byte[] data) throws SharedMemoryException;

    /*
     * sets the value to update if it ever becomes expect before aTimeout elapses. returns true if the value was write false if the timeout expired.
     * the parameter aSpinTime determines how long the thread should be put to sleep before trying again after a failed attempt.
     * a negative value for aSpinTime indicates that the thread shouldnt be put to sleep.
     * a negative value for aTimeout indicates the method should not timeout and thus never return false.
     */
    boolean spinAndSet(long offset, long expect, long update, long aSpinTime, long aTimeout, TimeUnit aUnit) throws SharedMemoryException;

    /*
     * sets the value to update if it ever becomes expect before aTimeout elapses. returns true if the value was write false if the timeout expired.
     * the parameter aSpinTime determines how long the thread should be put to sleep before trying again after a failed attempt.
     * a negative value for aSpinTime indicates that the thread shouldnt be put to sleep.
     * a negative value for aTimeout indicates the method should not timeout and thus never return false.
     */
    boolean spinAndSet(long offset, int expect, int update, long aSpinTime, long aTimeout, TimeUnit aUnit) throws SharedMemoryException;

    /*
     * sets the value to update if it ever becomes expect before aTimeout elapses. returns true if the value was write false if the timeout expired.
     * the parameter aSpinTime determines how long the thread should be put to sleep before trying again after a failed attempt.
     * a negative value for aSpinTime indicates that the thread shouldnt be put to sleep.
     * a negative value for aTimeout indicates the method should not timeout and thus never return false.
     */
    boolean spinAndSet(long offset, short expect, short update, long aSpinTime, long aTimeout, TimeUnit aUnit) throws SharedMemoryException;

    /*
     * sets the value to update if it ever becomes expect before aTimeout elapses. returns true if the value was write false if the timeout expired.
     * the parameter aSpinTime determines how long the thread should be put to sleep before trying again after a failed attempt.
     * a negative value for aSpinTime indicates that the thread shouldnt be put to sleep.
     * a negative value for aTimeout indicates the method should not timeout and thus never return false.
     */
    boolean spinAndSet(long offset, byte expect, byte update, long aSpinTime, long aTimeout, TimeUnit aUnit) throws SharedMemoryException;

    /**
     * returns true if this SharedMemory supports interrupts.
     * Generally: <br>
     * ivshmem-plain does not support any interrupts whatsoever.
     * ivshmem-doorbell only suports interrupts if you specify at least one vector on the ivshmem-server.
     * <br>
     * if this method returns false then any other Interrupt related methods will throw UnsupportedOperation exception.
     */
    boolean supportsInterrupts();

    /**
     * sends a interrupt to the given peer on the given vector.
     */
    void sendInterrupt(int aPeer, int aVector) throws SharedMemoryException;

    /**
     * registers an InterruptServiceRouting that is invoked when an interrupt on the given vector arrives.
     */
    void registerInterruptServiceRoutine(int aVector, InterruptServiceRoutine isr);

    /**
     * removes an InterruptServiceRouting that is invoked when an interrupt on the given vector arrives.
     */
    void removeInterruptServiceRoutine(int aVector, InterruptServiceRoutine isr);

    /**
     * call this method to actively poll for interrupts.
     * This method has to be called in order for InterruptServiceRoutines to be called.
     * If this method is not called then the SharedMemory will not receive interrupts.
     * This method does not return without external interference.
     * It will throw an SharedMemoryException when it encounters IO Error in which case polling will stop but may be started again if you wish to continue receiving interrupts.
     * If you want to make this method return then either interrupt the thread calling the method then an InterruptedException will be thrown or
     * close the device in which case this method will simply just return.
     * This method will internally block and not waste CPU time.
     * There may be an up to 1 second delay until this method throws an InterruptedException
     */
    void receiveInterrupts() throws InterruptedException, SharedMemoryException;

    /*
     * Returns true if this SharedMemory supports clients connecting/disconnecting and also has knowledge of this occuring.
     */
    boolean supportsConnectionListening();

    /**
     * call this method to actively poll for new peers connecting.
     * This method has to be called in order for PeerConnectionListener to be called.
     * If this method is not called then the SharedMemory will not receive connection notifications and will not be able to interact with any new peers that connect after this device was created.
     * If you interact with a peer that is already disconnected then instead of an SharedMemoryException with a no such peer error message you may receive a general io error instead when trying to send an interrupt to a disconnected peer.
     * This method does not return without external interference.
     * It will throw an SharedMemoryException when it encounters IO Error in which case polling will stop but may be started again if you wish to continue receiving connection notifications.
     * If you want to make this method return then either interrupt the thread calling the method then an InterruptedException will be thrown or
     * close the device in which case this method will simply just return.
     * This method will internally block and not waste CPU time.
     * There may be an up to 1 second delay until this method throws an InterruptedException
     */
    void listenForNewConnections() throws InterruptedException, SharedMemoryException;

    /*
     * Registers a peer connection listener.
     */
    void registerPeerConnectionListener(PeerConnectionListener listener);

    /*
     * Removes a peer connection listener.
     */
    void removePeerConnectionListener(PeerConnectionListener listener);

    /**
     * Starts all necessary threads that this Shared Memory needs in order to update all the listeners
     * using the given Executor.
     */
    void startNecessaryThreads(Executor executor);
}
