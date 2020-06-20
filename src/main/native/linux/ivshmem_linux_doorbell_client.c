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

#include <stdio.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/un.h>
#include <unistd.h>
#include <stdint.h>
#include <stdlib.h>
#include <byteswap.h>
#include <arpa/inet.h>
#include "../common/util/hash_table.h"
#include "../common/util/linked_list.h"
#include "../common/util/atomics.h"
#include "../common/util/inline.h"
#include <pthread.h>
#include <errno.h>
#include <sys/select.h>
#include <sys/mman.h>
#include <endian.h>
#include "../common/jni/de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory.h"
#include "../common/response.h"
#include "../common/shmem_common.h"
#include "../common/util/glibc_wrapper.h"


#define IVSHMEM_PACKET_SIZE sizeof(uint64_t)



/**
 * Magic number for SHMEM FD packet.
 */
const uint64_t MAGIC_NUMBER = 18446744073709551615u;
const uint64_t INTERRUPT_PACKET = 1;

const struct timeval DEFAULT_TIMEOUT = { 2, 0 };


uint64_t FFINLINE convertEndian(uint64_t aVal) {
	if (htonl(1) == 1) {
		//BIG ENDIAN
		return bswap_64(aVal);
	} else {
		return aVal;
	}
}



struct ivshmem_peer_node {
	struct hash_table_node node;
	uint16_t peer_id;
	uint16_t vector_count;
	bool deny_new_vectors;
	//Set to null after converted into array vector_fds
	linked_list* vector_list;
	int* vector_fds;
};

struct ivshmem_peer_node_search {
	struct hash_table_node node;
	uint16_t peer_id;
};

struct ivshmem_vector_node {
	linked_list_node node;
	int vector_fd;
};

struct ivshem_connection {
	struct mapped_shared_memory mapped;
	hash_table peers;
	bool peers_mutex_init;
	pthread_mutex_t peers_mutex;
	uint16_t peer_id;
	int sock_fd;
	int shmem_fd;
	int* vector_fd_highest;
	//Set to null after converted into array.
	linked_list* vector_list;
	uint16_t vector_count;
	int *vector_fds;
};

struct ivshmem_packet {
	int fd;
	union {
		uint64_t number;
		char raw[IVSHMEM_PACKET_SIZE];
	} data;

};

/**
 * Utility Functions.
 */

void deallocPeerVectorList(linked_list_node* node) {
	if (node == NULL) {
		return;
	}

	free(node);
}

void deallocPeerVectorListWithClose(linked_list_node* node) {
	if (node == NULL) {
		return;
	}

	struct ivshmem_vector_node * ptr = (struct ivshmem_vector_node*) node;

	if (ptr->vector_fd != -1) {
		close(ptr->vector_fd);
	}

	free(node);
}

void deallocPeerStructNode(hash_table_node *aNode) {
	if (aNode == NULL) {
		return;
	}

	struct ivshmem_peer_node* peer = (struct ivshmem_peer_node*) aNode;


	if (peer->vector_list != NULL) {
		linked_list_clear(peer->vector_list, &deallocPeerVectorListWithClose);
		free(peer->vector_list);
		peer->vector_list = NULL;
	}


	if (peer->vector_fds != NULL) {
		for (int i = 0; i < peer->vector_count; i++) {
			if (peer->vector_fds[i] != -1) {
				close(peer->vector_fds[i]);
			}
		}
		free(peer->vector_fds);
		peer->vector_fds = NULL;
	}

	free(aNode);
}

bool comparePeerStructNode(hash_table_node *aNode1, hash_table_node *aNode2) {
	if (aNode1 == aNode2) {
		return true;
	}

	if (aNode1 == NULL || aNode2 == NULL) {
		return false;
	}

	struct ivshmem_peer_node *peer1 = (struct ivshmem_peer_node*) aNode1;
	struct ivshmem_peer_node *peer2 = (struct ivshmem_peer_node*) aNode2;

	return peer1->peer_id == peer2->peer_id;
}

int FFINLINE initHashTable(hash_table *table) {
	hash_table_node **tempTable = malloc(sizeof(hash_table_node*) * 32);
	if (tempTable == NULL) {
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}

	hash_table_init(table, tempTable, 32, &comparePeerStructNode);
	return combineErrorCode(RES_OK, 0);
}






uint64_t convertVectorListToArray(linked_list** aVectorList, int** aVectorArray, uint16_t* aVectorArraySize) {
	if (aVectorList == NULL || aVectorArray == NULL || aVectorArraySize == NULL) {
		return RES_ERROR;
	}

	linked_list* tempList = *aVectorList;

	if (tempList == NULL) {
		*aVectorArray = NULL;
		*aVectorArraySize = 0;
		return combineErrorCode(RES_OK, 0);
	}

	if (tempList->size == 0) {
		*aVectorArray = NULL;
		*aVectorArraySize = 0;
		*aVectorList = NULL;
		free(tempList);
		return combineErrorCode(RES_OK, 0);
	}

	int* tempVectors = malloc(sizeof(int) * tempList->size);
	if (tempVectors == NULL) {
		*aVectorArray = NULL;
		*aVectorArraySize = 0;
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}


	linked_list_iterator tempIter;
	linked_list_iterator_ascending(tempList, &tempIter);

	int i = 0;
	while(tempIter.next != NULL) {
		struct ivshmem_vector_node* tempVectorNode = (struct ivshmem_vector_node*) linked_list_iterator_next(&tempIter);
		tempVectors[i] = tempVectorNode->vector_fd;
		i++;
	}

	*aVectorArraySize = tempList->size;
	*aVectorArray = tempVectors;
	linked_list_clear(tempList, &deallocPeerVectorList);

	free(tempList);
	*aVectorList = NULL;
	return combineErrorCode(RES_OK, 0);
}

