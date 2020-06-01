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

class LinuxSharedMemory {

    /*
     * Management
     */

    static native long openDevice(String aPath, long[] result);

    static native int[] getPeers(long device, long[] result);

    static native long getVectors(long device, int peer, int[] vectors);

    static native long close(long device);

    static native long pollServer(long device, int[] result);

    /*
     * Interrupt
     */

    static native long sendInterrupt(long device, int peer, int vector);

    static native long pollInterrupt(long device, int[] interrupts);

}
