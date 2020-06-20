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

#include <initguid.h>
#include <windows.h>
#include <setupapi.h>
#include <stdint.h>
#include <stdbool.h>
#include "../common/jni/de_aschuetz_ivshmem4j_windows_WindowsSharedMemory.h"
#include "../common/response.h"
#include "../common/util/atomics.h"
#include "../common/shmem_common.h"
#include "../common/util/linked_list.h"
#include "../common/util/glibc_wrapper.h"

DEFINE_GUID(DEVICE_GUID, 0xdf576976, 0x569d, 0x4672, 0x95, 0xa0, 0xf5, 0x7e,
		0x4e, 0xa0, 0xb2, 0x10);

#define CACHE_NONCACHED 0;
#define CACHE_CACHED 1;
#define CACHE_WRITECOMBINED 2;
#define REQUEST_PEERID CTL_CODE(FILE_DEVICE_UNKNOWN, 0x800, METHOD_BUFFERED, FILE_ANY_ACCESS)
#define REQUEST_SIZE   CTL_CODE(FILE_DEVICE_UNKNOWN, 0x801, METHOD_BUFFERED, FILE_ANY_ACCESS)
#define REQUEST_MMAP   CTL_CODE(FILE_DEVICE_UNKNOWN, 0x802, METHOD_BUFFERED, FILE_ANY_ACCESS)
#define RELEASE_MMAP   CTL_CODE(FILE_DEVICE_UNKNOWN, 0x803, METHOD_BUFFERED, FILE_ANY_ACCESS)
#define TRIGGER_INTERRUPT  CTL_CODE(FILE_DEVICE_UNKNOWN, 0x804, METHOD_BUFFERED, FILE_ANY_ACCESS)
#define REGISTER_INTERRUPT_WAIT CTL_CODE(FILE_DEVICE_UNKNOWN, 0x805, METHOD_BUFFERED, FILE_ANY_ACCESS)
#define MAX_SUPPORTED_INTERRUPTS 32

struct ivshmem_driver_memory_map {
	uint16_t peer;
	uint64_t size;
	void *memory;
	uint16_t vector_count;
};

struct ivshmem_driver_interrupt_send {
	uint16_t peerID;
	uint16_t vector;
};

struct ivshmem_driver_interrupt_receive {
	uint16_t vector;
	HANDLE event;
	bool singleShot;
};

struct ivshmem_device {
	uint64_t sharedMemorySize;
	uint32_t nameLength;
	char name[];
};

struct ivshmem_mapped_device {
	struct mapped_shared_memory mapped;
	HANDLE handle;
	struct ivshmem_driver_memory_map map;
	struct ivshmem_device *device;
	struct ivshmem_driver_interrupt_receive interrupts[MAX_SUPPORTED_INTERRUPTS];
};

struct ivshmem_device_node {
	linked_list_node node;
	struct ivshmem_device *device;
};

//Code

void node_dealloc(linked_list_node *aNode) {
	struct ivshmem_device_node *tempDevice = (struct ivshmem_device_node*) aNode;
	if (tempDevice == NULL) {
		return;
	}

	if (tempDevice->device != NULL) {
		free(tempDevice->device);
		tempDevice->device = NULL;
	}

	free(tempDevice);
}

