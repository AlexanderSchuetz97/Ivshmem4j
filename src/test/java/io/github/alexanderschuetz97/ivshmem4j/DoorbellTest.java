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

package io.github.alexanderschuetz97.ivshmem4j;

import io.github.alexanderschuetz97.ivshmem4j.api.InterruptServiceRoutine;
import io.github.alexanderschuetz97.ivshmem4j.api.Ivshmem;
import io.github.alexanderschuetz97.ivshmem4j.api.IvshmemMemory;
import io.github.alexanderschuetz97.nativeutils.api.NativeMemory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class DoorbellTest {


    private Process server;

    private String path;

    @Before
    public void before() throws IOException {
        path = "/tmp/doorbelltest" + Math.abs(new Random().nextInt());
        server = new ProcessBuilder("ivshmem-server", "-F", "-v", "-S", path, "-l", "1M", "-n", "32").start();
    }

    @After
    public void after() {
        server.destroy();
    }

    @Test
    public void test() throws InterruptedException {
        IvshmemMemory sharedMemory = Ivshmem.doorbell(path, 1000);
        NativeMemory memory = sharedMemory.getMemory();
        memory.write(0, (int) 1);
        Assert.assertEquals(1, memory.readInt(0));
        Assert.assertEquals(32, sharedMemory.getOwnVectors());

        IvshmemMemory sharedMemory2 = Ivshmem.doorbell(path, 1000);
        NativeMemory memory2 = sharedMemory2.getMemory();
        Assert.assertEquals(1, memory2.readInt(0));
        Assert.assertEquals(32, sharedMemory2.getOwnVectors());

        final AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        final Object mutex = new Object();
        sharedMemory.registerInterruptServiceRoutine(0, new InterruptServiceRoutine() {
            @Override
            public void onInterrupt(int aInterrupt) {
                atomicBoolean.set(true);
                synchronized (mutex) {
                    mutex.notifyAll();
                }
            }
        });

        synchronized (mutex) {
            sharedMemory2.sendInterrupt(sharedMemory.getOwnPeerID(), 0);
            mutex.wait(1000);
        }

        Assert.assertTrue(atomicBoolean.get());

        sharedMemory.close();
        sharedMemory2.close();
    }

}
