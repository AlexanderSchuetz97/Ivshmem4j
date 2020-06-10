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

package de.aschuetz.ivshmem4j.windows;

/**
 * JNI Internal.
 */
class WindowsSharedMemory {

    /**
     * Enumerates the devices.
     * result "array" must be 1 in size.
     * first element will be filled with a Object Array that contains n*2 elements.
     * n specifies the amount of connected devices.
     * element n contains long[] size 1 which contains the size of the device.
     * element n+1 contains byte[] which contains the device name (usually zero byte terminated string).
     */
    static native long getDevices(Object[] result);

    static native long openDevice(byte[] device, long[] handle);

    static native long close(long device);

    static native long sendInterrupt(long device, int vector, int peer);

    static native long pollInterrupt(long device, int[] someVectors);
}
