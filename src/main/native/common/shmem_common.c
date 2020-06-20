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


#include "response.h"
#include "shmem_common.h"
#include "../common/jni/de_aschuetz_ivshmem4j_common_CommonSharedMemory.h"
#include "util/atomics.h"
#include "util/glibc_wrapper.h"
#include <string.h>
/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    write
 * Signature: (JJ[BII)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_write__JJ_3BII(
		JNIEnv *env, jclass aClazz, jlong aConnectionPtr, jlong aOffset,
		jbyteArray aBuffer, jint aBufferOffset, jint aLen) {
	struct mapped_shared_memory *tempConnection =
			(struct mapped_shared_memory*) aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	uint32_t tempBufferOffset = aBufferOffset;
	uint64_t tempOffset = aOffset;
	size_t tempToCopy = aLen;

	if (aBuffer == NULL
			|| (*env)->GetArrayLength(env, aBuffer) - tempBufferOffset
					< tempToCopy) {
		return combineErrorCode(RES_BUFFER_OUT_OF_BOUNDS, 0);
	}

	if (tempConnection->size <= tempOffset || tempConnection->size < tempOffset + tempToCopy) {
		return combineErrorCode(RES_MEMORY_OUT_OF_BOUNDS, 0);
	}

	void *tempResolvedBuffer = (*env)->GetPrimitiveArrayCritical(env, aBuffer,
			0);
	if (tempResolvedBuffer == NULL) {
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}
	void *tempSource = tempResolvedBuffer + tempBufferOffset;
	void *tempTarget = (tempConnection->memory + tempOffset);
	wrap_memcpy(tempTarget, tempSource, tempToCopy);
	(*env)->ReleasePrimitiveArrayCritical(env, aBuffer, tempResolvedBuffer,
			JNI_ABORT);
	return combineErrorCode(RES_OK, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    write
 * Signature: (JJB)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_write__JJB(
		JNIEnv *env, jclass aClazz, jlong aConnectionPtr, jlong aOffset,
		jbyte aByte) {
	struct mapped_shared_memory *tempConnection =
			(struct mapped_shared_memory*) aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	uint64_t tempOffset = aOffset;

	if (tempConnection->size <= tempOffset || tempConnection->size <= tempOffset) {
		return combineErrorCode(RES_MEMORY_OUT_OF_BOUNDS, 0);
	}

	jbyte *tempTarget = (jbyte*) (tempConnection->memory + tempOffset);
	*tempTarget = aByte;

	return combineErrorCode(RES_OK, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    write
 * Signature: (JJI)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_write__JJI(
		JNIEnv *env, jclass aClazz, jlong aConnectionPtr, jlong aOffset,
		jint aInt) {
	struct mapped_shared_memory *tempConnection =
			(struct mapped_shared_memory*) aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	uint64_t tempOffset = aOffset;

	if (tempConnection->size <= tempOffset || tempConnection->size < tempOffset + sizeof(jint)) {
		return combineErrorCode(RES_MEMORY_OUT_OF_BOUNDS, 0);
	}

	jint *tempTarget = (jint*) (tempConnection->memory + tempOffset);
	*tempTarget = aInt;

	return combineErrorCode(RES_OK, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    write
 * Signature: (JJJ)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_write__JJJ(
		JNIEnv *env, jclass aClazz, jlong aConnectionPtr, jlong aOffset,
		jlong aLong) {
	struct mapped_shared_memory *tempConnection =
			(struct mapped_shared_memory*) aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	uint64_t tempOffset = aOffset;

	if (tempConnection->size <= tempOffset || tempConnection->size < tempOffset + sizeof(jlong)) {
		return combineErrorCode(RES_MEMORY_OUT_OF_BOUNDS, 0);
	}

	jlong *tempTarget = (jlong*) (tempConnection->memory + tempOffset);
	*tempTarget = aLong;

	return combineErrorCode(RES_OK, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    write
 * Signature: (JJF)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_write__JJF(
		JNIEnv *env, jclass aClazz, jlong aConnectionPtr, jlong aOffset,
		jfloat aFloat) {
	struct mapped_shared_memory *tempConnection =
			(struct mapped_shared_memory*) aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	uint64_t tempOffset = aOffset;

	if (tempConnection->size <= tempOffset || tempConnection->size < tempOffset + sizeof(jfloat)) {
		return combineErrorCode(RES_MEMORY_OUT_OF_BOUNDS, 0);
	}

	jfloat *tempTarget = (jfloat*) (tempConnection->memory + tempOffset);
	*tempTarget = aFloat;

	return combineErrorCode(RES_OK, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    write
 * Signature: (JJD)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_write__JJD(
		JNIEnv *env, jclass aClazz, jlong aConnectionPtr, jlong aOffset,
		jdouble aDouble) {
	struct mapped_shared_memory *tempConnection =
			(struct mapped_shared_memory*) aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	uint64_t tempOffset = aOffset;

	if (tempConnection->size <= tempOffset || tempConnection->size < tempOffset + sizeof(jdouble)) {
		return combineErrorCode(RES_MEMORY_OUT_OF_BOUNDS, 0);
	}

	jdouble *tempTarget = (jdouble*) (tempConnection->memory + tempOffset);
	*tempTarget = aDouble;

	return combineErrorCode(RES_OK, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    write
 * Signature: (JJS)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_write__JJS(
		JNIEnv *env, jclass aClazz, jlong aConnectionPtr, jlong aOffset,
		jshort aShort) {
	struct mapped_shared_memory *tempConnection =
			(struct mapped_shared_memory*) aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	uint64_t tempOffset = aOffset;

	if (tempConnection->size <= tempOffset || tempConnection->size < tempOffset + sizeof(jshort)) {
		return combineErrorCode(RES_MEMORY_OUT_OF_BOUNDS, 0);
	}

	jshort *tempTarget = (jshort*) (tempConnection->memory + tempOffset);
	*tempTarget = aShort;

	return combineErrorCode(RES_OK, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    read
 * Signature: (JJ[BII)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_read__JJ_3BII(
		JNIEnv *env, jclass aClazz, jlong aConnectionPtr, jlong aOffset,
		jbyteArray aBuffer, jint aBufferOffset, jint aLen) {
	struct mapped_shared_memory *tempConnection =
			(struct mapped_shared_memory*) aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	uint32_t tempBufferOffset = aBufferOffset;
	uint64_t tempOffset = aOffset;
	size_t tempToCopy = aLen;

	if (aBuffer == NULL
			|| (*env)->GetArrayLength(env, aBuffer) - tempBufferOffset
					< tempToCopy) {
		return combineErrorCode(RES_BUFFER_OUT_OF_BOUNDS, 0);
	}

	if (tempConnection->size <= tempOffset || tempConnection->size < tempOffset + tempToCopy) {
		return combineErrorCode(RES_MEMORY_OUT_OF_BOUNDS, 0);
	}

	void *tempResolvedBuffer = (*env)->GetPrimitiveArrayCritical(env, aBuffer,
			0);
	if (tempResolvedBuffer == NULL) {
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}
	void *tempTarget = tempResolvedBuffer + tempBufferOffset;
	void *tempSource = (tempConnection->memory + tempOffset);
	wrap_memcpy(tempTarget, tempSource, tempToCopy);
	(*env)->ReleasePrimitiveArrayCritical(env, aBuffer, tempResolvedBuffer,
			JNI_OK);
	return combineErrorCode(RES_OK, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    read
 * Signature: (JJ[I)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_read__JJ_3I(
		JNIEnv *env, jclass aClazz, jlong aConnectionPtr, jlong aOffset,
		jintArray aInt) {
	if (aInt == NULL || (*env)->GetArrayLength(env, aInt) != 1) {
		return combineErrorCode(RES_INVALID_ARGUMENTS, 0);
	}

	struct mapped_shared_memory *tempConnection =
			(struct mapped_shared_memory*) aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	uint64_t tempOffset = aOffset;

	if (tempConnection->size <= tempOffset || tempConnection->size < tempOffset + sizeof(jint)) {
		return combineErrorCode(RES_MEMORY_OUT_OF_BOUNDS, 0);
	}

	void *tempResolvedBuffer = (void*) (*env)->GetPrimitiveArrayCritical(env,
			aInt, 0);
	if (tempResolvedBuffer == NULL) {
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}
	jint *tempTarget = (jint*) tempResolvedBuffer;
	jint *tempSource = (jint*) (tempConnection->memory + tempOffset);
	*tempTarget = *tempSource;
	(*env)->ReleasePrimitiveArrayCritical(env, aInt, tempResolvedBuffer,
			JNI_OK);
	return combineErrorCode(RES_OK, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    read
 * Signature: (JJ[J)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_read__JJ_3J(
		JNIEnv *env, jclass aClazz, jlong aConnectionPtr, jlong aOffset,
		jlongArray aLong) {
	if (aLong == NULL || (*env)->GetArrayLength(env, aLong) != 1) {
		return combineErrorCode(RES_INVALID_ARGUMENTS, 0);
	}

	struct mapped_shared_memory *tempConnection =
			(struct mapped_shared_memory*) aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	uint64_t tempOffset = aOffset;

	if (tempConnection->size <= tempOffset || tempConnection->size < tempOffset + sizeof(jlong)) {
		return combineErrorCode(RES_MEMORY_OUT_OF_BOUNDS, 0);
	}

	void *tempResolvedBuffer = (void*) (*env)->GetPrimitiveArrayCritical(env,
			aLong, 0);
	if (tempResolvedBuffer == NULL) {
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}
	jlong *tempTarget = (jlong*) tempResolvedBuffer;
	jlong *tempSource = (jlong*) (tempConnection->memory + tempOffset);

	*tempTarget = *tempSource;
	(*env)->ReleasePrimitiveArrayCritical(env, aLong, tempResolvedBuffer,
			JNI_OK);
	return combineErrorCode(RES_OK, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    read
 * Signature: (JJ[F)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_read__JJ_3F(
		JNIEnv *env, jclass aClazz, jlong aConnectionPtr, jlong aOffset,
		jfloatArray aFloat) {
	if (aFloat == NULL || (*env)->GetArrayLength(env, aFloat) != 1) {
		return combineErrorCode(RES_INVALID_ARGUMENTS, 0);
	}

	struct mapped_shared_memory *tempConnection =
			(struct mapped_shared_memory*) aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	uint64_t tempOffset = aOffset;

	if (tempConnection->size <= tempOffset || tempConnection->size < tempOffset + sizeof(jfloat)) {
		return combineErrorCode(RES_MEMORY_OUT_OF_BOUNDS, 0);
	}

	void *tempResolvedBuffer = (void*) (*env)->GetPrimitiveArrayCritical(env,
			aFloat, 0);
	if (tempResolvedBuffer == NULL) {
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}
	jfloat *tempTarget = (jfloat*) tempResolvedBuffer;
	jfloat *tempSource = (jfloat*) (tempConnection->memory + tempOffset);

	*tempTarget = *tempSource;
	(*env)->ReleasePrimitiveArrayCritical(env, aFloat, tempResolvedBuffer,
			JNI_OK);
	return combineErrorCode(RES_OK, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    read
 * Signature: (JJ[D)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_read__JJ_3D(
		JNIEnv *env, jclass aClazz, jlong aConnectionPtr, jlong aOffset,
		jdoubleArray aDouble) {
	if (aDouble == NULL || (*env)->GetArrayLength(env, aDouble) != 1) {
		return combineErrorCode(RES_INVALID_ARGUMENTS, 0);
	}

	struct mapped_shared_memory *tempConnection =
			(struct mapped_shared_memory*) aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	uint64_t tempOffset = aOffset;

	if (tempConnection->size <= tempOffset || tempConnection->size < tempOffset + sizeof(jdouble)) {
		return combineErrorCode(RES_MEMORY_OUT_OF_BOUNDS, 0);
	}

	void *tempResolvedBuffer = (void*) (*env)->GetPrimitiveArrayCritical(env,
			aDouble, 0);
	if (tempResolvedBuffer == NULL) {
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}
	jdouble *tempTarget = (jdouble*) tempResolvedBuffer;
	jdouble *tempSource = (jdouble*) (tempConnection->memory + tempOffset);
	*tempTarget = *tempSource;
	(*env)->ReleasePrimitiveArrayCritical(env, aDouble, tempResolvedBuffer,
			JNI_OK);
	return combineErrorCode(RES_OK, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    read
 * Signature: (JJ[S)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_read__JJ_3S(
		JNIEnv *env, jclass aClazz, jlong aConnectionPtr, jlong aOffset,
		jshortArray aShort) {
	if (aShort == NULL || (*env)->GetArrayLength(env, aShort) != 1) {
		return combineErrorCode(RES_INVALID_ARGUMENTS, 0);
	}

	struct mapped_shared_memory *tempConnection =
			(struct mapped_shared_memory*) aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	uint64_t tempOffset = aOffset;

	if (tempConnection->size <= tempOffset || tempConnection->size < tempOffset + sizeof(jshort)) {
		return combineErrorCode(RES_MEMORY_OUT_OF_BOUNDS, 0);
	}

	void *tempResolvedBuffer = (void*) (*env)->GetPrimitiveArrayCritical(env,
			aShort, 0);
	if (tempResolvedBuffer == NULL) {
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}
	jshort *tempTarget = (jshort*) tempResolvedBuffer;
	jshort *tempSource = (jshort*) (tempConnection->memory + tempOffset);
	*tempTarget = *tempSource;
	(*env)->ReleasePrimitiveArrayCritical(env, aShort, tempResolvedBuffer,
			JNI_OK);
	return combineErrorCode(RES_OK, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    read
 * Signature: (JJ[B)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_read__JJ_3B(
		JNIEnv *env, jclass aClazz, jlong aConnectionPtr, jlong aOffset,
		jbyteArray aByte) {
	if (aByte == NULL || (*env)->GetArrayLength(env, aByte) != 1) {
		return combineErrorCode(RES_INVALID_ARGUMENTS, 0);
	}

	struct mapped_shared_memory *tempConnection =
			(struct mapped_shared_memory*) aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	uint64_t tempOffset = aOffset;

	if (tempConnection->size <= tempOffset || tempConnection->size < tempOffset + sizeof(jbyte)) {
		return combineErrorCode(RES_MEMORY_OUT_OF_BOUNDS, 0);
	}

	void *tempResolvedBuffer = (void*) (*env)->GetPrimitiveArrayCritical(env,
			aByte, 0);

	if (tempResolvedBuffer == NULL) {
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}

	jbyte *tempTarget = (jbyte*) tempResolvedBuffer;
	jbyte *tempSource = (jbyte*) (tempConnection->memory + tempOffset);
	*tempTarget = *tempSource;
	(*env)->ReleasePrimitiveArrayCritical(env, aByte, tempResolvedBuffer,
			JNI_OK);
	return combineErrorCode(RES_OK, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    getAndSet
 * Signature: (JJ[J)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_getAndSet__JJ_3J
	(JNIEnv *env, jclass aClazz, jlong aConnectionPtr, jlong aOffset, jlongArray aLong) {
	if (aLong == NULL || (*env)->GetArrayLength(env, aLong) != 1) {
		return combineErrorCode(RES_INVALID_ARGUMENTS, 0);
	}

	struct mapped_shared_memory *tempConnection =
			(struct mapped_shared_memory*) aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	uint64_t tempOffset = aOffset;

	if (tempConnection->size <= tempOffset || tempConnection->size < tempOffset + sizeof(jlong)) {
		return combineErrorCode(RES_MEMORY_OUT_OF_BOUNDS, 0);
	}


	void *tempResolvedBuffer = (void*) (*env)->GetPrimitiveArrayCritical(env,
				aLong, 0);
	if (tempResolvedBuffer == NULL) {
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}

	uint64_t *tempTarget = (uint64_t*) tempResolvedBuffer;
	uint64_t *tempSource = (uint64_t*) (tempConnection->memory + tempOffset);
	*tempTarget = xchg8b(tempSource, *tempTarget);
	(*env)->ReleasePrimitiveArrayCritical(env, aLong, tempResolvedBuffer,
			JNI_OK);
	return combineErrorCode(RES_OK, 0);

}

/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    getAndSet
 * Signature: (JJ[I)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_getAndSet__JJ_3I
  (JNIEnv *env, jclass aClazz, jlong aConnectionPtr, jlong aOffset, jintArray aInt) {
	if (aInt == NULL || (*env)->GetArrayLength(env, aInt) != 1) {
		return combineErrorCode(RES_INVALID_ARGUMENTS, 0);
	}

	struct mapped_shared_memory *tempConnection =
			(struct mapped_shared_memory*) aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	uint64_t tempOffset = aOffset;

	if (tempConnection->size <= tempOffset || tempConnection->size < tempOffset + sizeof(jint)) {
		return combineErrorCode(RES_MEMORY_OUT_OF_BOUNDS, 0);
	}


	void *tempResolvedBuffer = (void*) (*env)->GetPrimitiveArrayCritical(env,
			aInt, 0);
	if (tempResolvedBuffer == NULL) {
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}

	uint32_t *tempTarget = (uint32_t*) tempResolvedBuffer;
	uint32_t *tempSource = (uint32_t*) (tempConnection->memory + tempOffset);
	*tempTarget = xchg4b(tempSource, *tempTarget);
	(*env)->ReleasePrimitiveArrayCritical(env, aInt, tempResolvedBuffer,
			JNI_OK);
	return combineErrorCode(RES_OK, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    getAndSet
 * Signature: (JJ[S)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_getAndSet__JJ_3S
  (JNIEnv *env, jclass aClazz, jlong aConnectionPtr, jlong aOffset, jshortArray aShort) {
	if (aShort == NULL || (*env)->GetArrayLength(env, aShort) != 1) {
		return combineErrorCode(RES_INVALID_ARGUMENTS, 0);
	}

	struct mapped_shared_memory *tempConnection =
			(struct mapped_shared_memory*) aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	uint64_t tempOffset = aOffset;

	if (tempConnection->size <= tempOffset || tempConnection->size < tempOffset + sizeof(jshort)) {
		return combineErrorCode(RES_MEMORY_OUT_OF_BOUNDS, 0);
	}


	void *tempResolvedBuffer = (void*) (*env)->GetPrimitiveArrayCritical(env,
			aShort, 0);
	if (tempResolvedBuffer == NULL) {
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}

	uint16_t *tempTarget = (uint16_t*) tempResolvedBuffer;
	uint16_t *tempSource = (uint16_t*) (tempConnection->memory + tempOffset);
	*tempTarget = xchg2b(tempSource, *tempTarget);
	(*env)->ReleasePrimitiveArrayCritical(env, aShort, tempResolvedBuffer,
			JNI_OK);
	return combineErrorCode(RES_OK, 0);

}

/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    getAndSet
 * Signature: (JJ[B)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_getAndSet__JJ_3B
  (JNIEnv *env, jclass aClazz, jlong aConnectionPtr, jlong aOffset, jbyteArray aByte) {
	if (aByte == NULL || (*env)->GetArrayLength(env, aByte) != 1) {
		return combineErrorCode(RES_INVALID_ARGUMENTS, 0);
	}

	struct mapped_shared_memory *tempConnection =
			(struct mapped_shared_memory*) aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	uint64_t tempOffset = aOffset;

	if (tempConnection->size <= tempOffset || tempConnection->size < tempOffset + sizeof(jbyte)) {
		return combineErrorCode(RES_MEMORY_OUT_OF_BOUNDS, 0);
	}


	void *tempResolvedBuffer = (void*) (*env)->GetPrimitiveArrayCritical(env,
			aByte, 0);
	if (tempResolvedBuffer == NULL) {
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}

	uint8_t *tempTarget = (uint8_t*) tempResolvedBuffer;
	uint8_t *tempSource = (uint8_t*) (tempConnection->memory + tempOffset);
	*tempTarget = xchg1b(tempSource, *tempTarget);
	(*env)->ReleasePrimitiveArrayCritical(env, aByte, tempResolvedBuffer,
			JNI_OK);
	return combineErrorCode(RES_OK, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    getAndAdd
 * Signature: (JJ[J)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_getAndAdd__JJ_3J(
		JNIEnv *env, jclass aClazz, jlong aConnectionPtr, jlong aOffset,
		jlongArray aLong) {
	if (aLong == NULL || (*env)->GetArrayLength(env, aLong) != 1) {
		return combineErrorCode(RES_INVALID_ARGUMENTS, 0);
	}

	struct mapped_shared_memory *tempConnection =
			(struct mapped_shared_memory*) aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	uint64_t tempOffset = aOffset;

	if (tempConnection->size <= tempOffset || tempConnection->size < tempOffset + sizeof(jlong)) {
		return combineErrorCode(RES_MEMORY_OUT_OF_BOUNDS, 0);
	}

	void *tempResolvedBuffer = (void*) (*env)->GetPrimitiveArrayCritical(env,
			aLong, 0);
	if (tempResolvedBuffer == NULL) {
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}
	uint64_t *tempTarget = (uint64_t*) tempResolvedBuffer;
	uint64_t *tempSource = (uint64_t*) (tempConnection->memory + tempOffset);
	*tempTarget = xadd8b(tempSource, *tempTarget);
	(*env)->ReleasePrimitiveArrayCritical(env, aLong, tempResolvedBuffer,
			JNI_OK);
	return combineErrorCode(RES_OK, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    getAndAdd
 * Signature: (JJ[I)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_getAndAdd__JJ_3I(
		JNIEnv *env, jclass aClazz, jlong aConnectionPtr, jlong aOffset,
		jintArray aInt) {
	if (aInt == NULL || (*env)->GetArrayLength(env, aInt) != 1) {
		return combineErrorCode(RES_INVALID_ARGUMENTS, 0);
	}

	struct mapped_shared_memory *tempConnection =
			(struct mapped_shared_memory*) aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	uint64_t tempOffset = aOffset;

	if (tempConnection->size <= tempOffset || tempConnection->size < tempOffset + sizeof(jint)) {
		return combineErrorCode(RES_MEMORY_OUT_OF_BOUNDS, 0);
	}

	void *tempResolvedBuffer = (void*) (*env)->GetPrimitiveArrayCritical(env,
			aInt, 0);
	if (tempResolvedBuffer == NULL) {
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}
	uint32_t *tempTarget = (uint32_t*) tempResolvedBuffer;
	uint32_t *tempSource = (uint32_t*) (tempConnection->memory + tempOffset);
	*tempTarget = xadd4b(tempSource, *tempTarget);
	(*env)->ReleasePrimitiveArrayCritical(env, aInt, tempResolvedBuffer,
			JNI_OK);
	return combineErrorCode(RES_OK, 0);
}
/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    getAndAdd
 * Signature: (JJ[S)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_getAndAdd__JJ_3S(
		JNIEnv *env, jclass aClazz, jlong aConnectionPtr, jlong aOffset,
		jshortArray aShort) {
	if (aShort == NULL || (*env)->GetArrayLength(env, aShort) != 1) {
		return combineErrorCode(RES_INVALID_ARGUMENTS, 0);
	}

	struct mapped_shared_memory *tempConnection =
			(struct mapped_shared_memory*) aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	uint64_t tempOffset = aOffset;

	if (tempConnection->size <= tempOffset || tempConnection->size < tempOffset + sizeof(jshort)) {
		return combineErrorCode(RES_MEMORY_OUT_OF_BOUNDS, 0);
	}

	void *tempResolvedBuffer = (void*) (*env)->GetPrimitiveArrayCritical(env,
			aShort, 0);
	if (tempResolvedBuffer == NULL) {
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}
	uint16_t *tempTarget = (uint16_t*) tempResolvedBuffer;
	uint16_t *tempSource = (uint16_t*) (tempConnection->memory + tempOffset);
	*tempTarget = xadd2b(tempSource, *tempTarget);
	(*env)->ReleasePrimitiveArrayCritical(env, aShort, tempResolvedBuffer,
			JNI_OK);
	return combineErrorCode(RES_OK, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    getAndAdd
 * Signature: (JJ[B)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_getAndAdd__JJ_3B(
		JNIEnv *env, jclass aClazz, jlong aConnectionPtr, jlong aOffset,
		jbyteArray aByte) {
	if (aByte == NULL || (*env)->GetArrayLength(env, aByte) != 1) {
		return combineErrorCode(RES_INVALID_ARGUMENTS, 0);
	}

	struct mapped_shared_memory *tempConnection =
			(struct mapped_shared_memory*) aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	uint64_t tempOffset = aOffset;

	if (tempConnection->size <= tempOffset || tempConnection->size < tempOffset + sizeof(jbyte)) {
		return combineErrorCode(RES_MEMORY_OUT_OF_BOUNDS, 0);
	}

	void *tempResolvedBuffer = (void*) (*env)->GetPrimitiveArrayCritical(env,
			aByte, 0);
	if (tempResolvedBuffer == NULL) {
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}
	uint8_t *tempTarget = (uint8_t*) tempResolvedBuffer;
	uint8_t *tempSource = (uint8_t*) (tempConnection->memory + tempOffset);
	*tempTarget = xadd1b(tempSource, *tempTarget);
	(*env)->ReleasePrimitiveArrayCritical(env, tempResolvedBuffer,
			tempResolvedBuffer, JNI_OK);
	return combineErrorCode(RES_OK, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    compareAndSet
 * Signature: (JJJJ)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_compareAndSet__JJJJ(
		JNIEnv *env, jclass aClazz, jlong aConnectionPtr, jlong aOffset,
		jlong aExpect, jlong aUpdate) {
	struct mapped_shared_memory *tempConnection =
			(struct mapped_shared_memory*) aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	uint64_t tempOffset = aOffset;

	if (tempConnection->size <= tempOffset || tempConnection->size < tempOffset + sizeof(jlong)) {
		return combineErrorCode(RES_MEMORY_OUT_OF_BOUNDS, 0);
	}

	uint64_t *tempTarget = (uint64_t*) (tempConnection->memory + tempOffset);

	if (cmpxchg8b(tempTarget, (uint64_t) aExpect, (uint64_t) aUpdate)) {
		return combineErrorCode(RES_OK, 0);
	}

	return combineErrorCode(RES_CMPXCHG_FAILED, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    compareAndSet
 * Signature: (JJII)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_compareAndSet__JJII(
		JNIEnv *env, jclass aClazz, jlong aConnectionPtr, jlong aOffset,
		jint aExpect, jint aUpdate) {
	struct mapped_shared_memory *tempConnection =
			(struct mapped_shared_memory*) aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	uint64_t tempOffset = aOffset;

	if (tempConnection->size <= tempOffset || tempConnection->size < tempOffset + sizeof(jint)) {
		return combineErrorCode(RES_MEMORY_OUT_OF_BOUNDS, 0);
	}

	uint32_t *tempTarget = (uint32_t*) (tempConnection->memory + tempOffset);

	if (cmpxchg4b(tempTarget, (uint32_t) aExpect, (uint32_t) aUpdate)) {
		return combineErrorCode(RES_OK, 0);
	}

	return combineErrorCode(RES_CMPXCHG_FAILED, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    compareAndSet
 * Signature: (JJSS)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_compareAndSet__JJSS(
		JNIEnv *env, jclass aClazz, jlong aConnectionPtr, jlong aOffset,
		jshort aExpect, jshort aUpdate) {
	struct mapped_shared_memory *tempConnection =
			(struct mapped_shared_memory*) aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	uint64_t tempOffset = aOffset;

	if (tempConnection->size <= tempOffset || tempConnection->size < tempOffset + sizeof(jshort)) {
		return combineErrorCode(RES_MEMORY_OUT_OF_BOUNDS, 0);
	}

	uint16_t *tempTarget = (uint16_t*) (tempConnection->memory + tempOffset);

	if (cmpxchg2b(tempTarget, (uint16_t) aExpect, (uint16_t) aUpdate)) {
		return combineErrorCode(RES_OK, 0);
	}

	return combineErrorCode(RES_CMPXCHG_FAILED, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    compareAndSet
 * Signature: (JJBB)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_compareAndSet__JJBB(
		JNIEnv *env, jclass aClazz, jlong aConnectionPtr, jlong aOffset,
		jbyte aExpect, jbyte aUpdate) {
	struct mapped_shared_memory *tempConnection =
			(struct mapped_shared_memory*) aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	uint64_t tempOffset = aOffset;

	if (tempConnection->size <= tempOffset || tempConnection->size < tempOffset + sizeof(jbyte)) {
		return combineErrorCode(RES_MEMORY_OUT_OF_BOUNDS, 0);
	}

	uint8_t *tempTarget = (uint8_t*) (tempConnection->memory + tempOffset);

	if (cmpxchg1b(tempTarget, (uint8_t) aExpect, (uint8_t) aUpdate)) {
		return combineErrorCode(RES_OK, 0);
	}

	return combineErrorCode(RES_CMPXCHG_FAILED, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    compareAndSet
 * Signature: (JJ[B)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_compareAndSet__JJ_3B
   (JNIEnv *env, jclass aClazz, jlong aConnectionPtr, jlong aOffset,
		jbyteArray aData) {
	if (aData == NULL || (*env)->GetArrayLength(env, aData) != 32) {
		return combineErrorCode(RES_INVALID_ARGUMENTS, 0);
	}

	struct mapped_shared_memory *tempConnection =
			(struct mapped_shared_memory*) aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	uint64_t tempOffset = aOffset;

	if (tempConnection->size <= tempOffset || tempConnection->size < tempOffset + 16) {
		return combineErrorCode(RES_MEMORY_OUT_OF_BOUNDS, 0);
	}

	void *tempTarget = (tempConnection->memory + tempOffset);
	void *tempResolvedBuffer = (void*) (*env)->GetPrimitiveArrayCritical(env,
			aData, 0);
	if (tempResolvedBuffer == NULL) {
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}
	if (cmpxchg16b(tempTarget, (uint64_t*) tempResolvedBuffer)) {
		(*env)->ReleasePrimitiveArrayCritical(env, aData, tempResolvedBuffer,
				JNI_ABORT);
		return combineErrorCode(RES_OK, 0);
	}

	(*env)->ReleasePrimitiveArrayCritical(env, aData, tempResolvedBuffer,
			JNI_ABORT);
	return combineErrorCode(RES_CMPXCHG_FAILED, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    memset
 * Signature: (JJBJ)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_memset
  (JNIEnv *env, jclass aClazz, jlong aConnectionPtr, jlong aOffset, jbyte aValue, jlong aLen) {
	struct mapped_shared_memory *tempConnection =
			(struct mapped_shared_memory*) aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	uint64_t tempOffset = aOffset;
	uint64_t tempLen = aLen;



	if (tempConnection->size <= tempOffset || tempConnection->size < tempOffset + tempLen || tempOffset + tempLen < tempOffset) {
		return combineErrorCode(RES_MEMORY_OUT_OF_BOUNDS, 0);
	}

	if (tempLen == 0) {
		//NOOP...
		return combineErrorCode(RES_OK, 0);
	}

	void *tempTarget = (tempConnection->memory + tempOffset);

	memset(tempTarget, ((unsigned char)aValue), tempLen);

	return combineErrorCode(RES_OK, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    markClosed
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_markClosed
  (JNIEnv *env, jclass aClazz, jlong aConnectionPtr) {
	struct mapped_shared_memory *tempConnection =
				(struct mapped_shared_memory*) aConnectionPtr;

	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	tempConnection->closed = true;

	return combineErrorCode(RES_OK, 0);
}
/*
 * Class:     de_aschuetz_ivshmem4j_common_CommonSharedMemory
 * Method:    getNativeLibVersion
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_common_CommonSharedMemory_getNativeLibVersion
  (JNIEnv *env, jclass aClazz) {
	return 0;
}