/**
 * IVSHMEM Client functions
 */

/* read message from the unix socket */
uint64_t readPacket(int sock_fd, struct ivshmem_packet *packet) {
	memset(packet, 0, sizeof(struct ivshmem_packet));

	int ret;
	struct msghdr msg;
	struct iovec iov[1];
	union {
		struct cmsghdr cmsg;
		char control[CMSG_SPACE(sizeof(int))];
	} msg_control;

	iov[0].iov_base = &packet->data.raw[0];
	iov[0].iov_len = IVSHMEM_PACKET_SIZE;

	memset(&msg, 0, sizeof(msg));
	msg.msg_iov = iov;
	msg.msg_iovlen = 1;
	msg.msg_control = &msg_control;
	msg.msg_controllen = sizeof(msg_control);

	ret = recvmsg(sock_fd, &msg, 0);
	if (ret == -1) {
		int tempError = errno;
		if (tempError == EAGAIN || tempError == EWOULDBLOCK) {
			return combineErrorCode(RES_PACKET_TIMEOUT, tempError);
		}

		return combineErrorCode(RES_READ_ERROR, tempError);
	}

	if (ret < IVSHMEM_PACKET_SIZE) {
		return combineErrorCode(RES_PACKET_TOO_SHORT, 0);
	}

	if (ret == 0) {
		return combineErrorCode(RES_READ_ERROR, 0);
	}

	struct cmsghdr *cmsg;
	for (cmsg = CMSG_FIRSTHDR(&msg); cmsg; cmsg = CMSG_NXTHDR(&msg, cmsg)) {

		if (cmsg->cmsg_len != CMSG_LEN(sizeof(int))
				|| cmsg->cmsg_level != SOL_SOCKET
				|| cmsg->cmsg_type != SCM_RIGHTS) {
			continue;
		}

		wrap_memcpy(&packet->fd, CMSG_DATA(cmsg), sizeof(int));
		return combineErrorCode(RES_FD, 0);
	}

	return combineErrorCode(RES_OK, 0);
}

void closeIVSHMEM(struct ivshem_connection *aConnection) {
	if (aConnection == NULL) {
		return;
	}

	if (aConnection->peers_mutex_init) {
		pthread_mutex_destroy(&aConnection->peers_mutex);
		aConnection->peers_mutex_init = false;
	}

	if (aConnection->mapped.memory != NULL) {
		munmap(aConnection->mapped.memory, aConnection->mapped.size);
		aConnection->mapped.memory = NULL;
	}

	if (aConnection->peers.hashTable != NULL) {
		hash_table_clear(&aConnection->peers, &deallocPeerStructNode);
		free(aConnection->peers.hashTable);
		aConnection->peers.hashTable = NULL;
	}

	if (aConnection->shmem_fd != -1) {
		close(aConnection->shmem_fd);
		aConnection->shmem_fd = -1;
	}

	if (aConnection->sock_fd != -1) {
		close(aConnection->sock_fd);
		aConnection->sock_fd = -1;
	}

	if (aConnection->vector_list != NULL) {
		linked_list_clear(aConnection->vector_list, &deallocPeerVectorListWithClose);
		free(aConnection->vector_list);
		aConnection->vector_list = NULL;
	}

	if (aConnection->vector_fds != NULL) {
		for (int i = 0; i < aConnection->vector_count; i++) {
			if (aConnection->vector_fds[i] != -1) {
				close(aConnection->vector_fds[i]);
			}
		}

		free(aConnection->vector_fds);
		aConnection->vector_fds = NULL;
	}

}

