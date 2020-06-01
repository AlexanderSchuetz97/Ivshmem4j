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


#include "hash_table.h"
#ifndef NULL
#define NULL 0
#endif

bool insert(hash_table* table, hash_table_node** base, hash_table_node* node) {
	while (*base != NULL) {
		if (table->comparator(*base, node)) {
			return false;
		}
		base = &((*base)->next);
	}

	*base = node;
	return true;
}

void hash_table_init(hash_table* table, hash_table_node** hashTable, uint32_t hashTableLenght, hash_table_comparator comparator) {
	for (uint32_t i = 0; i < hashTableLenght; i++) {
		hashTable[i] = NULL;
	}

	table->size = 0;

	table->comparator = comparator;

	table->hashTable = hashTable;
	table->hashTableLenght = hashTableLenght;
}

uint32_t hash_table_get_optimal_length(hash_table* table) {
	if (table == NULL) {
		return 32;
	}

	if (table->hashTableLenght < 32) {
		return 32;
	}

	return (uint32_t) (((float)table->size / 0.75f) + 1.0f);
}

hash_table_node** hash_table_rehash(hash_table* table, hash_table_node** hashTable, uint32_t hashTableLenght) {
	if (hashTableLenght == 0) {
		return NULL;
	}

	for (uint32_t i = 0; i < hashTableLenght; i++) {
		hashTable[i] = NULL;
	}

	hash_table_node** oldtable = table->hashTable;

	for (uint32_t i = 0; i < table->hashTableLenght; i++) {
		hash_table_node* tempCurrent = table->hashTable[i];

		while (tempCurrent != NULL) {
			insert(table, &hashTable[tempCurrent->hash % hashTableLenght], tempCurrent);
			hash_table_node* tempRehashed = tempCurrent;
			tempCurrent = tempCurrent->next;
			tempRehashed->next = NULL;
		}
	}

	table->hashTable = hashTable;
	table->hashTableLenght = hashTableLenght;

	return oldtable;
}

bool hash_table_add(hash_table* table, hash_table_node* node) {
	if (table->hashTableLenght == 0) {
		return false;
	}

	node->next = NULL;

	bool tempSuccess = insert(table, &table->hashTable[node->hash % table->hashTableLenght], node);

	if (tempSuccess) {
		table->size++;
	}

	return tempSuccess;
}

bool hash_table_contains(hash_table* table, hash_table_node* node) {
	hash_table_node* tempCurrent = table->hashTable[node->hash % table->hashTableLenght];
	while(tempCurrent != NULL) {
		if (table->comparator(tempCurrent, node)) {
			return true;
		}

		tempCurrent = tempCurrent->next;
	}

	return false;
}

hash_table_node* hash_table_get(hash_table* table, hash_table_node* node) {
	hash_table_node* tempCurrent = table->hashTable[node->hash % table->hashTableLenght];
	while(tempCurrent != NULL) {
		if (table->comparator(tempCurrent, node)) {
			return tempCurrent;
		}

		tempCurrent = tempCurrent->next;
	}

	return NULL;
}

hash_table_node* hash_table_remove(hash_table* table, hash_table_node* node) {
	hash_table_node** tempCurrent = &table->hashTable[node->hash % table->hashTableLenght];

	while (*tempCurrent != NULL) {
		if (!table->comparator(*tempCurrent, node)) {
			tempCurrent = &(*tempCurrent)->next;
			continue;
		}

		hash_table_node* toRemove = *tempCurrent;

		*tempCurrent = toRemove->next;
		toRemove->next = NULL;
		table->size--;
		return toRemove;
	}

	return NULL;
}

void hash_table_clear(hash_table* table, hash_table_deallocator deallocator) {
	table->size = 0;
	for (uint32_t i = 0; i < table->hashTableLenght; i++) {
		hash_table_node* current = table->hashTable[i];
		if (current == NULL) {
			continue;
		}

		table->hashTable[i] = NULL;

		while(current != NULL) {
			hash_table_node* next = current->next;

			deallocator(current);

			current = next;
		}
	}
}

void hash_table_iterator_init(hash_table* table, hash_table_iterator* iterator) {
	iterator->table = table;
	iterator->next = NULL;
	iterator->tableIndex = -1;
	iterator->previous = NULL;

	for (uint32_t i = 0; i < table->hashTableLenght; i++) {
		if (table->hashTable[i] == NULL) {
			continue;
		}

		iterator->next = &table->hashTable[i];
		iterator->tableIndex = i;
		break;
	}
}

hash_table_node* hash_table_iterator_next(hash_table_iterator* iterator) {
	hash_table_node** next = iterator->next;
	if (next == NULL) {
		iterator->previous = NULL;
		return NULL;
	}

	hash_table_node* nextNode = *next;

	if (nextNode == NULL) {

		iterator->next = NULL;
		iterator->previous = NULL;
		return NULL;
	}

	iterator->previous = next;
	if (nextNode->next != NULL) {
		iterator->next = &nextNode->next;
		return nextNode;
	}

	hash_table* table = iterator->table;
	for (uint32_t i = iterator->tableIndex+1; i < table->hashTableLenght; i++) {
		if (table->hashTable[i] == NULL) {
			continue;
		}

		iterator->next = &table->hashTable[i];
		iterator->tableIndex = i;
		return nextNode;
	}

	iterator->next = NULL;
	return nextNode;
}

bool hash_table_iterator_has_next(hash_table_iterator* iterator) {
	hash_table_node** next = iterator->next;
	if (next == NULL) {
		return false;
	}

	if (*next == NULL) {
		return false;
	}

	return true;
}

hash_table_node* hash_table_iterator_remove(hash_table_iterator* iterator) {
	if (iterator->previous == NULL) {
		return NULL;
	}

	hash_table_node* toRemove = *iterator->previous;

	if (toRemove == NULL) {
		iterator->previous = NULL;
		return NULL;
	}

	*iterator->previous = toRemove->next;
	iterator->previous = NULL;
	iterator->table->size--;

	return toRemove;
}