uint64_t getDevices(linked_list *aList) {

	linked_list_init(aList);

	HDEVINFO tempDeviceInfoSet = SetupDiGetClassDevs(NULL, NULL, NULL,
	DIGCF_PRESENT | DIGCF_ALLCLASSES | DIGCF_DEVICEINTERFACE);

	if (tempDeviceInfoSet == INVALID_HANDLE_VALUE) {
		return combineErrorCode(RES_ENUMERATE_PCI_DEVICE_ERROR, GetLastError());
	}

	SP_DEVICE_INTERFACE_DATA tempDeviceData;

	uint32_t currentIndex = 0;

	while (true) {
		memset(&tempDeviceData, 0, sizeof(SP_DEVICE_INTERFACE_DATA));
		tempDeviceData.cbSize = sizeof(SP_DEVICE_INTERFACE_DATA);

		bool tempSuccess;
		tempSuccess = SetupDiEnumDeviceInterfaces(tempDeviceInfoSet, NULL,
				&DEVICE_GUID, currentIndex, &tempDeviceData);

		if (!tempSuccess) {
			DWORD tempLastError = GetLastError();
			if (tempLastError == ERROR_NO_MORE_ITEMS) {
				return combineErrorCode(RES_OK, 0);
			}

			linked_list_clear(aList, &node_dealloc);
			return combineErrorCode(RES_ENUMERATE_PCI_DEVICE_ERROR,
					tempLastError);
		}

		DWORD tempDeviceDetailLength = 0;

		tempSuccess = SetupDiGetDeviceInterfaceDetail(tempDeviceInfoSet,
				&tempDeviceData, NULL, 0, &tempDeviceDetailLength, NULL);


		if (tempSuccess) {
			linked_list_clear(aList, &node_dealloc);

			return combineErrorCode(RES_ENUMERATE_PCI_DEVICE_ERROR,
					GetLastError());
		}

		if (!tempSuccess) {
			DWORD tempErr = GetLastError();
			if (tempErr != ERROR_INSUFFICIENT_BUFFER) {
				return combineErrorCode(RES_ENUMERATE_PCI_DEVICE_ERROR,
						tempErr);
			}
		}

		if (tempDeviceDetailLength < sizeof(SP_DEVICE_INTERFACE_DETAIL_DATA)) {
			linked_list_clear(aList, &node_dealloc);
			return combineErrorCode(RES_ENUMERATE_PCI_DEVICE_ERROR, 0);
		}

		SP_DEVICE_INTERFACE_DETAIL_DATA *tempDeviceDetail = malloc(
				tempDeviceDetailLength);

		if (tempDeviceDetail == NULL) {
			linked_list_clear(aList, &node_dealloc);
			return combineErrorCode(RES_OUT_OF_MEMORY, 0);
		}

		memset(tempDeviceDetail, 0, tempDeviceDetailLength);
		tempDeviceDetail->cbSize = sizeof(SP_DEVICE_INTERFACE_DETAIL_DATA);

		tempSuccess = SetupDiGetDeviceInterfaceDetail(tempDeviceInfoSet,
				&tempDeviceData, tempDeviceDetail, tempDeviceDetailLength, NULL,
				NULL);

		if (!tempSuccess || tempDeviceDetail->cbSize < sizeof(DWORD) + 1) {
			free(tempDeviceDetail);
			linked_list_clear(aList, &node_dealloc);
			return combineErrorCode(RES_ENUMERATE_PCI_DEVICE_ERROR,
					tempSuccess ? 0 : GetLastError());
		}

		HANDLE tempDeviceHandle = CreateFile(tempDeviceDetail->DevicePath, 0, 0,
				NULL,
				OPEN_EXISTING, 0, 0);

		if (tempDeviceHandle == INVALID_HANDLE_VALUE) {
			free(tempDeviceDetail);
			linked_list_clear(aList, &node_dealloc);
			return combineErrorCode(RES_OPEN_PCI_DEVICE_HANDLE_ERROR,
					GetLastError());
		}

		DWORD tempLength = 0;
		uint64_t tempSharedMemorySize = 0;
		tempSuccess = DeviceIoControl(tempDeviceHandle, REQUEST_SIZE,
		NULL, 0, &tempSharedMemorySize, sizeof(uint64_t), &tempLength,
		NULL);

		if (!tempSuccess || tempSharedMemorySize == 0) {
			free(tempDeviceDetail);
			CloseHandle(tempDeviceHandle);
			linked_list_clear(aList, &node_dealloc);
			return combineErrorCode(RES_OPEN_PCI_DEVICE_HANDLE_ERROR,
					tempSuccess ? 0 : GetLastError());
		}

		size_t tempDevicePathLength = tempDeviceDetailLength - sizeof(DWORD);

		struct ivshmem_device *tempDevice = malloc(
				sizeof(struct ivshmem_device) + tempDevicePathLength);

		if (tempDevice == NULL) {
			free(tempDeviceDetail);
			CloseHandle(tempDeviceHandle);
			linked_list_clear(aList, &node_dealloc);
			return combineErrorCode(RES_OUT_OF_MEMORY, 0);
		}

		struct ivshmem_device_node *tempNode = malloc(
				sizeof(struct ivshmem_device_node));

		if (tempNode == NULL) {
			free(tempDevice);
			free(tempDeviceDetail);
			CloseHandle(tempDeviceHandle);
			linked_list_clear(aList, &node_dealloc);
			return combineErrorCode(RES_OUT_OF_MEMORY, 0);
		}

		memset(tempDevice, 0,
				sizeof(struct ivshmem_device) + tempDevicePathLength);

		linked_list_node_init(&tempNode->node);
		tempNode->device = tempDevice;

		wrap_memcpy(&tempDevice->name[0], tempDeviceDetail->DevicePath,
				tempDevicePathLength);

		tempDevice->nameLength = tempDevicePathLength;
		tempDevice->sharedMemorySize = tempSharedMemorySize;

		CloseHandle(tempDeviceHandle);
		free(tempDeviceDetail);

		if (!linked_list_add_last(aList, &tempNode->node)) {
			linked_list_clear(aList, &node_dealloc);
			free(tempNode);
			free(tempDevice);
			return combineErrorCode(RES_TOO_MANY_PCI_DEVICES, 0);
		}

		currentIndex++;
	}
}

