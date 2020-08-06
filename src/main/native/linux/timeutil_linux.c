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
#include "../common/util/timeutil.h"
#include <time.h>
#include <stdbool.h>
#include <errno.h>

#define NANOSECONDS_PER_MILISECOND 1000000

/**
 * This is for some reason much easier on windows...
 */
void sleepMillis(uint64_t aMillis) {
	uint64_t tempMillisWithoutSeconds = aMillis % 1000;
	struct timespec tempTime;
	tempTime.tv_sec = (aMillis - (tempMillisWithoutSeconds)) / 1000;
	tempTime.tv_nsec = tempMillisWithoutSeconds * NANOSECONDS_PER_MILISECOND;

	struct timespec tempRem;
	while(true) {
		if (nanosleep(&tempTime, &tempRem) == 0) {
			return;
		}

		int err = errno;

		if (err == EINTR) {
			tempTime.tv_sec = tempRem.tv_sec;
			tempTime.tv_nsec = tempRem.tv_nsec;
			continue;
		}

		return;
	}

}
