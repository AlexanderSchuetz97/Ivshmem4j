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

public interface PeerConnectionListener {

    /*
     * called for a new connecting peer. Due to poor protocol design by QEMU the amount of connectedVectors is not known initially.
     * This method is thus called repeatedly every time the amount of connected vectors increases. It is guaranteed by the protocol that this happens before the onDisconnect method is called.
     * Additionally it is also guaranteed that if the peer parameter changes then the previous "peer" has all vectors connected.
     * However it is impossible to tell if the current "onConnect" call is the last vector for the currently connecting peer.
     * As soon as this method is called with a given vector then that vector may be used immediately on the given peer.
     * It will also be returned by the getVectors command for the given peer.
     * The connected vectors value will be -1 if this value is unknown or not supported by this shared memory.
     */
    void onConnect(int peerID, int connectedVectors);

    /**
     * This method is called when a peer disconnects and all his vectors are no longer available for interrupts.
     */
    void onDisconnect(int peerID);
}
