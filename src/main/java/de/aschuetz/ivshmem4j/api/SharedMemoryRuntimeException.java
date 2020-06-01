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

package de.aschuetz.ivshmem4j.api;

/**
 * This exception is a RuntimeException wrapper for SharedMemoryException or otherwise illegal states.
 * It is thrown in situation when it is impossible to throw checked exceptions. (i.e. in methods that
 * implement interfaces from java which do not allow for custom checked exceptions)
 */
public class SharedMemoryRuntimeException extends RuntimeException {

    public SharedMemoryRuntimeException(SharedMemoryException e) {
        super(e);
    }

    public SharedMemoryRuntimeException(String aStr) {
        super(aStr);
    }
}