uint64_t connectIVSHMEM(struct ivshem_connection *aConnection, char *aDevice) {

	memset(aConnection, 0, sizeof(struct ivshem_connection));
	aConnection->peer_id = -1;
	aConnection->shmem_fd = -1;
	aConnection->sock_fd = -1;

	if (strnlen(aDevice, 109) > 108) {
		return combineErrorCode(RES_INVALID_DEVICE_PATH, 0);
	}

	uint64_t myRes;
	int sysRes;

	aConnection->sock_fd = socket(AF_UNIX, SOCK_STREAM, 0);

	if (aConnection->sock_fd == -1) {
		return combineErrorCode(RES_ERROR_CREATING_UNIX_SOCKET, errno);
	}

	if (setsockopt(aConnection->sock_fd, SOL_SOCKET, SO_RCVTIMEO,
			(char*) &DEFAULT_TIMEOUT, sizeof(DEFAULT_TIMEOUT)) < 0) {
		int err = errno;
		closeIVSHMEM(aConnection);
		return combineErrorCode(RES_ERROR_SETTING_TIMEOUT_ON_UNIX_SOCKET, err);
	}

	struct sockaddr_un sun;
	sun.sun_family = AF_UNIX;
	strcpy(&sun.sun_path[0], aDevice);

	if (connect(aConnection->sock_fd, (struct sockaddr*) &sun, sizeof(sun))
			< 0) {
		int err = errno;
		closeIVSHMEM(aConnection);
		return combineErrorCode(RES_ERROR_CONNECTING_UNIX_SOCKET, err);
	}

	struct ivshmem_packet packet;
	myRes = readPacket(aConnection->sock_fd, &packet);
	if (!checkErrorCode(myRes, RES_OK)) {
		closeIVSHMEM(aConnection);
		return myRes;
	}

	if (packet.data.number != 0) {
		closeIVSHMEM(aConnection);
		return combineErrorCode(RES_UNKNOWN_IVSHMEM_PROTOCOLL_VERSION, 0);
	}

	myRes = readPacket(aConnection->sock_fd, &packet);
	if (!checkErrorCode(myRes, RES_OK)) {
		closeIVSHMEM(aConnection);
		return myRes;
	}

	//Only 16 bits for are provided by the device anything else means fuckup.
	uint64_t tempMyPeerID = convertEndian(packet.data.number);
	if (tempMyPeerID > 0xffff) {
		closeIVSHMEM(aConnection);
		return combineErrorCode(RES_PEER_INVALID, 0);
	}

	aConnection->peer_id = (uint16_t) tempMyPeerID;

	myRes = readPacket(aConnection->sock_fd, &packet);
	if (!checkErrorCode(myRes, RES_FD)) {
		closeIVSHMEM(aConnection);
		if (checkErrorCode(myRes, RES_OK)) {
			return combineErrorCode(RES_FD_MISSING, 0);
		}
		return myRes;
	}

	//Documentation says every byte of the 8 bytes must be -1
	if (convertEndian(packet.data.number) != MAGIC_NUMBER) {
		closeIVSHMEM(aConnection);
		return checkErrorCode(RES_UNEXPECTED_PACKET, 0);
	}

	aConnection->shmem_fd = packet.fd;

	aConnection->vector_count = 0;
	aConnection->vector_fds = NULL;

	aConnection->vector_list = malloc(sizeof(linked_list));

	if (aConnection->vector_list == NULL) {
		closeIVSHMEM(aConnection);
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}

	linked_list_init(aConnection->vector_list);

	myRes = initHashTable(&aConnection->peers);
	if (!checkErrorCode(myRes, RES_OK)) {
		closeIVSHMEM(aConnection);
		return myRes;
	}


	sysRes = pthread_mutex_init(&aConnection->peers_mutex, NULL);
	if (sysRes != 0) {
		int err = errno;
		closeIVSHMEM(aConnection);
		return combineErrorCode(RES_MUTEX_INIT_ERROR, err);
	}
	aConnection->peers_mutex_init = true;

	//Documentation says that first all other currently connected peers come with their respective vectors
	//Followed by our own vector file descriptors.
	uint64_t tempCurrentPeerID = -1;
	struct ivshmem_peer_node *tempCurrentNode = NULL;
	struct linked_list * tempCurrentList = NULL;
	while (true) {
		myRes = readPacket(aConnection->sock_fd, &packet);

		//NO FD => This is a disconnect notification... great a client disconnected while we were connecting...
		if (checkErrorCode(myRes, RES_OK)) {
			uint64_t tempDisconnectedPeer = convertEndian(packet.data.number);

			//Documentation says this is 0xffff is max valid value for peerid.
			if (tempDisconnectedPeer > 0xffff) {
				closeIVSHMEM(aConnection);
				return combineErrorCode(RES_PEER_INVALID, 0);
			}


			if (tempDisconnectedPeer == tempMyPeerID) {
				closeIVSHMEM(aConnection);
				return combineErrorCode(RES_OWN_PEER_CLOSED, 0);
			}

			struct ivshmem_peer_node_search tempSearch;
			tempSearch.node.hash = tempDisconnectedPeer;
			tempSearch.peer_id = tempDisconnectedPeer;

			struct ivshmem_peer_node* tempNode = (struct ivshmem_peer_node*) hash_table_remove(&aConnection->peers, &tempSearch.node);
			if (tempNode == NULL) {
				closeIVSHMEM(aConnection);
				return combineErrorCode(RES_CLOSED_UNKNOWN_PEER, 0);
			}

			deallocPeerStructNode(&tempNode->node);

			//We are 100% done after we received a disconnect notification.
			break;
		}

		if (checkErrorCode(myRes, RES_PACKET_TIMEOUT)) {
			//Once we have not received a packet for a while we are done. The QEMU IVSHMEM DOORBELL protocoll is quite poor in that regard.
			//There is no other way to detect that you are done. Pls fix QEMU guys.
			break;
		}

		if (!checkErrorCode(myRes, RES_FD)) {
			closeIVSHMEM(aConnection);
			return myRes;
		}

		uint64_t tempPacketPeer = convertEndian(packet.data.number);

		//Documentation says this is 0xffff is max valid value for peerid.
		if (tempPacketPeer > 0xffff) {
			closeIVSHMEM(aConnection);
			return combineErrorCode(RES_PEER_INVALID, 0);
		}

		if (tempCurrentPeerID != tempPacketPeer) {
			tempCurrentPeerID = tempPacketPeer;

			if (tempPacketPeer == tempMyPeerID) {
				tempCurrentList = aConnection->vector_list;
				tempCurrentNode = NULL;
			} else {
				tempCurrentNode = malloc(sizeof(struct ivshmem_peer_node));
				if (tempCurrentNode == NULL) {
					closeIVSHMEM(aConnection);
					return combineErrorCode(RES_OUT_OF_MEMORY, 0);
				}

				memset(tempCurrentNode, 0, sizeof(struct ivshmem_peer_node));

				tempCurrentNode->vector_count = 0;
				tempCurrentNode->node.hash = tempCurrentPeerID;
				tempCurrentNode->peer_id = tempCurrentPeerID;


				if (!hash_table_add(&aConnection->peers, &tempCurrentNode->node)) {
					free(tempCurrentNode);
					closeIVSHMEM(aConnection);
					return combineErrorCode(RES_DUPLICATE_PEER, 0);
				}


				tempCurrentNode->vector_list = malloc(sizeof(linked_list));

				if (tempCurrentNode->vector_list == NULL) {
					closeIVSHMEM(aConnection);
					return combineErrorCode(RES_OUT_OF_MEMORY, 0);
				}

				linked_list_init(tempCurrentNode->vector_list);

				tempCurrentList = tempCurrentNode->vector_list;

			}


		}

		struct ivshmem_vector_node* tempVectorNode = malloc(sizeof(struct ivshmem_vector_node));

		if (tempVectorNode == NULL) {
			closeIVSHMEM(aConnection);
			return combineErrorCode(RES_OUT_OF_MEMORY, 0);
		}

		linked_list_node_init(&tempVectorNode->node);

		tempVectorNode->vector_fd = packet.fd;

		linked_list_add_last(tempCurrentList, &tempVectorNode->node);
	}

	myRes = convertVectorListToArray(&aConnection->vector_list, &aConnection->vector_fds, &aConnection->vector_count);

	if (!checkErrorCode(myRes, RES_OK)) {
		closeIVSHMEM(aConnection);
		return myRes;
	}

	hash_table_iterator tempIter;

	hash_table_iterator_init(&aConnection->peers, &tempIter);

	while(hash_table_iterator_has_next(&tempIter)) {
		struct ivshmem_peer_node* tempPeer = (struct ivshmem_peer_node*) hash_table_iterator_next(&tempIter);
		myRes = convertVectorListToArray(&tempPeer->vector_list, &tempPeer->vector_fds, &tempPeer->vector_count);

		if (!checkErrorCode(myRes, RES_OK)) {
			closeIVSHMEM(aConnection);
			return myRes;
		}

		tempPeer->deny_new_vectors = false;
	}

	//Find highest FD for FD_SET
	int highest = -1;
	for (int i = 0; i < aConnection->vector_count; i++) {
		if (aConnection->vector_fds != NULL && aConnection->vector_fds[i] > highest) {
			highest = aConnection->vector_fds[i];
			aConnection->vector_fd_highest = &aConnection->vector_fds[i];
		}
	}

	struct stat shmem_stat;
	memset(&shmem_stat, 0, sizeof(struct stat));
	if (fstat(aConnection->shmem_fd, &shmem_stat) != 0) {
		int err = errno;
		closeIVSHMEM(aConnection);
		return combineErrorCode(RES_ERROR_SHMEM_FSTAT, err);
	}
	aConnection->mapped.size = shmem_stat.st_size;


	aConnection->mapped.memory = mmap(0, aConnection->mapped.size, PROT_READ | PROT_WRITE, MAP_SHARED, aConnection->shmem_fd, 0);
	if (aConnection->mapped.memory == (void*)-1 || aConnection->mapped.memory == NULL) {
		int err = errno;
		aConnection->mapped.memory = 0;
		closeIVSHMEM(aConnection);
		return combineErrorCode(RES_ERROR_SHMEM_MMAP, err);
	}

	uint32_t tempSize = hash_table_get_optimal_length(&aConnection->peers);

	if (tempSize > aConnection->peers.hashTableLenght) {
		hash_table_node** tempNewTable = malloc(sizeof(hash_table_node*) * tempSize);
		if (tempNewTable == NULL) {
			closeIVSHMEM(aConnection);
			return combineErrorCode(RES_OUT_OF_MEMORY, 0);
		}
		hash_table_node** tempOldTable = hash_table_rehash(&aConnection->peers, tempNewTable, tempSize);
		if (tempOldTable ==  NULL) {
			closeIVSHMEM(aConnection);
			return combineErrorCode(RES_ERROR, 0);
		}

		free(tempOldTable);
	}


	return combineErrorCode(RES_OK, 0);
}

