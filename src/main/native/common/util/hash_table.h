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


#ifndef HASH_TABLE_H_
#define HASH_TABLE_H_

#include <stdint.h>
#include <stdbool.h>

typedef struct hash_table_node {
	/*
	 * This field must not be touched outside of the node.
	 * It is not evaluated for any node that is passed as parameter.
	 */
	struct hash_table_node* next;
	uint32_t hash;
} hash_table_node;


typedef bool (*hash_table_comparator)(hash_table_node*, hash_table_node*);
typedef void (*hash_table_deallocator)(hash_table_node*);

typedef struct hash_table {
	hash_table_node** hashTable;
	uint32_t hashTableLenght;
	uint32_t size;
	hash_table_comparator comparator;
} hash_table;

typedef struct hash_table_iterator {
	hash_table* table;
	hash_table_node** next;
	hash_table_node** previous;
	uint32_t tableIndex;
} hash_table_iterator;

void hash_table_init(hash_table* table, hash_table_node** hashTable, uint32_t hashTableLenght, hash_table_comparator comparator);

uint32_t hash_table_get_optimal_length(hash_table* table);

hash_table_node** hash_table_rehash(hash_table* table, hash_table_node** hashTable, uint32_t hashTableLenght);

void hash_table_clear(hash_table* table, hash_table_deallocator deallocator);

bool hash_table_add(hash_table* table, hash_table_node* node);

bool hash_table_contains(hash_table* table, hash_table_node* node);

hash_table_node* hash_table_get(hash_table* table, hash_table_node* node);

hash_table_node* hash_table_remove(hash_table* table, hash_table_node* node);

hash_table_node** hash_table_rehash(hash_table* table, hash_table_node** newHashTabe, uint32_t newHashtableLenght);

void hash_table_iterator_init(hash_table* table, hash_table_iterator* iterator);

bool hash_table_iterator_has_next(hash_table_iterator* iterator);

hash_table_node* hash_table_iterator_next(hash_table_iterator* iterator);

hash_table_node* hash_table_iterator_remove(hash_table_iterator* iterator);

#endif /* HASH_TABLE_H_ */
