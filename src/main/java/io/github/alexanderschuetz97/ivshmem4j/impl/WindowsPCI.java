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
import io.github.alexanderschuetz97.ivshmem4j.api.WindowsIvshmemPCIDevice;
import io.github.alexanderschuetz97.nativeutils.api.NativeMemory;
import io.github.alexanderschuetz97.nativeutils.api.NativeUtils;
import io.github.alexanderschuetz97.nativeutils.api.WinConst;
import io.github.alexanderschuetz97.nativeutils.api.WindowsNativeUtil;
import io.github.alexanderschuetz97.nativeutils.api.exceptions.InvalidFileDescriptorException;
import io.github.alexanderschuetz97.nativeutils.api.exceptions.MutexAbandonedException;
import io.github.alexanderschuetz97.nativeutils.api.exceptions.SharingViolationException;
import io.github.alexanderschuetz97.nativeutils.api.exceptions.UnknownNativeErrorException;
import io.github.alexanderschuetz97.nativeutils.api.structs.GUID;

import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class WindowsPCI implements IvshmemMemory {

    private static final GUID DEVICE_GUID = new GUID("DF576976-569D-4672-95A0-F57E4EA0B210");

    public static Collection<WindowsIvshmemPCIDevice> list() {
        List<WindowsIvshmemPCIDevice> devs = new ArrayList<>();
        WindowsNativeUtil wni = NativeUtils.getWindowsUtil();
        try {

            for (String path : wni.iterateDeviceInterfaces(null, null, WinConst.DIGCF_PRESENT | WinConst.DIGCF_ALLCLASSES | WinConst.DIGCF_DEVICEINTERFACE, DEVICE_GUID)) {
                long devHandle = wni.CreateFileA(path, 0, false, false, false, WindowsNativeUtil.CreateFileA_createMode.OPEN_EXISTING, 0);
                long size = 0;
                try (NativeMemory buf = wni.malloc(8)) {
                    int len = wni.DeviceIoControl(devHandle, wni.CTL_CODE(WinConst.FILE_DEVICE_UNKNOWN, 0x801, WinConst.METHOD_BUFFERED, WinConst.FILE_ANY_ACCESS), null, 0, 0, buf, 0, 8);
                    size = len == 4 ? buf.readUnsignedInt(0) : buf.readLong(0);
                } finally {
                    wni.CloseHandle(devHandle);
                }
                devs.add(new WindowsIvshmemPCIDevice(path, size));
            }

        } catch (UnknownNativeErrorException e) {
            throw new IvshmemException(wni.FormatMessageA((int) e.getCode()));
        } catch (InvalidFileDescriptorException | SharingViolationException | FileAlreadyExistsException e) {
            throw new IvshmemException(e);
        }

        return Collections.unmodifiableCollection(devs);
    }

    private final WindowsNativeUtil nativeUtil = NativeUtils.getWindowsUtil();
    private long handle = -1;
    private int ownPeerID;
    private long[] ownVectors;
    private final Collection<InterruptServiceRoutine>[] isrs;
    private NativeMemory memory;
    private final ReentrantReadWriteLock.WriteLock writeLock;
    private final ReentrantReadWriteLock.ReadLock readLock;
    private final Thread.UncaughtExceptionHandler handler;

    private volatile boolean closed = false;


    public WindowsPCI(WindowsIvshmemPCIDevice device, Executor executor, Thread.UncaughtExceptionHandler handler) {
        Objects.requireNonNull(device);
        Objects.requireNonNull(device.getName());
        this.handler = Objects.requireNonNull(handler);
        String path = device.getName();
        boolean succ = false;
        try {

            handle = nativeUtil.CreateFileA(path, 0, false, false, false, WindowsNativeUtil.CreateFileA_createMode.OPEN_EXISTING, 0);

            int hSize = nativeUtil.getPointerSize();

            long size;
            long memPtr;
            int vectors;
            if (hSize == 8) {
                //uint16_t peer
                //48 bit padding
                //uint64_t size
                //void* memory
                //uint16_t vector_count
                //48 bit padding
                try (NativeMemory out = nativeUtil.malloc(32)) {
                    try (NativeMemory in = nativeUtil.malloc(1)) {
                        out.set(0, (byte) 0, out.size());
                        in.set(0, (byte) 0, 1);
                        int len = nativeUtil.DeviceIoControl(handle, nativeUtil.CTL_CODE(WinConst.FILE_DEVICE_UNKNOWN, 0x802, WinConst.METHOD_BUFFERED, WinConst.FILE_ANY_ACCESS), in, 0, 1, out, 0, (int) out.size());
                        if (len != out.size()) {
                            throw new IvshmemException("native buffer filled with less bytes then expected got " + len + " expected " + out.size());
                        }

                        ownPeerID = out.readUnsignedShort(0);
                        size = out.readLong(8);
                        memPtr = out.readLong(16);
                        vectors = out.readUnsignedShort(24);
                    }
                }
            } else {
                //uint16_t peer
                //48 bit padding
                //uint64_t size
                //void* memory (32 bit, 4 byte)
                //uint16_t vector_count
                //16 bit padding
                try (NativeMemory out = nativeUtil.malloc(24)) {
                    try (NativeMemory in = nativeUtil.malloc(1)) {
                        out.set(0, (byte) 0, out.size());
                        in.set(0, (byte) 0, 1);
                        int len = nativeUtil.DeviceIoControl(handle, nativeUtil.CTL_CODE(WinConst.FILE_DEVICE_UNKNOWN, 0x802, WinConst.METHOD_BUFFERED, WinConst.FILE_ANY_ACCESS), in, 0, 1, out, 0, (int) out.size());
                        if (len != out.size()) {
                            throw new IvshmemException("native buffer filled with less bytes then expected got " + len + " expected " + out.size());
                        }

                        ownPeerID = out.readUnsignedShort(0);
                        size = out.readLong(8);
                        memPtr = out.readUnsignedInt(16);
                        vectors = out.readUnsignedShort(20);
                    }
                }
            }


            ownVectors = new long[vectors];
            isrs = new Collection[ownVectors.length];
            for (int i = 0; i < ownVectors.length; i++) {
                ownVectors[i] = -1;
                isrs[i] = Collections.synchronizedSet(new LinkedHashSet<InterruptServiceRoutine>());
            }


            //This struct is always aligned for 64 bit cpu even on 32 bit cpu... why?
            //uint16_t vector; (uint64_t?)
            //HANDLE event; (uint64_t?)
            //bool singleShot; (uint64_t?)
            try (NativeMemory in = nativeUtil.malloc(24)) {
                for (int i = 0; i < ownVectors.length; i++) {
                    in.set(0, (byte) 0, in.size());
                    ownVectors[i] = nativeUtil.CreateEventA(0, false, false, null);
                    in.write(0, (short) i);
                    in.write(8, ownVectors[i]);

                    nativeUtil.DeviceIoControl(handle, nativeUtil.CTL_CODE(WinConst.FILE_DEVICE_UNKNOWN, 0x805, WinConst.METHOD_BUFFERED, WinConst.FILE_ANY_ACCESS), in, 0, (int) in.size(), null, 0, 0);
                }
            }


            memory = nativeUtil.pointer(memPtr, size, new IvshmemPointerHandler(this));
            writeLock = memory.writeLock();
            readLock = memory.readLock();
            succ = true;
        } catch (SharingViolationException | FileAlreadyExistsException e) {
            throw new IvshmemException(e);
        } catch (UnknownNativeErrorException e) {
            throw new IvshmemException(nativeUtil.FormatMessageA((int) e.getCode()), e);
        } finally {
            if (!succ) {
                closeInternal();
            }
        }

        try {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    receiveInterrupts();
                }
            });

        } catch (Exception exc) {
            close();
            throw new IvshmemException(exc);
        }


    }

    protected void handleUncaught(Throwable t) {
        handler.uncaughtException(Thread.currentThread(), t);
    }

    private void checkClosed() {
        if (isClosed()) {
            throw new IllegalStateException("closed");
        }
    }

    @Override
    public NativeMemory getMemory() {
        return Objects.requireNonNull(memory);
    }

    @Override
    public boolean hasOwnPeerID() {
        return true;
    }

    @Override
    public int getOwnPeerID() {
        return ownPeerID;
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
        readLock.lock();
        try {
            checkClosed();
            return ownVectors.length;
        } finally {
            readLock.unlock();
        }

    }

    @Override
    public boolean isVectorValid(int vector) {
        readLock.lock();
        try {
            checkClosed();
            if (vector < 0 || vector >= ownVectors.length) {
                return false;
            }
            return true;
        } finally {
            readLock.unlock();
        }
    }


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
        if (memory != null) {
            memory.close();
            memory = null;
        }
        if (handle != -1) {
            try {
                nativeUtil.CloseHandle(handle);
            } catch (UnknownNativeErrorException e) {
                handleUncaught(e);
            } catch (InvalidFileDescriptorException e) {
                handleUncaught(e);
            }

            handle = -1;
        }

        if (ownVectors != null) {
            for (int i = 0; i < ownVectors.length; i++) {
                if (ownVectors[i] != -1) {
                    try {
                        nativeUtil.CloseHandle(ownVectors[i]);
                    } catch (UnknownNativeErrorException e) {
                        handleUncaught(e);
                    } catch (InvalidFileDescriptorException e) {
                        handleUncaught(e);
                    }

                    ownVectors[i] = -1;
                }

            }

            ownVectors = null;
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public boolean supportsInterrupts() {
        readLock.lock();
        try {
            checkClosed();
            return ownVectors.length > 0;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void sendInterrupt(int aPeer, int aVector) {
        readLock.lock();
        try {
            checkClosed();
            if (aPeer < 0 || aPeer > 0xffff) {
                throw new IvshmemException("invalid peer");
            }

            if (aVector < 0 || aVector >= ownVectors.length) {
                throw new IvshmemException("invalid vector");
            }

            //4 for 64 and 32 bit
            try (NativeMemory buf = nativeUtil.malloc(4)) {
                buf.write(0, (short) aPeer);
                buf.write(2, (short) aVector);
                nativeUtil.DeviceIoControl(handle, nativeUtil.CTL_CODE(WinConst.FILE_DEVICE_UNKNOWN, 0x804, WinConst.METHOD_BUFFERED, WinConst.FILE_ANY_ACCESS), buf, 0, (int) buf.size(), null, 0, 0);
            }
        } catch (UnknownNativeErrorException err) {
            throw new IvshmemException(nativeUtil.FormatMessageA((int) err.getCode()), err);
        } finally {
            readLock.unlock();
        }
    }

    protected void receiveInterrupts() {
        if (this.ownVectors.length == 0) {
            return;
        }

        int toIntIdx = 0;
        int[] toInt = new int[ownVectors.length];


        try {
            while (!closed) {
                readLock.lock();
                try {
                    if (closed) {
                        return;
                    }

                    toIntIdx = 0;

                    for (int i = 0; i < toInt.length; i++) {
                        if (nativeUtil.WaitForSingleObject(ownVectors[i], 0)) {
                            toInt[toIntIdx++] = i;
                        }
                    }


                    if (toIntIdx == 0) {
                        int idx = nativeUtil.WaitForMultipleObjects(this.ownVectors, 5000, false);
                        if (idx == -1) {
                            continue;
                        }

                        toInt[0] = idx;
                        toIntIdx++;
                    }

                } finally {
                    readLock.unlock();
                }


                for (int i = 0; i < toIntIdx; i++) {
                    Collection<InterruptServiceRoutine> isrs = this.isrs[i];
                    synchronized (isrs) {
                        for (InterruptServiceRoutine isr : isrs) {
                            try {
                                isr.onInterrupt(i);
                            } catch (Throwable e) {
                                handleUncaught(e);
                            }
                        }
                    }
                }
            }
        } catch (UnknownNativeErrorException | InvalidFileDescriptorException | MutexAbandonedException e) {
            handleUncaught(e);
        } finally {
            close();
        }


    }

    @Override
    public void registerInterruptServiceRoutine(int aVector, InterruptServiceRoutine isr) {
        if (aVector < 0 || aVector > isrs.length) {
            throw new IvshmemException("invalid vector");
        }

        isrs[aVector].add(Objects.requireNonNull(isr));
    }

    @Override
    public void removeInterruptServiceRoutine(int aVector, InterruptServiceRoutine isr) {
        if (aVector < 0 || aVector > isrs.length) {
            throw new IvshmemException("invalid vector");
        }

        isrs[aVector].remove(isr);
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
