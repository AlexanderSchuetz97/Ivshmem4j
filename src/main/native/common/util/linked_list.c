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

#include "linked_list.h"
#ifndef NULL
#define NULL 0
#endif
#define MAX_SIZE 0xffffffff
#define MAX_SIZE_ALMOST 0xfffffffd

//Internals
/*
 * removes a node from the list.
 */
void remove_node_internal(linked_list* list, linked_list_node* node) {
	if (node == list->head) {
		list->head = node->next;
	} else {
		node->previous->next = node->next;
	}

	if (node == list->tail) {
		list->tail = node->previous;
	} else {
		node->next->previous = node->previous;
	}

	node->state = REMOVED;
}

//Externals.

void linked_list_init(linked_list* list) {
	list->head = NULL;
	list->tail = NULL;
	list->size = 0;
}

void linked_list_node_init(linked_list_node * node) {
	node->state = NEW;
	node->next = NULL;
	node->previous = NULL;
}


bool linked_list_add_first(linked_list* list, linked_list_node* node) {
	if (node->state == ADDED) {
		return false;
	}

	if (list->head == NULL) {

		if (node->state == REMOVED) {
			node->next = NULL;
			node->previous = NULL;
		}

		list->head = node;
		list->tail = node;
		list->size = 1;
		node->state = ADDED;
		return true;
	}

	uint32_t newSize = list->size+1;
	if (newSize == MAX_SIZE) {
		return false;
	}

	list->size = newSize;
	node->next = list->head;
	list->head->previous = node;
	list->head = node;
	if (node->state == REMOVED) {
		node->previous = NULL;
	}
	node->state = ADDED;

	return true;
}


bool linked_list_add_last(linked_list* list, linked_list_node* node) {
	if (node->state == ADDED) {
		return false;
	}

	if (list->head == NULL) {
		if (node->state == REMOVED) {
			node->next = NULL;
			node->previous = NULL;
		}

		list->head = node;
		list->tail = node;
		list->size = 1;
		node->state = ADDED;
		return true;
	}

	uint32_t newSize = list->size+1;
	if (newSize == MAX_SIZE) {
		return false;
	}

	list->size = newSize;
	node->previous = list->tail;
	list->tail->next = node;
	list->tail = node;
	if (node->state == REMOVED) {
		node->next = NULL;
	}
	node->state = ADDED;
	return true;
}

bool linked_list_add(linked_list* list, linked_list_node* node, uint32_t index) {
	if (list->size == MAX_SIZE_ALMOST) {
		return false;
	}

	if (index > list->size) {
		return false;
	}

	if (index == 0) {
		return linked_list_add_first(list, node);
	}

	if (index == list->size) {
		return linked_list_add_last(list, node);
	}

	if (node->state == ADDED) {
		return false;
	}

	//Is it faster to iterate backwards or forwards?
	if (list->size / 2 > index) {
		//It is faster forwards
		linked_list_node* current = list->head;
		//Offset by +1 because we will insert AFTER the current node.
		uint32_t currentIndex = 1;

		while(current != NULL && currentIndex < index) {
			currentIndex++;
			current = current->next;
		}

		if (current == NULL) {
			//The list is broken. This shouldn't happen.
			//Something messed with the list externally.
			return false;
		}

		node->next = current->next;
		node->previous = current;
		if (current->next != NULL) {
			current->next->previous = node;
		}
		current->next = node;
		node->state = ADDED;
		list->size++;
		return true;

	} else  {
		//It is faster to iterate backwards

		linked_list_node* current = list->tail;
		uint32_t currentIndex = list->size-1;

		while(current != NULL && currentIndex >= index) {
			currentIndex--;
			current = current->previous;
		}

		if (current == NULL) {
			//The list is broken. This shouldn't happen.
			//Something messed with the list externally.
			return false;
		}

		node->next = current->next;
		node->previous = current;
		if (current->next != NULL) {
			current->next->previous = node;
		}
		current->next = node;
		node->state = ADDED;
		list->size++;
		return true;
	}

	return true;
}

linked_list_node* linked_list_remove(linked_list* list, uint32_t index) {
	linked_list_node* ele = linked_list_get(list, index);
	if (ele == NULL) {
		return NULL;
	}

	remove_node_internal(list, ele);
	list->size--;
	return ele;
}


linked_list_node* linked_list_remove_first_occurance(linked_list* list, linked_list_comparator comparator, void* comparatorData) {
	linked_list_node* current = list->head;

	while(current != NULL) {
		if (comparator(comparatorData, current)) {
			remove_node_internal(list, current);
			list->size--;
			return current;
		}

		current = current->next;
	}

	return NULL;
}

linked_list_node* linked_list_remove_last_occurance(linked_list* list, linked_list_comparator comparator, void* comparatorData) {
	linked_list_node* current = list->tail;

	while(current != NULL) {
		if (comparator(comparatorData, current)) {
			remove_node_internal(list, current);
			list->size--;
			return current;
		}

		current = current->previous;
	}

	return NULL;
}

