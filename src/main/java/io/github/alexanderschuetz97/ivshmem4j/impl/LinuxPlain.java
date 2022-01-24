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

package io.github.alexanderschuetz97.ivshmem4j.impl;

import io.github.alexanderschuetz97.ivshmem4j.api.InterruptServiceRoutine;
import io.github.alexanderschuetz97.ivshmem4j.api.IvshmemException;
import io.github.alexanderschuetz97.ivshmem4j.api.IvshmemMemory;
import io.github.alexanderschuetz97.ivshmem4j.api.PeerConnectionListener;
import io.github.alexanderschuetz97.nativeutils.api.LinuxConst;
import io.github.alexanderschuetz97.nativeutils.api.LinuxNativeUtil;
import io.github.alexanderschuetz97.nativeutils.api.NativeMemory;
import io.github.alexanderschuetz97.nativeutils.api.NativeUtils;
import io.github.alexanderschuetz97.nativeutils.api.exceptions.InvalidFileDescriptorException;
import io.github.alexanderschuetz97.nativeutils.api.exceptions.QuotaExceededException;
import io.github.alexanderschuetz97.nativeutils.api.exceptions.UnknownNativeErrorException;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static io.github.alexanderschuetz97.nativeutils.api.LinuxConst.O_CREAT;
import static io.github.alexanderschuetz97.nativeutils.api.LinuxConst.O_RDWR;
import static io.github.alexanderschuetz97.nativeutils.api.LinuxConst.S_IRWXG;
import static io.github.alexanderschuetz97.nativeutils.api.LinuxConst.S_IRWXO;
import static io.github.alexanderschuetz97.nativeutils.api.LinuxConst.S_IRWXU;

public class LinuxPlain implements IvshmemMemory {

    private final LinuxNativeUtil nativeUtil = NativeUtils.getLinuxUtil();
    private int memFD;
    private long memPtr;
    private long memSize;
    private NativeMemory memory;
    private ReentrantReadWriteLock.WriteLock writeLock;
    private final Thread.UncaughtExceptionHandler handler;

    public LinuxPlain(String path, long size, Thread.UncaughtExceptionHandler handler) {
        Objects.requireNonNull(path);
        this.handler = Objects.requireNonNull(handler);
        if (size < 0) {
            throw new IllegalArgumentException("size");
        }


        boolean succ = false;
        try {
            int flag = O_RDWR;

            boolean seek = false;
            File pFile = new File(path);
            if (!pFile.exists()) {
                if (size == 0) {
                    throw new IvshmemException(path + " does not exist");
                }

                flag |= O_CREAT;
                seek = true;
            } else {
                long l = pFile.length();
                if (l == 0) {
                    if (size == 0) {
                        throw new IvshmemException(path + " has a size of 0");
                    }

                    seek = true;
                } else {
                    if (l < size) {
                        seek = true;
                    } else {
                        size = l;
                    }
                }
            }


            try {
                memFD = nativeUtil.open(path, flag, S_IRWXO | S_IRWXG | S_IRWXU);
            } catch (IOException e) {
                throw new IvshmemException(e);
            } catch (UnknownNativeErrorException e) {
                throw new IvshmemException(nativeUtil.strerror_r((int) e.getCode()));
            }

            if (seek) {
                try {
                    nativeUtil.lseek(memFD, size - 1, LinuxNativeUtil.lseek_whence.SEEK_SET);
                    nativeUtil.write(memFD, new byte[1], 0, 1);
                } catch (IOException e) {
                    throw new IvshmemException(e);
                } catch (UnknownNativeErrorException e) {
                    throw new IvshmemException(nativeUtil.strerror_r((int) e.getCode()));
                }
            }

            memSize = size;
            try {
                memPtr = nativeUtil.mmap(memFD, size, LinuxConst.MAP_SHARED, true, true, 0);
            } catch (QuotaExceededException | AccessDeniedException | InvalidFileDescriptorException e) {
                throw new IvshmemException(e);
            }

            memory = nativeUtil.pointer(memPtr, size, new IvshmemPointerHandler(this));
            writeLock = memory.writeLock();
            succ = true;
        } finally {
            if (!succ) {
                closeInternal();
            }
        }
    }

    @Override
    public NativeMemory getMemory() {
        return Objects.requireNonNull(memory);
    }

    @Override
    public boolean hasOwnPeerID() {
        return false;
    }

    @Override
    public int getOwnPeerID() {
        throw new IvshmemException("own peer id is not known");
    }

    @Override
    public boolean knowsOtherPeers() {
        return false;
    }

    @Override
    public boolean isOtherPeerConnected(int aPeerId) {
        throw new IvshmemException("other peers are not known");
    }

    @Override
    public Collection<Integer> getPeers() {
        throw new IvshmemException("other peers are not known");
    }

    @Override
    public boolean knowsOtherPeerVectors() {
        return false;
    }

    @Override
    public int getVectors(int peer) {
        throw new IvshmemException("other peers are not known");
    }

    @Override
    public int getOwnVectors() {
        return 0;
    }

    @Override
    public boolean isVectorValid(int vector) {
        return false;
    }

    private volatile boolean closed = false;

    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        NativeMemory mem = memory;
        if (mem != null) {
            mem.close();
        }

        writeLock.lock();
        try {
            closeInternal();
        } finally {
            writeLock.unlock();
        }
    }

    protected void closeInternal() {
        closed = true;
        if (memory != null && memory.isValid()) {
            memory.close();
            memory = null;
        }

        if (memPtr != 0) {
            long pp = memPtr;
            memPtr = 0;
            try {
                nativeUtil.munmap(pp, memSize);
            } catch (UnknownNativeErrorException e) {
                handleUncaught(e);
            }
        }

        if (memFD != -1) {
            try {
                nativeUtil.close(memFD);
            } catch (IOException e) {
                handleUncaught(e);
            }
            memFD = -1;
        }
    }

    protected void handleUncaught(Throwable t) {
        handler.uncaughtException(Thread.currentThread(), t);
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public boolean supportsInterrupts() {
        return false;
    }

    @Override
    public void sendInterrupt(int aPeer, int aVector) {
        throw new IvshmemException("interrupts not supported");
    }

    @Override
    public void registerInterruptServiceRoutine(int aVector, InterruptServiceRoutine isr) {
        //NOOP
    }

    @Override
    public void removeInterruptServiceRoutine(int aVector, InterruptServiceRoutine isr) {
        //NOOP
    }

    @Override
    public void registerPeerConnectionListener(PeerConnectionListener listener) {
        //NOOP
    }

    @Override
    public void removePeerConnectionListener(PeerConnectionListener listener) {
        //NOOP
    }
}
