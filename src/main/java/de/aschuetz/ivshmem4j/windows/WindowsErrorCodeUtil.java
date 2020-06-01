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

import de.aschuetz.ivshmem4j.api.SharedMemoryException;
import de.aschuetz.ivshmem4j.common.CommonErrorCodeUtil;
import de.aschuetz.ivshmem4j.common.ErrorCodeEnum;

public class WindowsErrorCodeUtil extends CommonErrorCodeUtil {


    /**
     * Throws an Exception unless the result is OK.
     */
    protected static void checkCodeOK(long aCode) throws SharedMemoryException {
        CommonErrorCodeUtil.checkCodeOK(aCode);
    }

    /**
     * Throws an exception if this error cannot be handled.
     */
    protected static ErrorCodeEnum check(long aCode) throws SharedMemoryException {
        return CommonErrorCodeUtil.check(aCode);
    }

    /**
     * Returns true if the native code returned due to timeout.
     */
    protected static boolean checkCodePollInterrupt(long aCode) throws SharedMemoryException {
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
    protected static boolean checkCodePollServer(long aCode) throws SharedMemoryException {
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
