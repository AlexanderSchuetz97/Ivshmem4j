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


import de.aschuetz.ivshmem4j.api.ErrorCode;

public enum ErrorCodeEnum implements ErrorCode {
    //Common
    OK(0, "OK"),
    FD(1, "File descriptor received"),
    OUT_OF_MEMORY(999, "Out of Memory in native code"),
    ERROR(998, "Unknown error"),
    INVALID_DEVICE_PATH(9, "Path to Unix domain socket or PCI Device is invalid"),
    ERROR_CONNECTING_UNIX_SOCKET(10, "Unable to connect to the unix domain socket of the IVSHMEM server"),
    MUTEX_INIT_ERROR(11, "Unable to createOrOpen native Mutex"),
    INTERRUPT_CANT_SELF_INTERRUPT(17, "Cannot send interrupt to own peer id"),
    INTERRUPT_VECTOR_TOO_BIG(19, "Interrupt vector is too big"),
    INTERRUPT_SEND_ERROR(21, "Error sending interrupt to peer"),
    INTERRUPT_RECEIVE_ERROR(22, "Error receiving interrupt from other peer"),
    INTERRUPT_RECEIVE_NO_VECTORS(23, "No interrupt vectors are available to receive"),
    INTERRUPT_TIMEOUT(25, "Timeout while waiting for interrupt"),
    INVALID_ARGUMENTS(28, "Invalid arguments passed to function"),
    INVALID_CONNECTION_POINTER(29, "The native handle is null"),
    BUFFER_OUT_OF_BOUNDS(31, "A buffer that was passed as argument is out of bounds"),
    MEMORY_OUT_OF_BOUNDS(32, "The shared memory is out of bounds"),
    CMPXCHG_FAILED(34, "Zero flag was not set after CMPXCHG instruction"),
    OPEN_FAILURE(35, "Failed to open the Shared Memory file or device Handle"),
    SPIN_CLOSED(44, "Shared memory was closed while spinning"),
    SPIN_TIMEOUT(45, "Timeout while spinning"),
    UNSUPPORTED_OPERATION(46, "Operation not supported by current CPU architecture"),
    OFFSET_UNALIGNED(47, "Operation requires aligned offset but was unaligned. (offset has to be dividable by operand size)"),


    //Linux common
    ERROR_SHMEM_FSTAT(24, "Unable to fstat the shared memory file descriptor"),
    ERROR_SHMEM_MMAP(26, "Unable to mmap the shared memory file descriptor"),

    //Linux Host Plain
    ERROR_SHMEM_FILE_SET_SIZE(33, "Unable to write the shared memory file size"),
    FILE_DOES_NOT_EXIST(42, "Shared memory file does not exist."),
    FILL_EMPTY_FILE_ERROR(33, "Error setting the new shared memory file size"),
    FILE_IS_EMPTY(43, "Shared memory file has a size of zero"),

    //Linux Host Doorbell
    PACKET_TOO_SHORT(2, "Received IVSHMEM packet from IVSHMEM server is too short"),
    READ_ERROR(3, "Error reading IVSHMEM packet from IVSHMEM server"),
    UNKNOWN_IVSHMEM_PROTOCOLL_VERSION(4, "IVSHMEM server is using a unknown IVSHMEM protocoll version"),
    FD_MISSING(5, "IVSHMEM packet did not contain a file descriptor but one was expected"),
    UNEXPECTED_PACKET(6, "Received a unexpected IVSHMEM packet"),
    PEER_INVALID(7, "IVSHMEM server transmitted an invalid peer id"),
    ERROR_CREATING_UNIX_SOCKET(8, "Unable to createOrOpen unix domain socket with the IVSHMEM server"),
    ERROR_SETTING_TIMEOUT_ON_UNIX_SOCKET(12, "Unable to write timeout on unix domain socket"),
    PACKET_TIMEOUT(13, "IVSHMEM packet read timeout from unix domain socket"),
    CLOSED_UNKNOWN_PEER(14, "IVSHMEM server send a disconnect notification of a previously unconnected peer"),
    OWN_PEER_CLOSED(15, "IVSHMEM server send a disconnect notification of our own peer"),
    DUPLICATE_PEER(16, "IVSHMEM server transmitted a connect notification of a peer that is already connected"),
    INTERRUPT_VECTOR_CLOSED(20, "Interrupt vector file descriptor is closed"),
    PEER_DOESNT_EXIST(18, "Peer does not exist"),
    PEER_NOT_FOUND(30, "The requested peer could not be found"),
    POLL_SERVER_TIMEOUT(27, "Timeout while reading packets from the IVSHMEM server"),


    //Windows Guest
    ERROR_MMAP_SIZE_CHANGED(36, "Size of shared memory changed unexpectedly after mapping PCI device"),
    INTERRUPT_CREATE_EVENT_FAILURE(37, "Failed to createOrOpen a event handle to wait for interrupts"),
    INTERRUPT_EVENT_REGISTER_FAILURE(38, "Failed to register a event handle to the PCI device driver"),
    ENUMERATE_PCI_DEVICE_ERROR(39, "Error enumerating pci devices"),
    OPEN_PCI_DEVICE_HANDLE_ERROR(40, "Error opening pci device handle"),
    TOO_MANY_PCI_DEVICES(41, "Too many pci devices to enumerate"),

    ;
    private final int code;

    private final String message;

    ErrorCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String getHumanReadableErrorMessage() {
        return message;
    }

    private static final ErrorCodeEnum[] CODES = new ErrorCodeEnum[1000];

    /**
     * The native JNI code generally only returns error codes in numbers. This method can be used to translate them
     * into enum states.
     */
    public static ErrorCodeEnum get(int aCode) {
        if (aCode > CODES.length || aCode < 0) {
            return null;
        }

        return CODES[aCode];
    }

    /**
     * The native JNI code generally only returns error codes in numbers. This method can be used to translate them
     * into enum states.
     */
    public static ErrorCodeEnum get(long aCode) {
        if (aCode == 0) {
            return ErrorCodeEnum.OK;
        }

        return get((int) (aCode >> 32));
    }

    static {
        for (ErrorCodeEnum tempErrorCode : ErrorCodeEnum.values()) {
            CODES[tempErrorCode.getCode()] = tempErrorCode;
        }
    }
}