uint64_t pollIVSHMEM(struct ivshem_connection *aConnection, uint16_t* aPeer, int32_t* aVector) {
	struct ivshmem_packet packet;
	uint64_t res = readPacket(aConnection->sock_fd, &packet);

	if (!checkErrorCode(res, RES_OK) && !checkErrorCode(res, RES_FD)) {
		if (checkErrorCode(res, RES_PACKET_TIMEOUT)) {
			return combineErrorCode(RES_POLL_SERVER_TIMEOUT, EAGAIN);
		}
		return res;
	}

	uint64_t tempPacketPeer = convertEndian(packet.data.number);

	//Documentation says this is 0xffff is max valid value for peerid.
	if (tempPacketPeer > 0xffff) {
		return combineErrorCode(RES_PEER_INVALID, 0);
	}

	*aPeer = tempPacketPeer;
	*aVector = -1;

	struct ivshmem_peer_node_search search;
	search.node.hash = tempPacketPeer;
	search.peer_id = tempPacketPeer;

	//Disconnect
	if (checkErrorCode(res, RES_OK)) {
		pthread_mutex_lock(&aConnection->peers_mutex);
		struct ivshmem_peer_node* tempNode = (struct ivshmem_peer_node*) hash_table_remove(&aConnection->peers, &search.node);
		if (tempNode == NULL) {
			pthread_mutex_unlock(&aConnection->peers_mutex);
			return combineErrorCode(RES_PEER_DOESNT_EXIST, 0);
		}

		pthread_mutex_unlock(&aConnection->peers_mutex);
		deallocPeerStructNode(&tempNode->node);
		return combineErrorCode(RES_OK, 0);
	}

	//NEWFD
	if (checkErrorCode(res, RES_FD)) {
		pthread_mutex_lock(&aConnection->peers_mutex);
		struct ivshmem_peer_node* tempNode = (struct ivshmem_peer_node*) hash_table_get(&aConnection->peers, &search.node);
		if (tempNode == NULL) {
			tempNode = malloc(sizeof(struct ivshmem_peer_node));
			if (tempNode == NULL) {
				pthread_mutex_unlock(&aConnection->peers_mutex);
				close(packet.fd);
				return combineErrorCode(RES_OUT_OF_MEMORY, 0);
			}

			memset(tempNode, 0, sizeof(struct ivshmem_peer_node));
			tempNode->peer_id = tempPacketPeer;
			tempNode->node.hash = tempPacketPeer;
			tempNode->deny_new_vectors = false;

			if (!hash_table_add(&aConnection->peers, &tempNode->node)) {
				pthread_mutex_unlock(&aConnection->peers_mutex);
				close(packet.fd);
				return combineErrorCode(RES_ERROR, 0);
			}
		}

		if (tempNode->deny_new_vectors) {
			//We might not be Out of Memory now but the peers interrupt vectors cannot be saved because we were OOM in the past.
			pthread_mutex_unlock(&aConnection->peers_mutex);
			close(packet.fd);
			return combineErrorCode(RES_ERROR, 0);
		}

		ssize_t tempSize = (tempNode->vector_count + 1) * sizeof(int);

		int * tempOld = tempNode->vector_fds;
		if (tempOld == NULL && tempNode->vector_count != 0) {
			tempNode->deny_new_vectors = true;
			pthread_mutex_unlock(&aConnection->peers_mutex);
			close(packet.fd);
			return combineErrorCode(RES_ERROR, 0);
		}

		int* tempNewVectors = malloc(tempSize);

		if (tempNewVectors == NULL) {
			//Otherwise would cause alignment issues of interrupt table for future interrupts...
			tempNode->deny_new_vectors = true;
			close(packet.fd);
			pthread_mutex_unlock(&aConnection->peers_mutex);
			return combineErrorCode(RES_OUT_OF_MEMORY, 0);
		}

		if (tempOld != NULL) {
			wrap_memcpy(tempNewVectors, tempOld, (tempNode->vector_count) * sizeof(int));
			free(tempOld);
		}

		tempNewVectors[tempNode->vector_count] = packet.fd;
		tempNode->vector_fds = tempNewVectors;
		tempNode->vector_count++;
		*aVector = tempNode->vector_count;

		pthread_mutex_unlock(&aConnection->peers_mutex);
		return combineErrorCode(RES_OK, 0);
	}

	//CANT HAPPEN
	return res;
}

