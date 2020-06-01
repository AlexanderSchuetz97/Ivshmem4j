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

class CommonSharedMemory {

    static long EXPECTED_NATIVE_LIB_VERSION = 0;

    static native long getNativeLibVersion();

    /*
     * Read/Write
     */

    static native long write(long device, long offset, byte[] buffer, int bufferOffset, int len);

    static native long write(long device, long offset, byte aByte);

    static native long write(long device, long offset, int aInt);

    static native long write(long device, long offset, long aLong);

    static native long write(long device, long offset, float aFloat);

    static native long write(long device, long offset, double aDouble);

    static native long write(long device, long offset, short aShort);

    static native long read(long device, long offset, byte[] buffer, int bufferOffset, int len);

    static native long read(long device, long offset, int[] aInt);

    static native long read(long device, long offset, long[] aLong);

    static native long read(long device, long offset, float[] aFloat);

    static native long read(long device, long offset, double[] aDouble);

    static native long read(long device, long offset, short[] aShort);

    static native long read(long device, long offset, byte[] aByte);

    static native long memset(long device, long offset, byte value, long len);

    /*
     * Atomics
     */

    static native long xadd(long device, long offset, long aLong[]);

    static native long xadd(long device, long offset, int aInt[]);

    static native long xadd(long device, long offset, short aShort[]);

    static native long xadd(long device, long offset, byte aByte[]);

    static native long xchg(long device, long offset, long aLong[]);

    static native long xchg(long device, long offset, int aInt[]);

    static native long xchg(long device, long offset, short aShort[]);

    static native long xchg(long device, long offset, byte aByte[]);

    static native long cmpxchg(long device, long offset, long expect, long update);

    static native long cmpxchg(long device, long offset, int expect, int update);

    static native long cmpxchg(long device, long offset, short expect, short update);

    static native long cmpxchg(long device, long offset, byte expect, byte update);

    /*
     * data has to be 32 bytes long and first 16 bytes is expect next 16 bytes is update.
     */
    static native long cmpxchg16b(long device, long offset, byte[] data);
}