uint32_t linked_list_remove_all_occurances(linked_list* list, linked_list_comparator comparator, void* comparatorData, linked_list_deallocator deallocator) {
	uint32_t removed = 0;
	linked_list_node* current = list->head;

	while(current != NULL) {
		if (comparator(comparatorData, current)) {
			linked_list_node* toRemove = current;
			current = current->next;
			remove_node_internal(list, toRemove);
			deallocator(toRemove);
			removed++;
			list->size--;
			continue;
		}

		current = current->next;
	}

	return removed;
}

void linked_list_clear(linked_list* list, linked_list_deallocator deallocator) {
	linked_list_node* current = list->head;

	while(current != NULL) {
		linked_list_node* toRemove = current;
		current = current->next;
		toRemove->state = REMOVED;
		deallocator(toRemove);
	}

	list->head = NULL;
	list->size = 0;
	list->tail = NULL;
}


linked_list_node* linked_list_get(linked_list* list, uint32_t index) {
	if (index >= list->size) {
		return NULL;
	}

	//Is it faster to iterate backwards or forwards?
	if (list->size / 2 > index) {
		//It is faster forwards
		linked_list_node* current = list->head;
		uint32_t currentIndex = 0;

		while(current != NULL && currentIndex != index) {
			current = current->next;
			currentIndex++;
		}

		if (current == NULL) {
			//The list is broken. This shouldn't happen.
			//Something messed with the list externally.
			return NULL;
		}

		return current;
	} else {
		//It is faster backwards.
		linked_list_node* current = list->head;
		uint32_t currentIndex = list->size-1;

		while(current != NULL && currentIndex != index) {
			current = current->previous;
			currentIndex--;
		}

		if (current == NULL) {
			//The list is broken. This shouldn't happen.
			//Something messed with the list externally.
			return NULL;
		}

		return current;
	}
}

/*
 * returns the index of the given node. If the node is not part of the given list then 0xffffffff is returned.
 */
uint32_t linked_list_get_index(linked_list* list, linked_list_comparator comparator, void* comparatorData) {
	uint32_t currentIndex = 0;
	linked_list_node* current = list->head;

	while(current != NULL) {
		if (comparator(comparatorData, current)) {
			return currentIndex;
		}
		currentIndex++;
		current = current->next;
	}

	return MAX_SIZE;
}




uint32_t linked_list_get_node_index(linked_list* list, linked_list_node* node) {
	if(node->state != ADDED) {
		return MAX_SIZE;
	}

	uint32_t currentIndex = 0;
	linked_list_node* current = list->head;

	while(current != NULL) {
		if (current == node) {
			return currentIndex;
		}
		currentIndex++;
		current = current->next;
	}

	return MAX_SIZE;
}


void linked_list_for_each(linked_list* list, linked_list_consumer consumer, void* consumerData) {
	linked_list_node* current = list->head;

	while(current != NULL) {
		consumer(consumerData, current);
		current = current->next;
	}
}


void linked_list_iterator_ascending(linked_list* list, linked_list_iterator* iterator) {
	iterator->list = list;
	iterator->lastReturned = NULL;
	iterator->next = list->head;
	iterator->previous = NULL;
}


void linked_list_iterator_descending(linked_list* list, linked_list_iterator* iterator) {
	iterator->list = list;
	iterator->lastReturned = NULL;
	iterator->next = NULL;
	iterator->previous = list->tail;
}


linked_list_node* linked_list_iterator_next(linked_list_iterator* iterator) {
	linked_list_node* next = iterator->next;
	if (next == NULL) {
		iterator->lastReturned = NULL;
		iterator->previous = NULL;
		return NULL;
	}
	iterator->lastReturned = next;
	iterator->next = next->next;
	iterator->previous = next;
	return next;
}


linked_list_node* linked_list_iterator_previous(linked_list_iterator* iterator) {
	linked_list_node* prev = iterator->previous;
	if (prev == NULL) {
		iterator->next = NULL;
		iterator->lastReturned = NULL;
		return NULL;
	}
	iterator->lastReturned = prev;
	iterator->next = prev;
	iterator->previous = prev->previous;
	return prev;
}


linked_list_node* linked_list_iterator_remove(linked_list_iterator* iterator) {
	linked_list_node* toRM = iterator->lastReturned;
	if (toRM == NULL) {
		return NULL;
	}

	iterator->lastReturned = NULL;
	if (iterator->next == toRM) {
		iterator->next = toRM->next;
	}

	if (iterator->previous == toRM) {
		iterator->previous = toRM->previous;
	}

	remove_node_internal(iterator->list, toRM);

	iterator->list->size--;

	return toRM;
}


bool linked_list_iterator_add(linked_list_iterator* iterator, linked_list_node* node) {
	linked_list_node* toAddAfter = iterator->lastReturned;
	if( node->state == ADDED) {
		return false;
	}

	if (toAddAfter == NULL) {
		return false;
	}

	iterator->lastReturned = NULL;

	if (iterator->list->size == MAX_SIZE_ALMOST) {
		return false;
	}

	iterator->list->size++;
	node->state = ADDED;
	node->previous = toAddAfter;
	node->next = toAddAfter->next;
	toAddAfter->next = node;

	if (toAddAfter == iterator->list->tail) {
		iterator->list->tail = node;
	}

	return true;
}
