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

import de.aschuetz.ivshmem4j.api.SharedMemoryException;

/**
 * Internal utility for native error codes.
 */
public class ErrorCodeUtil {

    /**
     * Returns true if CMPXCHG succeed false if it failed throws exception otherwise.
     */
    public static boolean checkCodeCMPXCHG(long aCode) throws SharedMemoryException {
        ErrorCodeEnum tempCode = check(aCode);
        if (tempCode == ErrorCodeEnum.OK) {
            return true;
        }

        if (tempCode == ErrorCodeEnum.CMPXCHG_FAILED) {
            return false;
        }

        throw new SharedMemoryException(tempCode, (int) aCode);
    }

    /**
     * Throws an Exception unless the result is OK.
     */
    public static void checkCodeOK(long aCode) throws SharedMemoryException {
        ErrorCodeEnum tempCode = check(aCode);
        if (tempCode != ErrorCodeEnum.OK) {
            throw new SharedMemoryException(tempCode, (int) aCode);
        }
    }

    public static boolean checkCodeSpin(long aCode) throws SharedMemoryException {
        ErrorCodeEnum tempCode = check(aCode);
        if (tempCode == ErrorCodeEnum.OK) {
            return true;
        }

        if (tempCode == ErrorCodeEnum.SPIN_TIMEOUT) {
            return false;
        }

        throw new SharedMemoryException(tempCode, (int) aCode);
    }

    /**
     * Throws an exception if this error cannot be handled.
     */
    public static ErrorCodeEnum check(long aCode) throws SharedMemoryException {
        if (aCode == 0) {
            return ErrorCodeEnum.OK;
        }

        int tempMyCode = (int) (aCode >> 32);
        int tempSysCode = (int) aCode;
        ErrorCodeEnum tempCode = ErrorCodeEnum.get(tempMyCode);
        if (tempCode == null) {
            throw new SharedMemoryException("Unexpected native return code: " + tempMyCode, ErrorCodeEnum.ERROR, tempSysCode);
        }

        switch (tempCode) {
            case INTERRUPT_TIMEOUT:
                return tempCode;
            case CMPXCHG_FAILED:
                return tempCode;
            case POLL_SERVER_TIMEOUT:
                return tempCode;
            case SPIN_TIMEOUT:
                return tempCode;
            case OUT_OF_MEMORY:
                throw new OutOfMemoryError(tempCode.getHumanReadableErrorMessage());
            default:
                throw new SharedMemoryException(tempCode, tempSysCode);
        }
    }

    /**
     * Returns true if the native code returned due to timeout.
     */
    public static boolean checkCodePollInterrupt(long aCode) throws SharedMemoryException {
        ErrorCodeEnum tempCode = check(aCode);
        if (tempCode == ErrorCodeEnum.OK) {
            return false;
        }

        if (tempCode == ErrorCodeEnum.INTERRUPT_TIMEOUT) {
            return true;
        }

        throw new SharedMemoryException(tempCode, (int) aCode);
    }

    /**
     * Returns true if the native code returned due to timeout.
     */
    public static boolean checkCodePollServer(long aCode) throws SharedMemoryException {
        ErrorCodeEnum tempCode = check(aCode);
        if (tempCode == ErrorCodeEnum.OK) {
            return false;
        }

        if (tempCode == ErrorCodeEnum.POLL_SERVER_TIMEOUT) {
            return true;
        }

        throw new SharedMemoryException(tempCode, (int) aCode);
    }
}
