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

#include <stdint.h>
#include <stdbool.h>

#ifndef RESPONSE_H_
#define RESPONSE_H_

//Common
#define RES_OK 0
#define RES_FD 1
#define RES_OUT_OF_MEMORY 999
#define RES_ERROR 998
#define RES_INVALID_DEVICE_PATH 9
#define RES_ERROR_CONNECTING_UNIX_SOCKET 10
#define RES_MUTEX_INIT_ERROR 11
#define RES_INTERRUPT_CANT_SELF_INTERRUPT 17
#define RES_INTERRUPT_VECTOR_TOO_BIG 19
#define RES_INTERRUPT_SEND_ERROR 21
#define RES_INTERRUPT_RECEIVE_ERROR 22
#define RES_INTERRUPT_RECEIVE_NO_VECTORS 23
#define RES_INTERRUPT_TIMEOUT 25
#define RES_INVALID_ARGUMENTS 28
#define RES_INVALID_CONNECTION_POINTER 29
#define RES_BUFFER_OUT_OF_BOUNDS 31
#define RES_MEMORY_OUT_OF_BOUNDS 32
#define RES_CMPXCHG_FAILED 34
#define RES_OPEN_FAILURE 35

//Linux common
#define RES_ERROR_SHMEM_FSTAT 24
#define RES_ERROR_SHMEM_MMAP 26

//Linux plain
#define RES_ERROR_SHMEM_FILE_SET_SIZE 33

//Linux doorbell
#define RES_PACKET_TOO_SHORT 2
#define RES_READ_ERROR 3
#define RES_UNKNOWN_IVSHMEM_PROTOCOLL_VERSION 4
#define RES_FD_MISSING 5
#define RES_UNEXPECTED_PACKET 6
#define RES_PEER_INVALID 7
#define RES_ERROR_CREATING_UNIX_SOCKET 8
#define RES_ERROR_SETTING_TIMEOUT_ON_UNIX_SOCKET 12
#define RES_PACKET_TIMEOUT 13
#define RES_CLOSED_UNKNOWN_PEER 14
#define RES_OWN_PEER_CLOSED 15
#define RES_DUPLICATE_PEER 16
#define RES_PEER_DOESNT_EXIST 18
#define RES_INTERRUPT_VECTOR_CLOSED 20
#define RES_PEER_NOT_FOUND 30
#define RES_POLL_SERVER_TIMEOUT 27


//Windows
#define RES_ERROR_MMAP_SIZE_CHANGED 36
#define RES_INTERRUPT_CREATE_EVENT_FAILURE 37
#define RES_INTERRUPT_EVENT_REGISTER_FAILURE 38
#define RES_ENUMERATE_PCI_DEVICE_ERROR 39
#define RES_OPEN_PCI_DEVICE_HANDLE_ERROR 40
#define RES_TOO_MANY_PCI_DEVICES 41

uint64_t combineErrorCode(int aMyCode, int aDetail);

bool checkErrorCode(uint64_t aCombined, int aMyCode);

int getErrorCode(uint64_t aCombined);

#endif /* RESPONSE_H_ */