uint64_t mapDevice(struct ivshmem_mapped_device *aMappedDevice,
		struct ivshmem_device *aDevice) {
	memset(aMappedDevice, 0, sizeof(struct ivshmem_mapped_device));
	for (int i = 0; i < MAX_SUPPORTED_INTERRUPTS; i++) {
		aMappedDevice->interrupts[i].event = INVALID_HANDLE_VALUE;
	}
	aMappedDevice->device = aDevice;
	aMappedDevice->handle = CreateFile(aDevice->name, 0, 0, NULL,
	OPEN_EXISTING, 0, 0);

	if (aMappedDevice->handle == INVALID_HANDLE_VALUE) {
		memset(aMappedDevice, 0, sizeof(struct ivshmem_mapped_device));
		for (int i = 0; i < MAX_SUPPORTED_INTERRUPTS; i++) {
			aMappedDevice->interrupts[i].event = INVALID_HANDLE_VALUE;
		}
		aMappedDevice->handle = INVALID_HANDLE_VALUE;
		return combineErrorCode(RES_OPEN_FAILURE, GetLastError());
	}

	DWORD tempLength;
	uint8_t tempMode = CACHE_NONCACHED
	;
	bool tempSuccess = DeviceIoControl(aMappedDevice->handle, REQUEST_MMAP,
			&tempMode, sizeof(tempMode), &aMappedDevice->map,
			sizeof(struct ivshmem_driver_memory_map), &tempLength, NULL);

	if (!tempSuccess) {
		DWORD tempLastError = GetLastError();
		CloseHandle(aMappedDevice->handle);
		memset(aMappedDevice, 0, sizeof(struct ivshmem_mapped_device));
		for (int i = 0; i < MAX_SUPPORTED_INTERRUPTS; i++) {
			aMappedDevice->interrupts[i].event = INVALID_HANDLE_VALUE;
		}
		aMappedDevice->handle = INVALID_HANDLE_VALUE;
		return combineErrorCode(RES_ERROR_SHMEM_MMAP, tempLastError);
	}

	if (aMappedDevice->map.size != aDevice->sharedMemorySize) {
		CloseHandle(aMappedDevice->handle);
		memset(aMappedDevice, 0, sizeof(struct ivshmem_mapped_device));
		for (int i = 0; i < MAX_SUPPORTED_INTERRUPTS; i++) {
			aMappedDevice->interrupts[i].event = INVALID_HANDLE_VALUE;
		}
		aMappedDevice->handle = INVALID_HANDLE_VALUE;
		return combineErrorCode(RES_ERROR_MMAP_SIZE_CHANGED, 0);
	}

	if (aMappedDevice->map.memory == NULL) {
		CloseHandle(aMappedDevice->handle);
		memset(aMappedDevice, 0, sizeof(struct ivshmem_mapped_device));
		for (int i = 0; i < MAX_SUPPORTED_INTERRUPTS; i++) {
			aMappedDevice->interrupts[i].event = INVALID_HANDLE_VALUE;
		}
		aMappedDevice->handle = INVALID_HANDLE_VALUE;
		return combineErrorCode(RES_ERROR_SHMEM_MMAP, 0);
	}

	//Register interrups to the device driver.
	for (int i = 0;
			i < MAX_SUPPORTED_INTERRUPTS && i < aMappedDevice->map.vector_count;
			i++) {
		struct ivshmem_driver_interrupt_receive *tempInterrupt =
				&aMappedDevice->interrupts[i];

		tempInterrupt->event = CreateEvent(NULL, FALSE, FALSE, NULL);
		tempInterrupt->vector = (uint16_t) i;
		tempInterrupt->singleShot = FALSE;

		if (tempInterrupt->event == INVALID_HANDLE_VALUE) {
			DWORD tempLastError = GetLastError();
			CloseHandle(aMappedDevice->handle);
			for (int j = 0; j < i; j++) {
				CloseHandle(aMappedDevice->interrupts[j].event);
			}
			memset(aMappedDevice, 0, sizeof(struct ivshmem_mapped_device));
			for (int j = 0; j < MAX_SUPPORTED_INTERRUPTS; j++) {
				aMappedDevice->interrupts[j].event = INVALID_HANDLE_VALUE;
			}
			return combineErrorCode(RES_INTERRUPT_CREATE_EVENT_FAILURE,
					tempLastError);
		}

		DWORD tempLength;
		if (!DeviceIoControl(aMappedDevice->handle, REGISTER_INTERRUPT_WAIT,
				tempInterrupt, sizeof(struct ivshmem_driver_interrupt_receive),
				NULL, 0, &tempLength, NULL)) {
			DWORD tempLastError = GetLastError();
			CloseHandle(aMappedDevice->handle);
			for (int j = 0; j <= i; j++) {
				CloseHandle(aMappedDevice->interrupts[j].event);
			}
			memset(aMappedDevice, 0, sizeof(struct ivshmem_mapped_device));
			for (int j = 0; j < MAX_SUPPORTED_INTERRUPTS; j++) {
				aMappedDevice->interrupts[j].event = INVALID_HANDLE_VALUE;
			}
			return combineErrorCode(RES_INTERRUPT_EVENT_REGISTER_FAILURE,
					tempLastError);
		}
	}

	aMappedDevice->mapped.size = aMappedDevice->map.size;
	aMappedDevice->mapped.memory = aMappedDevice->map.memory;

	return combineErrorCode(RES_OK, 0);
}

