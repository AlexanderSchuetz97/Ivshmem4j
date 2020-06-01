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


#ifndef LINKED_LIST_H_
#define LINKED_LIST_H_
#include <stdint.h>
#include <stdbool.h>


typedef enum {
	ADDED, REMOVED, NEW
} linked_list_node_state;

typedef struct linked_list_node {
	linked_list_node_state state;
	struct linked_list_node* next;
	struct linked_list_node* previous;

} linked_list_node;

typedef struct linked_list {
	struct linked_list_node* head;
	struct linked_list_node* tail;
	uint32_t size;
} linked_list;

typedef struct linked_list_iterator {
	linked_list* list;
	linked_list_node* next;
	linked_list_node* previous;
	linked_list_node* lastReturned;
} linked_list_iterator;

//Function Pointers.

typedef void (*linked_list_deallocator)(linked_list_node*);

typedef void (*linked_list_consumer)(void *, linked_list_node*);

typedef bool (*linked_list_comparator)(void *, linked_list_node*);

//Functions

/*
 * Initializes a new linked list
 */
void linked_list_init(linked_list* list);

/*
 * Initializes a new linked list node. If you wish to re add a node that was previously removed from a list
 * then you have to call this method again with the node before you can safely add it.
 * You may not call this method with a node that was not removed from the list yet.
 * This method does not touch the "data" section of the node (i.e after the node).
 */
void linked_list_node_init(linked_list_node * node);

//insertion

/*
 * adds a element at the head of the list. returns false if the insertion would cause the list to have 0xffffffff elements.
 */
bool linked_list_add_first(linked_list* list, linked_list_node* node);

/*
 * adds a element at the tail of the list. returns false if the insertion would cause the list to have 0xffffffff elements.
 */
bool linked_list_add_last(linked_list* list, linked_list_node* node);

/*
 * adds a element at the specified index. returns true if sucessfull false if the given index is out of bounds and the node was not inserted.
 * returns false if the insertion would cause the list to have 0xffffffff elements.
 */
bool linked_list_add(linked_list* list, linked_list_node* node, uint32_t index);

//removal

/*
 * removes the element at the given index from the list. returns NULL if the index is out of bounds.
 */
linked_list_node* linked_list_remove(linked_list* list, uint32_t index);

/*
 * removes the first occurance as determined by the comparator from the list. returns NULL if the element is not found.
 */
linked_list_node* linked_list_remove_first_occurance(linked_list* list, linked_list_comparator comparator, void* comparatorData);

/*
 * removes the last occurance as determined by the comparator from the list. returns NULL if the element is not found.
 */
linked_list_node* linked_list_remove_last_occurance(linked_list* list, linked_list_comparator comparator, void* comparatorData);

/*
 * removes all occurances as determined by the comparator from the list. elements have to be destroyed by a given deallocator.
 * returns the number of elements that were removed form the list.
 */
uint32_t linked_list_remove_all_occurances(linked_list* list, linked_list_comparator comparator, void* comparatorData, linked_list_deallocator deallocator);

/*
 * clears the list. All elements have to be destroyed by a given deallocator.
 * Unless both head and tail are NULL, this method has to be called before releasing the memory of
 */
void linked_list_clear(linked_list* list, linked_list_deallocator deallocator);

//retrieval

/*
 * returns the element at the given index or NULL if the index is out of bounds.
 */
linked_list_node* linked_list_get(linked_list* list, uint32_t index);

/*
 * returns the index of the given node. If the node is not part of the given list then 0xffffffff is returned.
 */
uint32_t linked_list_get_node_index(linked_list* list, linked_list_node* node);

/*
 * returns the first index that matches the comparator. If no node is found then 0xffffffff is returned.
 */
uint32_t linked_list_get_index(linked_list* list, linked_list_comparator comparator, void* comparatorData);

//iteration

/*
 * calls the given consumer for every element in the list.
 * The consumer is not allowed to clear the list or remove the element it is currently called with.
 * It may safely remove any other element or insert elements at any point in the list while beeing called.
 */
void linked_list_for_each(linked_list* list, linked_list_consumer consumer, void* consumerData);

/*
 * returns a new iterator that will returns the head of the list by calling "next"
 * the lastIndex in the iterator will hold an undefined value until next has been called at least once.
 * While the iterator is used clear is not allowed to be called.
 * The remove function requires that the element that was last returned by the iterator is not removed.
 * The next/has_next function requires that the element after the last returned element (or the head if next/previous have not been called yet) is not removed.
 * The previous/has_previous function requires that the element prior the the last returned element (or the tail if next/previous have not been called yet) is not removed.
 */
void linked_list_iterator_ascending(linked_list* list, linked_list_iterator* iterator);

/*
 * returns a new iterator that will return the tail of the list by calling "previous"
 * the lastIndex in the iterator will hold an undefined value until previous has been called at least once.
 */
void linked_list_iterator_descending(linked_list* list, linked_list_iterator* iterator);

/*
 * returns the next node.
 */
linked_list_node* linked_list_iterator_next(linked_list_iterator* iterator);

/*
 * returns the previous node.
 */
linked_list_node* linked_list_iterator_previous(linked_list_iterator* iterator);

/*
 * removes the node that was last returned by eiter next or previous from the list.
 * If this method is called more than once in succession it will return NULL and do nothing.
 * Be aware that this method can only be safely called when there is exactly one iterator iterating over the list.
 * will return NULL and not remove the node if called more than once in sucession or
 * if called before either next or previous have been called successfull.
 */
linked_list_node* linked_list_iterator_remove(linked_list_iterator* iterator);

/*
 * adds an element after the element that was last returned by either next or previous.
 * will return false and not insert the node if called more than once in sucession or
 * if called before either next or previous have been called successfull.
 * returns false if the insertion would cause the list to have 0xffffffff elements.
 */
bool linked_list_iterator_add(linked_list_iterator* iterator, linked_list_node* node);


#endif /* SRC_LINKED_LIST_H_ */