uint64_t sendInterrupt(struct ivshem_connection *aConnection, uint16_t peer, uint16_t vector) {

	if (aConnection->peer_id == peer) {
		return combineErrorCode(RES_INTERRUPT_CANT_SELF_INTERRUPT, 0);
	}

	struct ivshmem_peer_node_search search;
	search.node.hash = peer;
	search.peer_id = peer;

	pthread_mutex_lock(&aConnection->peers_mutex);
	struct ivshmem_peer_node* tempNode = (struct ivshmem_peer_node*) hash_table_get(&aConnection->peers, (hash_table_node*) &search);

	if (tempNode == NULL) {
		pthread_mutex_unlock(&aConnection->peers_mutex);
		return combineErrorCode(RES_PEER_DOESNT_EXIST, 0);
	}

	if (tempNode->vector_count <= vector) {
		pthread_mutex_unlock(&aConnection->peers_mutex);
		return combineErrorCode(RES_INTERRUPT_VECTOR_TOO_BIG, 0);
	}

	int tempFD = tempNode->vector_fds[vector];

	if (tempFD == -1) {
		pthread_mutex_unlock(&aConnection->peers_mutex);
		return combineErrorCode(RES_INTERRUPT_VECTOR_CLOSED, 0);
	}


	if (write(tempFD, &INTERRUPT_PACKET, IVSHMEM_PACKET_SIZE) != IVSHMEM_PACKET_SIZE) {
		pthread_mutex_unlock(&aConnection->peers_mutex);
		return combineErrorCode(RES_INTERRUPT_SEND_ERROR, errno);
	}

	pthread_mutex_unlock(&aConnection->peers_mutex);
	return combineErrorCode(RES_OK, 0);
}