void closeDevice(struct ivshmem_mapped_device *aMappedDevice) {
	if (aMappedDevice->handle != INVALID_HANDLE_VALUE) {
		CloseHandle(aMappedDevice->handle);
	}
	aMappedDevice->handle = INVALID_HANDLE_VALUE;

	for (int i = 0; i < MAX_SUPPORTED_INTERRUPTS; i++) {
		if (aMappedDevice->interrupts[i].event != INVALID_HANDLE_VALUE) {
			CloseHandle(aMappedDevice->interrupts[i].event);
			aMappedDevice->interrupts[i].event = INVALID_HANDLE_VALUE;
		}
	}

	aMappedDevice->mapped.memory = NULL;
	aMappedDevice->map.memory = NULL;
}

bool sendInterrupt(struct ivshmem_mapped_device *aMappedDevice,
		uint16_t aVector, uint16_t peer) {
	struct ivshmem_driver_interrupt_send tempInterrupt;
	tempInterrupt.peerID = peer;
	tempInterrupt.vector = aVector;

	DWORD tempLength = 0;
	return DeviceIoControl(aMappedDevice->handle, TRIGGER_INTERRUPT,
			&tempInterrupt, sizeof(struct ivshmem_driver_interrupt_send), NULL,
			0, &tempLength, NULL);
}

