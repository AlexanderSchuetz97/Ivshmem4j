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

#include <sys/stat.h>
#include <fcntl.h>
#include <stdint.h>
#include <stdbool.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/mman.h>
#include <string.h>
#include <unistd.h>
#include "../common/response.h"
#include "../common/shmem_common.h"
#include "../common/jni/de_aschuetz_ivshmem4j_linux_plain_LinuxSharedMemory.h"

struct mapped_file {
	struct mapped_shared_memory map;
	int fd;
};

/*
 * Class:     de_aschuetz_ivshmem4j_linux_plain_LinuxSharedMemory
 * Method:    createOrOpenFile
 * Signature: (Ljava/lang/String;J[J)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_linux_plain_LinuxSharedMemory_createOrOpenFile
  (JNIEnv *env, jclass clazz, jstring aPath, jlong aPreferedSize, jlongArray aResult) {
	if (aResult == NULL || aPreferedSize <= 0 || aPath == NULL || (*env)->GetStringLength(env, aPath) <= 0 || (*env) ->GetArrayLength(env, aResult) != 2) {
		return combineErrorCode(RES_INVALID_ARGUMENTS, 0);
	}

	struct mapped_file* tempMap = malloc(sizeof(struct mapped_file));
	if (tempMap == NULL) {
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}

	memset(tempMap, 0 , sizeof(struct mapped_file));

	const char * tempChars = (*env)->GetStringUTFChars(env, aPath, 0);
	if (tempChars == NULL) {
		free(tempMap);
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}


	ssize_t tempLen = strlen(tempChars);
	char buf[tempLen+1];

	memcpy(&buf[0], tempChars, tempLen+1);
	(*env)->ReleaseStringUTFChars(env, aPath, tempChars);

	//permission 777
	tempMap->fd = open(buf, O_RDWR | O_CREAT, S_IRWXO | S_IRWXG | S_IRWXU);

	if (tempMap->fd == -1) {
		free(tempMap);
		return combineErrorCode(RES_OPEN_FAILURE, errno);
	}

	struct stat tempStat;
	memset(&tempStat, 0, sizeof(struct stat));

	if (fstat(tempMap->fd, &tempStat) == -1) {
		int err = errno;
		close(tempMap->fd);
		free(tempMap);
		return combineErrorCode(RES_ERROR_SHMEM_FSTAT, err);
	}

	tempMap->map.size = aPreferedSize;
	if (tempStat.st_size != 0) {
		tempMap->map.size = tempStat.st_size;
	} else {
		if (tempMap->map.size > 1 && lseek(tempMap->fd, tempMap->map.size-1, SEEK_SET) == -1) {
			int err = errno;
			close(tempMap->fd);
			free(tempMap);
			return combineErrorCode(RES_ERROR, err);
		}

		if (write(tempMap->fd, "", 1) != 1) {
			int err = errno;
			close(tempMap->fd);
			free(tempMap);
			return combineErrorCode(RES_ERROR, err);
		}
	}

	tempMap->map.memory = mmap(0, tempMap->map.size, PROT_READ | PROT_WRITE, MAP_SHARED, tempMap->fd, 0);
	if (tempMap->map.memory == (void*)-1 || tempMap->map.memory == NULL) {
		int err = errno;
		tempMap->map.memory = 0;
		close(tempMap->fd);
		free(tempMap);
		return combineErrorCode(RES_ERROR_SHMEM_MMAP, err);
	}

	jlong* tempBuf = (*env)->GetLongArrayElements(env, aResult, NULL);

	tempBuf[0] = (jlong) tempMap;
	tempBuf[1] = tempMap->map.size;

	(*env)->ReleaseLongArrayElements(env, aResult, tempBuf, JNI_OK);

	return combineErrorCode(RES_OK, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_linux_plain_LinuxSharedMemory
 * Method:    close
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_linux_plain_LinuxSharedMemory_close
  (JNIEnv * env, jclass clazz, jlong aFilePointer ) {
	struct mapped_file* tempMap = (struct mapped_file*) aFilePointer;
	if (tempMap == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	if (tempMap->map.memory != NULL) {
		munmap(tempMap->map.memory, tempMap->map.size);
		tempMap->map.memory = NULL;
	}

	if (tempMap->fd != -1) {
		close(tempMap->fd);
		tempMap->fd = -1;
	}

	free(tempMap);
	return combineErrorCode(RES_OK, 0);
}
