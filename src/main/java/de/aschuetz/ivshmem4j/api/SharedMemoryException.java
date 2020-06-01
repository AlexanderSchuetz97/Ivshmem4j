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

import de.aschuetz.ivshmem4j.common.ErrorCodeEnum;

import java.io.IOException;

/**
 * This exception is thrown when an error interfacing with shared memory is encountered.
 * This can be anything from not being able to open the shared memory file or supplying illegal arguments
 * to certain methods. It is generally accompanied by a ErrorCodeEnum that describes what went wrong.
 */
public class SharedMemoryException extends IOException {
    /*
     * ErrorCode that describes the problem.
     */
    protected final ErrorCode code;
    /**
     * Sometimes the operating system supplies an additional code which may also be present.
     * On Linux this is the POISX error code after making a syscall.
     * On Windows this would be the value of GetLastError() after making a syscall.
     * This value is 0 if its not present.
     */
    protected final int operatingSystemErrorCode;

    public SharedMemoryException(String aMessage) {
        this(aMessage, ErrorCodeEnum.ERROR, 0);
    }

    public SharedMemoryException(String aMessage, ErrorCode code, int operatingSystemErrorCode) {
        super(aMessage);
        this.code = code;
        this.operatingSystemErrorCode = operatingSystemErrorCode;
    }

    public SharedMemoryException(ErrorCodeEnum code, int operatingSystemErrorCode) {
        this(code == null ? ErrorCodeEnum.ERROR.getHumanReadableErrorMessage() : code.getHumanReadableErrorMessage(), code, operatingSystemErrorCode);
    }

    public SharedMemoryException(ErrorCodeEnum code) {
        this(code, 0);
    }

    public ErrorCode getCode() {
        return code;
    }

    /**
     * If present returns the integer that was returned by "errno" on linux or "GetLastError" on Windows.
     */
    public int getOperatingSystemErrorCode() {
        return operatingSystemErrorCode;
    }
}
