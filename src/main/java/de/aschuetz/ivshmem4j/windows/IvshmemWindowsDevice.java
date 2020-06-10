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

import de.aschuetz.ivshmem4j.api.SharedMemory;
import de.aschuetz.ivshmem4j.api.SharedMemoryException;

import java.util.ArrayList;
import java.util.Collection;

import static de.aschuetz.ivshmem4j.common.ErrorCodeUtil.checkCodeOK;

/**
 * You may connect multiple ivshmem devices to your windows vm.
 * This class can be used to identify the individual pci devices by enumerating them, getting its name (virtual pci bus-id),
 * aswell as its size. Once you have found the device you wish to open (or perhaps multiple devices)
 * then you may call the open method to createOrOpen a SharedMemory with that device.
 */
public class IvshmemWindowsDevice {

    private final byte[] name;

    private final long size;

    private IvshmemWindowsDevice(byte[] aName, long aSize) {
        this.name = aName;
        this.size = aSize;
    }

    /**
     * Gets a collection of all currently installed IVSHMEM windows hardware devices.
     */
    public static Collection<IvshmemWindowsDevice> getSharedMemoryDevices() throws SharedMemoryException {
        Object[] tempResult = new Object[1];
        checkCodeOK(WindowsSharedMemory.getDevices(tempResult));

        Object tempRawObj = tempResult[0];
        if (!(tempRawObj instanceof Object[])) {
            throw new SharedMemoryException("Native code was unable to allocate object array!");
        }

        Object[] tempRaw = (Object[]) tempRawObj;

        if (tempRaw.length % 2 != 0) {
            throw new SharedMemoryException("Native code allocated an invalid object array!");
        }

        ArrayList<IvshmemWindowsDevice> tempList = new ArrayList<IvshmemWindowsDevice>();

        for (int i = 0; i < tempRaw.length; i += 2) {

            Object tempObj = tempRaw[i];
            if (!(tempObj instanceof long[])) {
                throw new SharedMemoryException("Received invalid Shared Memory size index from windows kernel device enumeration: " + i);
            }

            long[] tempSize = (long[]) tempObj;
            if (tempSize.length != 1 && tempSize[0] <= 0) {
                throw new SharedMemoryException("Received invalid Shared Memory size index from windows kernel device enumeration: " + i);
            }

            tempObj = tempRaw[i + 1];

            if (!(tempObj instanceof byte[])) {
                throw new SharedMemoryException("Received invalid Shared Memory device name index from windows kernel device enumeration: " + i);
            }

            IvshmemWindowsDevice tempDevice = new IvshmemWindowsDevice((byte[]) tempObj, tempSize[0]);
            tempList.add(tempDevice);
        }


        return tempList;
    }

    /**
     * opens the device by mapping the memory. Note that a Device can only be mapped a single time by a single process.
     * There is no way to tell if a device is already mapped by another process/open call other than to try and catch the error.
     * This is safe to do and will not cause any issued if it fails.
     * If it fails then a retry may be attempted if it is assumed that whatever keept the device busy is now gone
     * otherwise a retry will fail again.
     */
    public SharedMemory open() throws SharedMemoryException {
        long[] tempResult = new long[3];
        checkCodeOK(WindowsSharedMemory.openDevice(name, tempResult));
        return new IvshmemMappedWindowsDevice(this, tempResult[0], (int) tempResult[1], (int) tempResult[2]);
    }

    /*
     * gets the name as a byte array. this is filled by the windows kernel. it is usually a UTF String that is terminated by a 0 byte.
     * This method only returns a copy of this byte array.
     */
    public byte[] getName() {
        if (name == null) {
            return null;
        }

        byte[] tempArray = new byte[name.length];

        System.arraycopy(name, 0, tempArray, 0, name.length);

        return tempArray;
    }

    /*
     * this method tries to interpret the byte array of getName() as zero terminated string.
     * if that fails it just interprets it as a string probably returning a garbage string.
     * This String will contain the PCI Bus ID which is useful to identify the device.
     */
    public String getNameAsString() {
        if (name == null) {
            return null;
        }

        int tempZeroByteIndex = 0;
        for (int i = 0; i < name.length; i++) {
            if (name[i] == 0x0) {
                tempZeroByteIndex = i;
                break;
            }
        }

        if (tempZeroByteIndex == 0) {
            return new String(name);
        }

        return new String(name, 0, tempZeroByteIndex);
    }

    /**
     * Returns true if the name is a zero terminated String (as it should be).
     */
    public boolean isNameValid() {
        if (name == null) {
            return false;
        }

        for (int i = 0; i < name.length; i++) {
            if (name[i] == 0x0) {
                return true;
            }
        }

        return false;
    }

    /*
     * gets the shared memory size as that is the only information available before mapping the device.
     */
    public long getSharedMemorySize() {
        return size;
    }

    @Override
    public String toString() {
        return "IvshmemWindowsDevice{size=" + size + ", name=" + getNameAsString() + "}";
    }
}
