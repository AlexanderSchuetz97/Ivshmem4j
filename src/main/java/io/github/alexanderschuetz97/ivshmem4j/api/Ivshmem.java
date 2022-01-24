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

import io.github.alexanderschuetz97.ivshmem4j.impl.DaemonThreadFactory;
import io.github.alexanderschuetz97.ivshmem4j.impl.LinuxDoorbellClient;
import io.github.alexanderschuetz97.ivshmem4j.impl.LinuxPlain;
import io.github.alexanderschuetz97.ivshmem4j.impl.StdErrHandler;
import io.github.alexanderschuetz97.ivshmem4j.impl.WindowsPCI;
import io.github.alexanderschuetz97.nativeutils.api.NativeUtils;

import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Ivshmem {

    private static Executor EXECUTOR;

    /**
     * Lazy load a threadpool on demand if user does not specify one.
     */
    private synchronized static Executor getDefaultExecutor() {
        if (EXECUTOR == null) {
            EXECUTOR = Executors.newCachedThreadPool(new DaemonThreadFactory());
        }

        return EXECUTOR;
    }

    public static IvshmemMemory doorbell(String path, long grace, Executor executor, Thread.UncaughtExceptionHandler handler) throws IvshmemException {
        if (!NativeUtils.isLinux()) {
            throw new UnsupportedOperationException();
        }
        return new LinuxDoorbellClient(path, grace, executor == null ? getDefaultExecutor() : executor, handler);
    }

    public static IvshmemMemory doorbell(String path) throws IvshmemException {
        return doorbell(path, 5000, getDefaultExecutor(), StdErrHandler.INSTANCE);
    }

    public static IvshmemMemory doorbell(String path, long grace) throws IvshmemException {
        return doorbell(path, grace, getDefaultExecutor(), StdErrHandler.INSTANCE);
    }

    public static IvshmemMemory doorbell(String path, long grace, Executor executor) throws IvshmemException {
        return doorbell(path, grace, executor, StdErrHandler.INSTANCE);
    }

    public static IvshmemMemory doorbell(String path, Thread.UncaughtExceptionHandler handler) throws IvshmemException {
        return doorbell(path, 5000, getDefaultExecutor(), handler);
    }

    public static IvshmemMemory doorbell(String path, Executor executor, Thread.UncaughtExceptionHandler handler) throws IvshmemException {
        return doorbell(path, 5000, executor, handler);
    }

    public static IvshmemMemory plain(String path, long size, Thread.UncaughtExceptionHandler handler) throws IvshmemException {
        if (!NativeUtils.isLinux()) {
            throw new UnsupportedOperationException();
        }
        return new LinuxPlain(path, size, handler);
    }

    public static IvshmemMemory plain(String path, long size) throws IvshmemException {
        return plain(path, size, StdErrHandler.INSTANCE);
    }

    public static IvshmemMemory plain(String path) throws IvshmemException {
        return plain(path, 0, StdErrHandler.INSTANCE);
    }

    public static Collection<WindowsIvshmemPCIDevice> windowsListPCI() throws IvshmemException {
        return WindowsPCI.list();
    }

    public static IvshmemMemory windowsPCI(WindowsIvshmemPCIDevice pciDevice, Executor executor, Thread.UncaughtExceptionHandler handler) {
        return new WindowsPCI(pciDevice, executor, handler);
    }

    public static IvshmemMemory windowsPCI(WindowsIvshmemPCIDevice pciDevice, Executor executor) {
        return new WindowsPCI(pciDevice, executor, StdErrHandler.INSTANCE);
    }

    public static IvshmemMemory windowsPCI(WindowsIvshmemPCIDevice pciDevice, Thread.UncaughtExceptionHandler handler) {
        return new WindowsPCI(pciDevice, getDefaultExecutor(), handler);
    }

    public static IvshmemMemory windowsPCI(WindowsIvshmemPCIDevice pciDevice) {
        return new WindowsPCI(pciDevice, getDefaultExecutor(), StdErrHandler.INSTANCE);
    }

}
