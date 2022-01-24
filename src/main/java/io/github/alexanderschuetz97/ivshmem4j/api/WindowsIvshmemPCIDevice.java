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

/**
 * Transfer object that contains name+size of a windows ivshmem pci device.
 */
public class WindowsIvshmemPCIDevice {

    private final String name;
    private final long size;


    public WindowsIvshmemPCIDevice(String name, long size) {
        this.name = name;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WindowsIvshmemPCIDevice that = (WindowsIvshmemPCIDevice) o;

        if (size != that.size) return false;
        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (int) (size ^ (size >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "WindowsIvshmemPCIDevice{" +
                "name='" + name + '\'' +
                ", size=" + size +
                '}';
    }
}
