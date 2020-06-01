/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory */

#ifndef _Included_de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory
#define _Included_de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory
 * Method:    openDevice
 * Signature: (Ljava/lang/String;[J)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory_openDevice
  (JNIEnv *, jclass, jstring, jlongArray);

/*
 * Class:     de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory
 * Method:    getPeers
 * Signature: (J[J)[I
 */
JNIEXPORT jintArray JNICALL Java_de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory_getPeers
  (JNIEnv *, jclass, jlong, jlongArray);

/*
 * Class:     de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory
 * Method:    getVectors
 * Signature: (JI[I)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory_getVectors
  (JNIEnv *, jclass, jlong, jint, jintArray);

/*
 * Class:     de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory
 * Method:    close
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory_close
  (JNIEnv *, jclass, jlong);

/*
 * Class:     de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory
 * Method:    pollServer
 * Signature: (J[I)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory_pollServer
  (JNIEnv *, jclass, jlong, jintArray);

/*
 * Class:     de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory
 * Method:    sendInterrupt
 * Signature: (JII)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory_sendInterrupt
  (JNIEnv *, jclass, jlong, jint, jint);

/*
 * Class:     de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory
 * Method:    pollInterrupt
 * Signature: (J[I)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory_pollInterrupt
  (JNIEnv *, jclass, jlong, jintArray);

#ifdef __cplusplus
}
#endif
#endif
