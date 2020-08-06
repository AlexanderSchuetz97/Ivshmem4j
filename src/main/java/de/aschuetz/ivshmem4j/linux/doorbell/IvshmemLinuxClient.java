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

package de.aschuetz.ivshmem4j.linux.doorbell;

import de.aschuetz.ivshmem4j.api.PeerConnectionListener;
import de.aschuetz.ivshmem4j.api.SharedMemory;
import de.aschuetz.ivshmem4j.api.SharedMemoryException;
import de.aschuetz.ivshmem4j.common.AbstractSharedMemoryWithInterrupts;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static de.aschuetz.ivshmem4j.common.ErrorCodeUtil.*;

/*
 * ivshmem-doorbell implementation of a host side ivshmem client.
 */
public class IvshmemLinuxClient extends AbstractSharedMemoryWithInterrupts {

    private final List<PeerConnectionListener> peerConnectionListeners = new ArrayList<>();

    private final String path;

    private IvshmemLinuxClient(long nativeHandle, int peerID, int vectors, long size, String path) {
        super(size, peerID, vectors);
        this.nativePointer = nativeHandle;
        this.path = path;
    }

    public String getServerSocketPath() {
        return path;
    }

    /**
     * Create a new {@link IvshmemLinuxClient} that will connect to a AF_UNIX Socket (Unix Domain Socket)
     * character device at the given path expecting to find a IVSHMEM-SERVER there.
     */
    public static SharedMemory connect(String aPath) throws SharedMemoryException {
        long[] tempResult = new long[4];
        long tempCode = LinuxSharedMemory.openDevice(aPath, tempResult);
        checkCodeOK(tempCode);

        return new IvshmemLinuxClient(tempResult[0], (int) tempResult[1], (int) tempResult[2], tempResult[3], aPath);
    }


    @Override
    public boolean knowsOtherPeers() {
        return true;
    }

    public int[] getPeers() throws SharedMemoryException {
        readLock.lock();
        try {
            long[] tempCode = new long[1];
            int[] tempRes = LinuxSharedMemory.getPeers(nativePointer, tempCode);
            checkCodeOK(tempCode[0]);
            return tempRes;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean knowsOtherPeerVectors() {
        return true;
    }

    public int getVectors(int peer) throws SharedMemoryException {
        readLock.lock();
        try {
            int[] tempVectors = new int[1];
            long tempResult = LinuxSharedMemory.getVectors(nativePointer, peer, tempVectors);
            checkCodeOK(tempResult);
            return tempVectors[0];
        } finally {
            readLock.unlock();
        }
    }


    @Override
    public void sendInterrupt(int aPeer, int aVector) throws SharedMemoryException {
        checkInterruptSupport();
        readLock.lock();
        try {
            checkCodeOK(LinuxSharedMemory.sendInterrupt(nativePointer, aPeer, aVector));
        } finally {
            readLock.unlock();
        }
    }

    @Override
    protected boolean pollInterrupt0(int[] buffer) throws SharedMemoryException {
        readLock.lock();
        try {
            return checkCodePollInterrupt(LinuxSharedMemory.pollInterrupt(nativePointer, buffer));
        } finally {
            readLock.unlock();
        }
    }

    @Override
    protected void close0() {
        if (nativePointer == 0) {
            return;
        }

        LinuxSharedMemory.close(nativePointer);
    }

    @Override
    public boolean supportsConnectionListening() {
        return true;
    }

    private AtomicBoolean isPollingServer = new AtomicBoolean(false);

    @Override
    public void listenForNewConnections() throws InterruptedException, SharedMemoryException {

        if (isClosed()) {
            throw new IllegalStateException("Shared memory is beeing closed!");
        }

        if (!isPollingServer.compareAndSet(false, true)) {
            throw new IllegalStateException("More than one thread cannot be polling for new connections at the same time.");
        }

        readLock.lock();
        try {
            int[] tempResult = new int[2];
            while (!closeStarted.get()) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                if (checkCodePollServer(LinuxSharedMemory.pollServer(nativePointer, tempResult))) {
                    continue;
                }

                if (tempResult[1] == -1) {
                    synchronized (this.peerConnectionListeners) {
                        for (PeerConnectionListener listener : this.peerConnectionListeners) {
                            listener.onDisconnect(tempResult[0]);
                        }
                    }
                } else {
                    synchronized (this.peerConnectionListeners) {
                        for (PeerConnectionListener listener : this.peerConnectionListeners) {
                            listener.onConnect(tempResult[0], tempResult[1]);
                        }
                    }
                }
            }
        } finally {
            isPollingServer.set(false);
            readLock.unlock();
        }
    }

    @Override
    public void registerPeerConnectionListener(PeerConnectionListener listener) {
        synchronized (this.peerConnectionListeners) {
            this.peerConnectionListeners.add(listener);
        }
    }

    @Override
    public void removePeerConnectionListener(PeerConnectionListener listener) {
        synchronized (this.peerConnectionListeners) {
            this.peerConnectionListeners.remove(listener);
        }
    }

    @Override
    public String toString() {
        return "IvshmemLinuxClient{" +
                "path=" + path +
                ", vectors=" + vectors +
                ", peerID=" + peerID +
                ", size=" + size +
                '}';
    }
}