uint64_t pollInterrupt(struct ivshem_connection *aConnection, uint16_t someVectors[], uint16_t aMaxVectorCount, uint16_t* aVectorCount) {
	if (aConnection->vector_count == 0) {
		return combineErrorCode(RES_INTERRUPT_RECEIVE_NO_VECTORS, 0);
	}

	fd_set tempSet;

	FD_ZERO(&tempSet);

	for (int i = 0; i < aConnection->vector_count; i++) {
		FD_SET(aConnection->vector_fds[i], &tempSet);
	}



	struct timeval tempTimeout = DEFAULT_TIMEOUT;
	int tempResult = select(*aConnection->vector_fd_highest, &tempSet, NULL, NULL, &tempTimeout);

	if (tempResult == 0) {
		return combineErrorCode(RES_INTERRUPT_TIMEOUT, 0);
	}

	if (tempResult < 0) {
		int err = errno;
		if (err == EINTR) {
			return combineErrorCode(RES_INTERRUPT_TIMEOUT, EINTR);
		}
		return combineErrorCode(RES_INTERRUPT_RECEIVE_ERROR, err);
	}

	uint16_t tempVectorCount = 0;
	uint64_t tempInterrupt = 0;
	for (int i = 0; i < aMaxVectorCount; i++) {
		if (!FD_ISSET(aConnection->vector_fds[i], &tempSet)) {
			continue;
		}

		int ret = read(aConnection->vector_fds[i], &tempInterrupt, IVSHMEM_PACKET_SIZE);
		if (ret != IVSHMEM_PACKET_SIZE) {
			continue;
		}
		someVectors[tempVectorCount] = i;
		tempVectorCount++;
	}
	*aVectorCount = tempVectorCount;

	if (tempVectorCount == 0) {
		return combineErrorCode(RES_INTERRUPT_RECEIVE_ERROR,0);
	}

	return combineErrorCode(RES_OK, 0);
}

int main() {
	printf("Hello World\n");
	char* tempDevice = "/tmp/shmemsock";
	struct ivshem_connection tempConnection;
	uint64_t tempRes = connectIVSHMEM(&tempConnection, tempDevice);
	printf("\n");
	printf("\n");
	printf("\n");
	printf("Response=%i\n", getErrorCode(tempRes));
	if (!checkErrorCode(tempRes, RES_OK)) {
		return -1;
	}

	printf("Self:\n");
	printf("ID: %u\n", tempConnection.peer_id);
	printf("SHMEM: %i\n", tempConnection.shmem_fd);
	printf("SHMEM size: %li\n", tempConnection.mapped.size);
	printf("Vector Count: %i\n", tempConnection.vector_count);
	printf("VECTORS: ");
	for (int i = 0; i < tempConnection.vector_count; i++) {
		printf("%i ", tempConnection.vector_fds[i]);
	}
	printf("\n");
	printf("\n");

	hash_table_iterator tempIter;
	hash_table_iterator_init(&tempConnection.peers, &tempIter);

	printf("\n");
	while(hash_table_iterator_has_next(&tempIter)) {
		struct ivshmem_peer_node* tempCurrent = (struct ivshmem_peer_node*) hash_table_iterator_next(&tempIter);
		printf("GOT OTHER PEER\n");
		printf("ID: %u\n", tempCurrent->peer_id);
		printf("VECTORS: ");
		for (int i = 0; i < tempCurrent->vector_count; i++) {
			printf("%i ", tempCurrent->vector_fds[i]);
		}
		printf("\n");
		printf("\n");
	}

	tempRes = sendInterrupt(&tempConnection, 1, 12);
	printf("SEND %i\n", getErrorCode(tempRes));
	uint16_t count = 0;
	uint16_t table[tempConnection.vector_count];

	tempRes = pollInterrupt(&tempConnection, table, tempConnection.vector_count, &count);
	printf("GOT %i\n", getErrorCode(tempRes));
	if (checkErrorCode(tempRes, RES_OK)) {
		printf("Interrupts: ");
		for (int i = 0; i < count; i++) {
			printf("%i ", table[i]);
		}
		printf("\n");
	}

	while(true) {
		printf("POLLING\n");
		int32_t tempVec;
		uint16_t tempPeer;
		tempRes = pollIVSHMEM(&tempConnection, &tempPeer , &tempVec);
		printf("RES %i\n", getErrorCode(tempRes));
		if (!checkErrorCode(tempRes, RES_OK)) {
			return -1;
		}
		printf("RELISTING PEERS\n");
		printf("\n");

		hash_table_iterator_init(&tempConnection.peers, &tempIter);
		while(hash_table_iterator_has_next(&tempIter)) {
			struct ivshmem_peer_node* tempCurrent = (struct ivshmem_peer_node*) hash_table_iterator_next(&tempIter);
			printf("GOT OTHER PEER\n");
			printf("ID: %u\n", tempCurrent->peer_id);
			printf("VECTORS: ");
			for (int i = 0; i < tempCurrent->vector_count; i++) {
				printf("%i ", tempCurrent->vector_fds[i]);
			}
			printf("\n");
			printf("\n");
		}
	}


	closeIVSHMEM(&tempConnection);
	return 0;
}

