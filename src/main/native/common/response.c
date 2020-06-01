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

#include "util/inline.h"
#include "response.h"

uint64_t FFINLINE combineErrorCode(int aMyCode, int aDetail) {
	union {
		struct {
			int first;
			int second;
		} split;
		uint64_t merged;
	} temp;

	temp.split.first = aDetail;
	temp.split.second = aMyCode;

	return temp.merged;
}

bool FFINLINE checkErrorCode(uint64_t aCombined, int aMyCode) {
	union {
		struct {
			int first;
			int second;
		} split;
		uint64_t merged;
	} temp;

	temp.merged = aCombined;

	return temp.split.second == aMyCode;
}

int FFINLINE getErrorCode(uint64_t aCombined) {
	union {
		struct {
			int first;
			int second;
		} split;
		uint64_t merged;
	} temp;

	temp.merged = aCombined;

	return temp.split.second;
}
