//
// Copyright Alexander Schütz, 2020-2022
//
// This file is part of Ivshmem4j.
//
// Ivshmem4j is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// Ivshmem4j is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// A copy of the GNU Lesser General Public License should be provided
// in the COPYING & COPYING.LESSER files in top level directory of Ivshmem4j.
// If not, see <https://www.gnu.org/licenses/>.
//

package io.github.alexanderschuetz97.ivshmem4j.impl;

public class StdErrHandler implements Thread.UncaughtExceptionHandler {

    public static final StdErrHandler INSTANCE = new StdErrHandler();

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (e != null) {
            e.printStackTrace();
        }
    }
}