uint64_t pollInterrupt(struct ivshmem_mapped_device *aMappedDevice,
		bool someVectors[]) {

	for (int i = 0; i < MAX_SUPPORTED_INTERRUPTS; i++) {
		someVectors[i] = false;
	}

	DWORD tempInterruptCount =
			MAX_SUPPORTED_INTERRUPTS < aMappedDevice->map.vector_count ?
					MAX_SUPPORTED_INTERRUPTS : aMappedDevice->map.vector_count;

	if (tempInterruptCount == 0) {
		return combineErrorCode(RES_INTERRUPT_RECEIVE_NO_VECTORS, 0);
	}

	DWORD tempResult;
	bool tempFound = false;
	for (int i = 0; i < tempInterruptCount; i++) {

		tempResult = WaitForSingleObject(aMappedDevice->interrupts[i].event,
				WAIT_OBJECT_0);

		switch (tempResult) {
		case (WAIT_OBJECT_0): {
			tempFound = true;
			someVectors[i] = true;
			continue;
		}
		case (WAIT_TIMEOUT): {
			continue;
		}
		default: {
			return combineErrorCode(RES_INTERRUPT_RECEIVE_ERROR, GetLastError());
		}
		}
	}

	if (tempFound) {
		return combineErrorCode(RES_OK, 0);
	}

	HANDLE tempHandles[tempInterruptCount];

	for (int i = 0; i < tempInterruptCount; i++) {
		tempHandles[i] = aMappedDevice->interrupts[i].event;
	}

	tempResult = WaitForMultipleObjects(tempInterruptCount, &tempHandles[0],
			FALSE, 1000);

	switch (tempResult) {
	case (WAIT_TIMEOUT): {
		return combineErrorCode(RES_INTERRUPT_TIMEOUT, 0);
	}
	case (WAIT_FAILED): {
		return combineErrorCode(RES_INTERRUPT_RECEIVE_ERROR, GetLastError());
	}
	default: {
		if (tempResult - WAIT_OBJECT_0 >= tempInterruptCount) {
			return combineErrorCode(RES_INTERRUPT_RECEIVE_ERROR, GetLastError());
		}

		someVectors[tempResult - WAIT_OBJECT_0] = true;
		return combineErrorCode(RES_OK, 0);
	}
	}
}

//JNI