/*
 * JNI
 */


/*
 * Class:     de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory
 * Method:    openDevice
 * Signature: (Ljava/lang/String;[J)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory_openDevice
  (JNIEnv * env, jclass aClazz, jstring aPath, jlongArray aResult) {
	if (aResult == NULL || aPath == NULL || (*env)->GetArrayLength(env,aResult) != 4) {
		return combineErrorCode(RES_INVALID_ARGUMENTS, 0);
	}

	const char* tempPath = (*env)->GetStringUTFChars(env, aPath, 0);
	if (tempPath == NULL) {
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}

	size_t tempLen = strlen(tempPath);
	char tempPathCopy[tempLen+1];
	wrap_memcpy(tempPathCopy, tempPath, tempLen+1);
	(*env)->ReleaseStringUTFChars(env, aPath, tempPath);

	struct ivshem_connection* tempConnection = malloc(sizeof(struct ivshem_connection));
	if (tempConnection == NULL) {
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}

	uint64_t tempRes = connectIVSHMEM(tempConnection, tempPathCopy);
	if (!checkErrorCode(tempRes, RES_OK)) {
		free(tempConnection);
		return tempRes;
	}

	jlong* tempBuf = (*env)->GetLongArrayElements(env, aResult, NULL);

	tempBuf[0] = (jlong) tempConnection;
	tempBuf[1] = tempConnection->peer_id;
	tempBuf[2] = tempConnection->vector_count;
	tempBuf[3] = tempConnection->mapped.size;

	(*env)->ReleaseLongArrayElements(env, aResult, tempBuf, JNI_OK);

	return combineErrorCode(RES_OK, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory
 * Method:    getPeers
 * Signature: (J[J)[I
 */
JNIEXPORT jintArray JNICALL Java_de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory_getPeers
  (JNIEnv * env, jclass aClazz, jlong aConnectionPtr, jlongArray tempResAr) {

	if (tempResAr == NULL || (*env)->GetArrayLength(env, tempResAr) != 1) {
		return NULL;
	}

	struct ivshem_connection* tempConnection = (struct ivshem_connection*)aConnectionPtr;
	if (tempConnection == NULL) {
		jlong* tempLong = (*env)->GetLongArrayElements(env, tempResAr, 0);
		*tempLong = combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
		(*env)->ReleaseLongArrayElements(env, tempResAr, tempLong, JNI_OK);
		return NULL;
	}


	pthread_mutex_lock(&tempConnection->peers_mutex);
	jintArray tempRet = (*env)->NewIntArray(env, tempConnection->peers.size);
	jint* tempInts = (*env)->GetIntArrayElements(env, tempRet, 0);
	hash_table_iterator tempIter;
	hash_table_iterator_init(&tempConnection->peers, &tempIter);

	for (int i= 0; i < tempConnection->peers.size; i++) {
		struct ivshmem_peer_node * tempPeer = (struct ivshmem_peer_node*) hash_table_iterator_next(&tempIter);
		if (tempPeer == NULL) {
			tempInts[i] = -1;
		} else {
			tempInts[i] = tempPeer->peer_id;
		}
	}
	(*env)->ReleaseIntArrayElements(env, tempRet, tempInts, JNI_OK);

	jlong* tempLong = (*env)->GetLongArrayElements(env, tempResAr, 0);
	*tempLong = combineErrorCode(RES_OK, 0);
	(*env)->ReleaseLongArrayElements(env, tempResAr, tempLong, JNI_OK);
	pthread_mutex_unlock(&tempConnection->peers_mutex);

	return tempRet;
}

