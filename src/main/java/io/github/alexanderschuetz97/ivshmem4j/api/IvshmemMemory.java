//
// Copyright Alexander Schütz, 2020-2022
//
// This file is part of Ivshmem4j.
//
// Ivshmem4j is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// Ivshmem4j is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// A copy of the GNU Lesser General Public License should be provided
// in the COPYING & COPYING.LESSER files in top level directory of Ivshmem4j.
// If not, see <https://www.gnu.org/licenses/>.
//
package io.github.alexanderschuetz97.ivshmem4j.api;

import io.github.alexanderschuetz97.nativeutils.api.NativeMemory;

import java.util.Collection;

/**
 * Interface for Ivshmem Memory. It contains the actual native memory mapping and several ivshmem specific functions.
 */
public interface IvshmemMemory extends AutoCloseable {

    /**
     * returns the Native memory mapping for this SharedMemory
     */
    NativeMemory getMemory();

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
    boolean isOtherPeerConnected(int aPeerId);

    /**
     * returns the other peers that are also accessing the SharedMemory.
     */
    Collection<Integer> getPeers();

    /**
     * returns true if this SharedMemory knows the vectors of other peers.
     * Generally it can be assumed that the vectors of other peers mirror the vectors of this peer.
     * if this method returns false then getVectors will throw an UnsupportedOperationException.
     */
    boolean knowsOtherPeerVectors();

    /**
     * returns the vectors of the peer.
     */
    int getVectors(int peer);

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
    @Override
    void close();

    /**
     * returns true if the shared memory is closed and any further interaction with it will cause an exception.
     */
    boolean isClosed();

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
    void sendInterrupt(int aPeer, int aVector);

    /**
     * registers an InterruptServiceRouting that is invoked when an interrupt on the given vector arrives.
     */
    void registerInterruptServiceRoutine(int aVector, InterruptServiceRoutine isr);

    /**
     * removes an InterruptServiceRouting that is invoked when an interrupt on the given vector arrives.
     */
    void removeInterruptServiceRoutine(int aVector, InterruptServiceRoutine isr);


    /*
     * Registers a peer connection listener.
     */
    void registerPeerConnectionListener(PeerConnectionListener listener);

    /*
     * Removes a peer connection listener.
     */
    void removePeerConnectionListener(PeerConnectionListener listener);

}
