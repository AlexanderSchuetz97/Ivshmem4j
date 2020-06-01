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

package de.aschuetz.ivshmem4j.linux.plain;

import de.aschuetz.ivshmem4j.api.SharedMemory;
import de.aschuetz.ivshmem4j.api.SharedMemoryException;
import de.aschuetz.ivshmem4j.common.AbstractSharedMemory;

public class LinuxMappedFileSharedMemory extends AbstractSharedMemory {

    private final String path;

    private LinuxMappedFileSharedMemory(String aPath, long aHandle, long aSize) throws SharedMemoryException {
        super(aSize);
        path = aPath;
        nativePointer = aHandle;
    }

    public static SharedMemory create(String aPath, long size) throws SharedMemoryException {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be bigger than 0");
        }

        if (aPath == null) {
            throw new IllegalArgumentException("Path must not be null");
        }

        long[] tempResult = new long[2];
        LinuxErrorCodeUtil.checkCodeOK(LinuxSharedMemory.createOrOpenFile(aPath, size, tempResult));
        return new LinuxMappedFileSharedMemory(aPath, tempResult[0], tempResult[1]);
    }

    @Override
    protected void close0() {
        LinuxSharedMemory.close(nativePointer);
    }


    @Override
    public String toString() {
        return "LinuxMappedFileSharedMemory{" +
                "path= '" + path + '\'' +
                ", size= " + size +
                '}';
    }
}