/*
 * Class:     de_aschuetz_ivshmem4j_windows_WindowsSharedMemory
 * Method:    getDevices
 * Signature: ([Ljava/lang/Object;)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_windows_WindowsSharedMemory_getDevices
  (JNIEnv *env, jclass clazz, jobjectArray result) {

	if (result == NULL || (*env)->GetArrayLength(env, result) != 1) {
		return combineErrorCode(RES_INVALID_ARGUMENTS, 0);
	}


	linked_list tempList;

	uint64_t tempRes = getDevices(&tempList);

	if (!checkErrorCode(tempRes, RES_OK)) {
		return tempRes;
	}

	uint32_t tempSize = tempList.size;

	jclass tempObjectClazz = (*env)->FindClass(env, "java/lang/Object");

	if (tempObjectClazz == NULL) {
		linked_list_clear(&tempList, &node_dealloc);
		return combineErrorCode(RES_ERROR, 0);
	}

	jobjectArray tempDeviceArray = (*env)->NewObjectArray(env, tempSize * 2,
			tempObjectClazz, NULL);

	if (tempDeviceArray == NULL) {
		linked_list_clear(&tempList, &node_dealloc);
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}

	linked_list_iterator tempIter;
	linked_list_iterator_ascending(&tempList, &tempIter);

	jsize tempIndex = 0;
	for (struct ivshmem_device_node *tempCurrent =
			(struct ivshmem_device_node*) linked_list_iterator_next(&tempIter);
			tempCurrent != NULL;
			tempCurrent =
					(struct ivshmem_device_node*) linked_list_iterator_next(
							&tempIter)) {

		jlong tempSizeLong = tempCurrent->device->sharedMemorySize;
		jlongArray tempSizeArray = (*env)->NewLongArray(env, 1);
		if (tempSizeArray == NULL) {
			linked_list_clear(&tempList, &node_dealloc);
			return combineErrorCode(RES_OUT_OF_MEMORY, 0);
		}

		(*env)->SetLongArrayRegion(env, tempSizeArray, 0, 1, &tempSizeLong);

		jbyteArray tempName = (*env)->NewByteArray(env,
				tempCurrent->device->nameLength);
		if (tempName == NULL) {
			linked_list_clear(&tempList, &node_dealloc);
			return combineErrorCode(RES_OUT_OF_MEMORY, 0);
		}

		(*env)->SetByteArrayRegion(env, tempName, 0,
				tempCurrent->device->nameLength,
				(jbyte*) &tempCurrent->device->name[0]);

		(*env)->SetObjectArrayElement(env, tempDeviceArray, tempIndex,
				tempSizeArray);
		tempIndex++;
		(*env)->SetObjectArrayElement(env, tempDeviceArray, tempIndex,
				tempName);
		tempIndex++;
	}

	linked_list_clear(&tempList, &node_dealloc);

	(*env)->SetObjectArrayElement(env, result, 0, tempDeviceArray);

	return combineErrorCode(RES_OK, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_windows_WindowsSharedMemory
 * Method:    openDevice
 * Signature: ([B[J)L
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_windows_WindowsSharedMemory_openDevice(
		JNIEnv *env, jclass aClazz, jbyteArray aName, jlongArray aHandle) {
	if (aName == NULL || aHandle == NULL
			|| (*env)->GetArrayLength(env, aHandle) != 3) {
		return combineErrorCode(RES_INVALID_ARGUMENTS, 0);
	}

	linked_list tempList;

	uint64_t tempRes = getDevices(&tempList);

	if (!checkErrorCode(tempRes, RES_OK)) {
		return tempRes;
	}

	linked_list_iterator tempIter;
	linked_list_iterator_ascending(&tempList, &tempIter);

	jsize tempSize = (*env)->GetArrayLength(env, aName);
	jbyte *tempBuf = (*env)->GetByteArrayElements(env, aName, NULL);
	if (tempBuf == NULL) {
		linked_list_clear(&tempList, &node_dealloc);
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}

	struct ivshmem_device *tempFound = NULL;
	for (struct ivshmem_device_node *tempCurrent =
			(struct ivshmem_device_node*) linked_list_iterator_next(&tempIter);
			tempCurrent != NULL;
			tempCurrent =
					(struct ivshmem_device_node*) linked_list_iterator_next(
							&tempIter)) {

		if (tempCurrent->device->nameLength != tempSize) {
			continue;
		}

		if (memcmp(tempCurrent->device->name, tempBuf, tempSize) != 0) {
			continue;
		}

		tempFound = tempCurrent->device;
		tempCurrent->device = NULL;
		break;
	}

	(*env)->ReleaseByteArrayElements(env, aName, tempBuf, JNI_ABORT);
	linked_list_clear(&tempList, &node_dealloc);
	if (tempFound == NULL) {
		return combineErrorCode(RES_INVALID_DEVICE_PATH, 0);
	}

	struct ivshmem_mapped_device *tempConnectedDevice = malloc(
			sizeof(struct ivshmem_mapped_device));
	if (tempConnectedDevice == NULL) {
		free(tempFound);
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}

	uint32_t tempResult = mapDevice(tempConnectedDevice, tempFound);
	switch (tempResult) {
	case (RES_OK): {
		jlong *tempHandle = (*env)->GetLongArrayElements(env, aHandle, NULL);
		tempHandle[0] = (jlong) (void*) tempConnectedDevice;
		tempHandle[1] = (jlong) tempConnectedDevice->map.peer;
		tempHandle[2] = (jlong) tempConnectedDevice->map.vector_count;
		(*env)->ReleaseLongArrayElements(env, aHandle, tempHandle, 0);

		return combineErrorCode(RES_OK, 0);
	}
	default: {
		free(tempConnectedDevice);
		free(tempFound);
		return tempResult;
	}
	}

}

/*
 * Class:     de_aschuetz_ivshmem4j_windows_WindowsSharedMemory
 * Method:    close
 * Signature: (J)L
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_windows_WindowsSharedMemory_close(
		JNIEnv *env, jclass aClazz, jlong aDevicePointer) {
	struct ivshmem_mapped_device *tempDevice =
			(struct ivshmem_mapped_device*) (void*) aDevicePointer;
	if (tempDevice == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	closeDevice(tempDevice);
	free(tempDevice->device);
	tempDevice->device = NULL;
	free(tempDevice);

	return combineErrorCode(RES_OK, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_windows_WindowsSharedMemory
 * Method:    sendInterrupt
 * Signature: (JII)L
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_windows_WindowsSharedMemory_sendInterrupt(
		JNIEnv *env, jclass aClazz, jlong aDevicePointer, jint aVector,
		jint peer) {
	struct ivshmem_mapped_device *tempDevice =
			(struct ivshmem_mapped_device*) (void*) aDevicePointer;
	if (tempDevice == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	if (tempDevice->map.vector_count <= aVector) {
		return combineErrorCode(RES_INTERRUPT_VECTOR_TOO_BIG, 0);
	}

	if (sendInterrupt(tempDevice, aVector, peer)) {
		return combineErrorCode(RES_OK, GetLastError());
	}

	return combineErrorCode(RES_INTERRUPT_SEND_ERROR, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_windows_WindowsSharedMemory
 * Method:    pollInterrupt
 * Signature: (J[I)L
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_windows_WindowsSharedMemory_pollInterrupt(
		JNIEnv *env, jclass aClazz, jlong aDevicePointer, jintArray someVectors) {
	if (someVectors == NULL
			|| (*env)->GetArrayLength(env, someVectors)
					!= MAX_SUPPORTED_INTERRUPTS + 1) {
		return combineErrorCode(RES_INVALID_ARGUMENTS, 0);
	}

	struct ivshmem_mapped_device *tempDevice =
			(struct ivshmem_mapped_device*) (void*) aDevicePointer;
	if (tempDevice == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	bool tempVectors[MAX_SUPPORTED_INTERRUPTS];
	uint64_t tempResult = pollInterrupt(tempDevice, tempVectors);
	if (tempResult != RES_OK) {
		return tempResult;
	}

	jint *tempResolvedBuffer = (*env)->GetPrimitiveArrayCritical(env,
			someVectors, 0);
	if (tempResolvedBuffer == NULL) {
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}

	jint tempI = 0;
	for (int i = 0; i < MAX_SUPPORTED_INTERRUPTS; i++) {
		if (tempVectors[i] == true) {
			tempResolvedBuffer[tempI + 1] = i;
			tempI++;
		}
	}
	tempVectors[0] = tempI;
	(*env)->ReleasePrimitiveArrayCritical(env, someVectors, tempResolvedBuffer,
			JNI_OK);

	return combineErrorCode(RES_OK, 0);
}