/*
 * Class:     de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory
 * Method:    getVectors
 * Signature: (JI[I)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory_getVectors
  (JNIEnv * env, jclass aClazz, jlong aConnectionPtr, jint aPeer, jintArray aResult) {
	if (aResult == NULL || (*env)->GetArrayLength(env,aResult) != 1) {
		return combineErrorCode(RES_INVALID_ARGUMENTS, 0);
	}

	struct ivshem_connection* tempConnection = (struct ivshem_connection*)aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	struct ivshmem_peer_node_search tempSearch;
	tempSearch.node.hash = aPeer;
	tempSearch.peer_id = aPeer;

	pthread_mutex_lock(&tempConnection->peers_mutex);


	struct ivshmem_peer_node* tempNode = (struct ivshmem_peer_node*) hash_table_get(&tempConnection->peers, &tempSearch.node);

	if (tempNode == NULL) {
		pthread_mutex_unlock(&tempConnection->peers_mutex);
		return combineErrorCode(RES_PEER_NOT_FOUND, 0);
	}

	uint16_t tempVectorCount = tempNode->vector_count;
	pthread_mutex_unlock(&tempConnection->peers_mutex);

	jint* tempInt = (*env)->GetIntArrayElements(env, aResult, 0);
	*tempInt = tempVectorCount;
	(*env)->ReleaseIntArrayElements(env, aResult, tempInt, JNI_OK);



	return combineErrorCode(RES_OK, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory
 * Method:    close
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory_close
  (JNIEnv * env, jclass aClazz, jlong aConnectionPtr) {
	struct ivshem_connection* tempConnection = (struct ivshem_connection*)aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	closeIVSHMEM(tempConnection);
	free(tempConnection);

	return combineErrorCode(RES_OK, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory
 * Method:    pollServer
 * Signature: (J[I)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory_pollServer
  (JNIEnv * env, jclass aClazz, jlong aConnectionPtr, jintArray aResult) {
	if (aResult == NULL || (*env)->GetArrayLength(env,aResult) != 2) {
		return combineErrorCode(RES_INVALID_ARGUMENTS, 0);
	}

	struct ivshem_connection* tempConnection = (struct ivshem_connection*)aConnectionPtr;
	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	//-1 means disconnect
	int32_t tempVectors;
	uint16_t tempPeer;

	uint64_t tempRes = pollIVSHMEM(tempConnection, &tempPeer, &tempVectors);

	if (!checkErrorCode(tempRes, RES_OK)) {
		return tempRes;
	}

	jint* tempInt = (*env)->GetIntArrayElements(env, aResult, 0);
	tempInt[0] = tempPeer;
	tempInt[1] = tempVectors;
	(*env)->ReleaseIntArrayElements(env, aResult, tempInt, JNI_OK);

	return combineErrorCode(RES_OK, 0);
}

/*
 * Class:     de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory
 * Method:    sendInterrupt
 * Signature: (JII)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory_sendInterrupt
  (JNIEnv * env, jclass aClazz, jlong aConnectionPtr, jint aPeer, jint aVector) {
	struct ivshem_connection* tempConnection = (struct ivshem_connection*) aConnectionPtr;

	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	return sendInterrupt(tempConnection, (uint16_t) aPeer, (uint16_t) aVector);
}

/*
 * Class:     de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory
 * Method:    pollInterrupt
 * Signature: (J[I)J
 */
JNIEXPORT jlong JNICALL Java_de_aschuetz_ivshmem4j_linux_doorbell_LinuxSharedMemory_pollInterrupt
  (JNIEnv * env, jclass aClazz, jlong aConnectionPtr, jintArray aInterupt) {
	struct ivshem_connection* tempConnection = (struct ivshem_connection*) aConnectionPtr;

	if (tempConnection == NULL) {
		return combineErrorCode(RES_INVALID_CONNECTION_POINTER, 0);
	}

	if (aInterupt == NULL) {
		return combineErrorCode(RES_INVALID_ARGUMENTS, 0);
	}

	jsize tempSize = (*env)->GetArrayLength(env, aInterupt);

	if (tempSize < 2 || tempSize > 0xffff) {
		return combineErrorCode(RES_INVALID_ARGUMENTS, 0);
	}

	uint16_t tempBufSize = tempSize-1;
	uint16_t* tempBuf = malloc(tempBufSize * sizeof(uint16_t));

	if (tempBuf == NULL) {
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}

	uint16_t tempFillCount = 0;
	uint64_t tempRes = pollInterrupt(tempConnection, tempBuf, tempBufSize, &tempFillCount);
	if (!checkErrorCode(tempRes, RES_OK)) {
		free(tempBuf);
		return tempRes;
	}

	void* tempResolvedBuffer = (void*) (*env)->GetPrimitiveArrayCritical(env, aInterupt, 0);
	if (tempResolvedBuffer == NULL) {
		return combineErrorCode(RES_OUT_OF_MEMORY, 0);
	}
	jint* tempTarget = (jint*) tempResolvedBuffer;
	tempTarget[0] = (jint) tempFillCount;
	for (int i = 0; i < tempBufSize && i < tempFillCount; i++) {
		tempTarget[i+1] = (jint) tempBuf[i];
	}
	(*env)->ReleasePrimitiveArrayCritical(env, aInterupt, tempResolvedBuffer, JNI_OK);
	free(tempBuf);
	return combineErrorCode(RES_OK, 0);
}
