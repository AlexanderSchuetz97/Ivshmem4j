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

import io.github.alexanderschuetz97.ivshmem4j.api.Ivshmem;
import io.github.alexanderschuetz97.ivshmem4j.api.IvshmemMemory;
import io.github.alexanderschuetz97.nativeutils.api.NativeMemory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BasicSharedMemoryTest {

    private Random rng = new Random();

    private File shmemfile;

    private IvshmemMemory shmemory;

    private NativeMemory memory;

    private ExecutorService executor;

    private static boolean i386 = false;

    @Before
    public void before() throws Throwable {
        i386 = "i386".equalsIgnoreCase(System.getProperty("os.arch"));
        rng.setSeed(System.currentTimeMillis());
        shmemfile = new File("/dev/shm/" + getClass().getSimpleName() + Math.abs(rng.nextInt()));
        if (shmemfile.exists()) {
            shmemfile.delete();
        }
        shmemory = Ivshmem.plain(shmemfile.getAbsolutePath(), 4096);
        shmemfile.deleteOnExit();
        rng.setSeed(0);
        executor = Executors.newCachedThreadPool();

        memory = shmemory.getMemory();
        for (long i = 0; i < memory.size(); i++) {
            memory.write(i, (byte) 0);
        }
    }

    @After
    public void after() {
        if (!shmemory.isClosed()) {
            shmemory.close();
        }
        shmemfile.delete();
        executor.shutdownNow();
    }

    @Test
    public void testSingleByteWriteAndRead() throws Throwable {
        ArrayList<Integer> tempInt = new ArrayList<>(0xff);
        for (int i = 0; i < 0xff; i++) {
            tempInt.add(i);
        }

        Collections.shuffle(tempInt);

        for (int value : tempInt) {
            for (long i = 0; i < memory.size(); i++) {
                memory.write(i, (byte) value);
                Assert.assertEquals((byte) value, memory.read(i));
                Assert.assertEquals(value, memory.readUnsignedByte(i));

            }
        }
    }

    @Test
    public void testSingleShortWriteAndRead() throws Throwable {
        ArrayList<Integer> tempInt = new ArrayList<>(0xffff);
        for (int i = 0; i < 0xffff; i++) {
            tempInt.add(i);
        }

        Collections.shuffle(tempInt);

        for (int value : tempInt) {
            for (long i = 0; i + 1 < memory.size(); i++) {
                memory.write(i, (short) value);
                Assert.assertEquals((short) value, memory.readShort(i));
                Assert.assertEquals(value, memory.readUnsignedShort(i));
            }
        }
    }

    @Test
    public void testSingleIntWriteAndRead() throws Throwable {
        ArrayList<Integer> tempInt = new ArrayList<>(0xffff);
        for (int i = 0; i < 0xffff; i++) {
            tempInt.add(rng.nextInt());
        }

        Collections.shuffle(tempInt);

        for (int value : tempInt) {
            for (long i = 0; i + 3 < memory.size(); i++) {
                memory.write(i, value);
                Assert.assertEquals(value, memory.readInt(i));
            }
        }
    }

    @Test
    public void testSingleLongWriteAndRead() throws Throwable {
        ArrayList<Long> tempInt = new ArrayList<>(0xffff);
        for (int i = 0; i < 0xffff; i++) {
            tempInt.add(rng.nextLong());
        }

        Collections.shuffle(tempInt);

        for (long value : tempInt) {
            for (long i = 0; i + 7 < memory.size(); i++) {
                //System.out.println(i);
                memory.write(i, value);
                Assert.assertEquals(value, memory.readLong(i));
            }
        }
    }

    @Test
    public void xadd1Byte() throws Throwable {
        for (long i = 0; i < memory.size(); i++) {
            memory.write(i, (byte) 0);
            for (int j = 0; j <= 0xff; j++) {
                byte aByte = memory.getAndAdd(i, (byte) 1);
                Assert.assertEquals(j, aByte & 0xFF);
            }
            memory.write(i, (byte) 0);
            for (int j = 0; j <= 0xff; j++) {
                byte aByte = memory.getAndAdd(i, (byte) -1);
                Assert.assertEquals((byte) -j, aByte);
            }

            memory.write(i, Byte.MAX_VALUE);
            memory.getAndAdd(i, (byte) 1);
            Assert.assertEquals(Byte.MIN_VALUE, memory.read(i));
            memory.getAndAdd(i, (byte) -1);
            Assert.assertEquals(Byte.MAX_VALUE, memory.read(i));
        }
    }

    @Test
    public void xadd2Byte() throws Throwable {
        for (long i = 0; i + 1 < memory.size(); i++) {
            memory.write(i, (short) 0);
            for (int j = 0; j <= 0xffff; j++) {
                short aByte = memory.getAndAdd(i, (short) 1);
                Assert.assertEquals(j, aByte & 0xFFFF);
            }
            memory.write(i, (short) 0);
            for (int j = 0; j <= 0xffff; j++) {
                short aByte = memory.getAndAdd(i, (short) -1);
                Assert.assertEquals((short) -j, aByte);
            }

            memory.write(i, Short.MAX_VALUE);
            memory.getAndAdd(i, (short) 1);
            Assert.assertEquals(Short.MIN_VALUE, memory.readShort(i));
            memory.getAndAdd(i, (short) -1);
            Assert.assertEquals(Short.MAX_VALUE, memory.readShort(i));
        }
    }

    @Test
    public void xadd4Byte() throws Throwable {
        for (long i = 0; i + 3 < memory.size(); i++) {
            memory.write(i, 0);
            for (int j = 0; j <= 0xffff; j++) {
                int aByte = memory.getAndAdd(i, 1);
                Assert.assertEquals(j, aByte & 0xFFFF);
            }
            memory.write(i, 0);
            for (int j = 0; j <= 0xffff; j++) {
                int aByte = memory.getAndAdd(i, -1);
                Assert.assertEquals(-j, aByte);
            }
            memory.write(i, Integer.MAX_VALUE);
            memory.getAndAdd(i, 1);
            Assert.assertEquals(Integer.MIN_VALUE, memory.readInt(i));
            memory.getAndAdd(i, -1);
            Assert.assertEquals(Integer.MAX_VALUE, memory.readInt(i));
        }
    }

    @Test
    public void xadd8Byte() throws Throwable {
        int inc = 1;

        if (i386) {
            inc = 8;
        }

        for (long i = 0; i + 7 < memory.size(); i += inc) {
            memory.write(i, (long) 0);
            for (long j = 0; j <= 0xffff; j++) {
                long aByte = memory.getAndAdd(i, (long) 1);
                Assert.assertEquals(j, aByte & 0xFFFF);
            }
            memory.write(i, (long) 0);
            for (long j = 0; j <= 0xffff; j++) {
                long aByte = memory.getAndAdd(i, (long) -1);
                Assert.assertEquals(-j, aByte);
            }
            memory.write(i, Long.MAX_VALUE);
            memory.getAndAdd(i, (long) 1);
            Assert.assertEquals(Long.MIN_VALUE, memory.readLong(i));
            memory.getAndAdd(i, (long) -1);
            Assert.assertEquals(Long.MAX_VALUE, memory.readLong(i));
        }
    }

    @Test
    public void cmpxchg1b() throws Throwable {
        for (long i = 0; i < memory.size(); i++) {
            for (int j = 0; j < 0xff; j++) {
                Assert.assertTrue(memory.compareAndSet(i, (byte) j, (byte) (j + 1)));
                Assert.assertFalse(memory.compareAndSet(i, (byte) j, (byte) (j + 2)));
            }
        }
    }

    @Test
    public void cmpxchg2b() throws Throwable {
        for (long i = 0; i + 1 < memory.size(); i++) {
            memory.write(i, (short) 0);
            for (int j = 0; j < 0xffff; j++) {
                Assert.assertTrue(memory.compareAndSet(i, (short) j, (short) (j + 1)));
                Assert.assertFalse(memory.compareAndSet(i, (short) j, (short) (j + 2)));
            }
        }
    }

    @Test
    public void cmpxchg4b() throws Throwable {
        for (long i = 0; i + 3 < memory.size(); i++) {
            memory.write(i, 0);
            for (int j = 0; j < 0xffff; j++) {
                Assert.assertTrue(memory.compareAndSet(i, j, j + 1));
                Assert.assertFalse(memory.compareAndSet(i, j, j + 2));

                if (j != -j) {
                    Assert.assertTrue(memory.compareAndSet(i, j + 1, -j));
                    Assert.assertFalse(memory.compareAndSet(i, j, j + 1));
                    Assert.assertTrue(memory.compareAndSet(i, -j, j + 1));
                }
            }
        }
    }

    @Test
    public void cmpxchg8b() throws Throwable {
        int inc = 1;
        if (i386) {
            inc = 8;
        }

        for (long i = 0; i + 7 < memory.size(); i += inc) {
            memory.write(i, (long) 0);
            for (long j = 0; j < 0xffff; j++) {
                Assert.assertTrue(memory.compareAndSet(i, j, j + 1));
                Assert.assertFalse(memory.compareAndSet(i, j, j + 2));

                if (j != -j) {
                    Assert.assertTrue(memory.compareAndSet(i, j + 1, -j));
                    Assert.assertFalse(memory.compareAndSet(i, j, j + 1));
                    Assert.assertTrue(memory.compareAndSet(i, -j, j + 1));
                }
            }
        }
    }

    @Test
    public void compxchg16b() throws Throwable {

        byte[] buf = new byte[16];
        byte[] buf2 = new byte[16];
        byte[] buf3 = new byte[32];

        if (i386) {
            try {
                memory.compareAndSet(0, buf3);
                Assert.fail();
            } catch (UnsupportedOperationException exc) {
            }
            return;
        }


        for (long i = 0; i + 15 < memory.size(); i += 16) {
            rng.nextBytes(buf);
            rng.nextBytes(buf2);
            System.arraycopy(buf, 0, buf3, 0, buf.length);
            System.arraycopy(buf2, 0, buf3, buf.length, buf2.length);
            memory.write(i, buf);


            Assert.assertTrue(memory.compareAndSet(i, buf3));

            memory.read(i, buf, 0, buf.length);
            Assert.assertTrue(Arrays.equals(buf, buf2));
        }
    }

    @Test
    public void testI386missalignment() {
        if (!i386) {
            return;
        }

        try {
            memory.getAndSet(1, (long) 1);
            Assert.fail();
        } catch (IllegalArgumentException e) {
        }


        try {
            memory.compareAndSet(1, (long) 1, (long) 1);
            Assert.fail();
        } catch (IllegalArgumentException e) {
        }


        try {
            memory.getAndAdd(1, (long) 1);
            Assert.fail();
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void xchg8b() throws Throwable {
        int increment = 1;

        if (i386) {
            increment = 8;
        }

        for (long i = 0; i + 7 < memory.size(); i += increment) {
            long tempBase = rng.nextLong();
            long tempSet = rng.nextLong();
            memory.write(i, tempBase);
            Assert.assertEquals(tempBase, memory.getAndSet(i, tempSet));
            Assert.assertEquals(tempSet, memory.readLong(i));
        }
    }


    @Test
    public void xchg4b() throws Throwable {
        for (long i = 0; i + 7 < memory.size(); i++) {
            int tempBase = rng.nextInt();
            int tempSet = rng.nextInt();
            memory.write(i, tempBase);
            Assert.assertEquals(tempBase, memory.getAndSet(i, tempSet));
            Assert.assertEquals(tempSet, memory.readInt(i));
        }
    }

    @Test
    public void xchg2b() throws Throwable {
        for (long i = 0; i + 7 < memory.size(); i++) {
            short tempBase = (short) rng.nextInt();
            short tempSet = (short) rng.nextInt();
            memory.write(i, tempBase);
            Assert.assertEquals(tempBase, memory.getAndSet(i, tempSet));
            Assert.assertEquals(tempSet, memory.readShort(i));
        }
    }

    @Test
    public void xchg1b() throws Throwable {
        for (long i = 0; i + 7 < memory.size(); i++) {
            byte tempBase = (byte) rng.nextInt();
            byte tempSet = (byte) rng.nextInt();
            memory.write(i, tempBase);
            Assert.assertEquals(tempBase, memory.getAndSet(i, tempSet));
            Assert.assertEquals(tempSet, memory.read(i));
        }
    }

    @Test
    public void testWriteAndReadByteArrays() throws Throwable {
        int size = (int) memory.size() / 8;

        byte[] buf = new byte[size];
        byte[] buf2 = new byte[size];

        for (int i = 0; i < 8; i++) {
            rng.nextBytes(buf);
            memory.write(i * size, buf, 0, size);
            memory.read(i * size, buf2, 0, size);
            Assert.assertTrue(Arrays.equals(buf, buf2));
        }
    }

    @Test
    public void testOutOfBoundsWrite() throws Throwable {
        try {
            memory.write(memory.size(), (byte) 0);
            Assert.fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            memory.write(memory.size() + 1, (byte) 0);
            Assert.fail();
        } catch (IllegalArgumentException e) {

        }
        memory.write(memory.size() - 1, (byte) 0);

        try {
            memory.write(memory.size() - 1, (short) 0);
            Assert.fail();
        } catch (IllegalArgumentException e) {

        }
        try {
            memory.write(memory.size(), (short) 0);
            Assert.fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            memory.write(memory.size() + 1, (short) 0);
            Assert.fail();
        } catch (IllegalArgumentException e) {
        }
        memory.write(memory.size() - 2, (short) 0);

        try {
            memory.write(memory.size() - 3, 0);
            Assert.fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            memory.write(memory.size(), 0);
            Assert.fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            memory.write(memory.size() + 1, 0);
            Assert.fail();
        } catch (IllegalArgumentException e) {
        }
        memory.write(memory.size() - 4, 0);

        try {
            memory.write(memory.size() - 7, (long) 0);
            Assert.fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            memory.write(memory.size(), (long) 0);
            Assert.fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            memory.write(memory.size() + 1, (long) 0);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            ;
        }
        memory.write(memory.size() - 8, (long) 0);

        try {
            memory.write(-1, (long) 0);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        try {
            memory.write(-64, (long) 0);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        try {
            memory.write(-64, 0);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        try {
            memory.write(-1, 0);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        try {
            memory.write(-64, (short) 0);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        try {
            memory.write(-1, (short) 0);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        try {
            memory.write(-64, (byte) 0);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        try {
            memory.write(-1, (byte) 0);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
    }

    @Test
    public void testOutOfBoundsRead() throws Throwable {
        try {
            memory.read(memory.size());
            Assert.fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            memory.read(memory.size() + 1);
            Assert.fail();
        } catch (IllegalArgumentException e) {
        }
        memory.read(memory.size() - 1);

        try {
            memory.readShort(memory.size() - 1);
            Assert.fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            memory.readShort(memory.size());
            Assert.fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            memory.readShort(memory.size() + 1);
            Assert.fail();
        } catch (IllegalArgumentException e) {
        }
        memory.readShort(memory.size() - 2);

        try {
            memory.readInt(memory.size() - 3);
            Assert.fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            memory.readInt(memory.size());
            Assert.fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            memory.readInt(memory.size() + 1);
            Assert.fail();
        } catch (IllegalArgumentException e) {
        }
        memory.readInt(memory.size() - 4);

        try {
            memory.readLong(memory.size() - 7);
            Assert.fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            memory.readLong(memory.size());
            Assert.fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            memory.readLong(memory.size() + 1);
            Assert.fail();
        } catch (IllegalArgumentException e) {
        }
        memory.readLong(memory.size() - 8);

        try {
            memory.readLong(-1);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        try {
            memory.readLong(-64);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        try {
            memory.readInt(-64);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        try {
            memory.readInt(-1);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        try {
            memory.readShort(-64);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        try {
            memory.readShort(-1);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        try {
            memory.read(-64);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        try {
            memory.read(-1);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
    }

    @Test
    public void testOutOfBoundsCMPXCHG() throws Throwable {

        //CMGPXCHG byte
        try {
            memory.compareAndSet(memory.size(), (byte) 0, (byte) 0);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        try {
            memory.compareAndSet(memory.size() + 1, (byte) 0, (byte) 0);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        try {
            memory.compareAndSet(-64, (byte) 0, (byte) 0);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        try {
            memory.compareAndSet(-1, (byte) 0, (byte) 0);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        memory.compareAndSet(memory.size() - 1, (byte) 0, (byte) 0);

        //CMGPXCHG short
        try {
            memory.compareAndSet(memory.size() - 1, (short) 0, (short) 0);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        try {
            memory.compareAndSet(memory.size(), (short) 0, (short) 0);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        try {
            memory.compareAndSet(memory.size() + 1, (short) 0, (short) 0);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        try {
            memory.compareAndSet(-64, (short) 0, (short) 0);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        try {
            memory.compareAndSet(-1, (short) 0, (short) 0);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        memory.compareAndSet(memory.size() - 2, (short) 0, (short) 0);

        //CMGPXCHG int
        try {
            memory.compareAndSet(memory.size() - 3, 0, 0);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        try {
            memory.compareAndSet(memory.size(), 0, 0);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        try {
            memory.compareAndSet(memory.size() + 1, 0, 0);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        try {
            memory.compareAndSet(-64, 0, 0);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        try {
            memory.compareAndSet(-1, 0, 0);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        memory.compareAndSet(memory.size() - 4, 0, 0);

        //CMGPXCHG long
        try {
            memory.compareAndSet(memory.size() - 7, (long) 0, (long) 0);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        try {
            memory.compareAndSet(memory.size(), (long) 0, (long) 0);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        try {
            memory.compareAndSet(memory.size() + 1, (long) 0, (long) 0);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        try {
            memory.compareAndSet(-64, (long) 0, (long) 0);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        try {
            memory.compareAndSet(-1, (long) 0, (long) 0);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
        memory.compareAndSet(memory.size() - 8, (long) 0, (long) 0);

        //CMPXCHG16B
        byte[] tempBuf = new byte[32];
        try {
            memory.compareAndSet(memory.size() - 15, tempBuf);
            Assert.fail();
        } catch (IllegalArgumentException exc) {

        }
        try {
            memory.compareAndSet(memory.size(), tempBuf);
            Assert.fail();
        } catch (IllegalArgumentException exc) {

        }
        try {
            memory.compareAndSet(memory.size() + 1, tempBuf);
            Assert.fail();
        } catch (IllegalArgumentException exc) {

        }
        try {
            memory.compareAndSet(-64, tempBuf);
            Assert.fail();
        } catch (IllegalArgumentException exc) {

        }
        try {
            memory.compareAndSet(-7, tempBuf);
            Assert.fail();
        } catch (IllegalArgumentException exc) {

        }
        if (!i386) {
            memory.compareAndSet(memory.size() - 16, tempBuf);
        }
    }

    @Test
    public void testOutOfBoundsXADD() throws Throwable {
        //UNDERFLOW

        try {
            memory.getAndAdd(-1, (long) 1);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }

        try {
            memory.getAndAdd(-1, 1);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }

        try {
            memory.getAndAdd(-1, (short) 1);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }

        try {
            memory.getAndAdd(-1, (byte) 1);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }

        //TEST OOB TOO BIG
        try {
            memory.getAndAdd(memory.size() - 7, (long) 1);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }

        try {
            memory.getAndAdd(memory.size() - 3, 1);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }

        try {
            memory.getAndAdd(memory.size() - 1, (short) 1);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }

        try {
            memory.getAndAdd(memory.size(), (byte) 1);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }

        //Test SUCCESS at edge
        memory.getAndAdd(memory.size() - 8, (long) 1);
        memory.getAndAdd(memory.size() - 4, 1);
        memory.getAndAdd(memory.size() - 2, (short) 1);
        memory.getAndAdd(memory.size() - 1, (byte) 1);


    }

    @Test
    public void testOutOfBoundsXCHG() throws Throwable {
        //UNDERFLOW

        try {
            memory.getAndSet(-1, (long) 1);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }

        try {
            memory.getAndSet(-1, 1);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }

        try {
            memory.getAndSet(-1, (short) 1);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }

        try {
            memory.getAndSet(-1, (byte) 1);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }

        //TEST OOB TOO BIG
        try {
            memory.getAndSet(memory.size() - 7, (long) 1);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }

        try {
            memory.getAndSet(memory.size() - 3, 1);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }

        try {
            memory.getAndSet(memory.size() - 1, (short) 1);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }

        try {
            memory.getAndSet(memory.size(), (byte) 1);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }

        //Test SUCCESS at edge
        memory.getAndSet(memory.size() - 8, (long) 1);
        memory.getAndSet(memory.size() - 4, 1);
        memory.getAndSet(memory.size() - 2, (short) 1);
        memory.getAndSet(memory.size() - 1, (byte) 1);
    }


    @Test
    public void testMemset() throws Throwable {
        byte tempVal = 0;
        for (long l = 0; l < memory.size(); l++) {
            tempVal++;
            long tempLen = Math.min(memory.size() - l, Math.abs(rng.nextLong()) % 32);


            if (memory.isValid(l - 1)) {
                memory.write(l - 1, (byte) (tempVal - 1));
            }

            if (memory.isValid(l + tempLen)) {
                memory.write(l + tempLen, (byte) (tempVal + 1));
            }

            for (long i = 0; i < tempLen; i++) {
                memory.write(l + i, (byte) (tempVal - 2));
            }

            memory.set(l, tempVal, tempLen);

            if (memory.isValid(l - 1)) {
                Assert.assertEquals((byte) (tempVal - 1), memory.read(l - 1));
            }


            if (memory.isValid(l + tempLen)) {
                Assert.assertEquals((byte) (tempVal + 1), memory.read(l + tempLen));
            }


            for (long i = 0; i < tempLen; i++) {
                Assert.assertEquals(tempVal, memory.read(l + i));
            }

            memory.write(l, (byte) 2);
            memory.set(l, (byte) 1, 0);
            Assert.assertEquals((byte) 2, memory.read(l));

            try {
                memory.set(l, (byte) 0, (memory.size() - l) + 1);
                Assert.fail();
            } catch (IllegalArgumentException exc) {
            }

            try {
                memory.set(l, (byte) 0, -1);
                Assert.fail();
            } catch (IllegalArgumentException exc) {
            }

        }

        memory.set(memory.size() - 1, (byte) 0, 1);

        try {
            memory.set(memory.size(), (byte) 0, 1);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }


        try {
            memory.set(memory.size(), (byte) 0, -1);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }


        try {
            memory.set(memory.size() + 5, (byte) 0, -8);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }


        try {
            memory.set(memory.size() + 5, (byte) 0, 1);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }


        try {
            memory.set(memory.size(), (byte) 0, 0);
            Assert.fail();
        } catch (IllegalArgumentException exc) {
        }
    }

    @Test
    public void testSpin() throws Throwable {
        Assert.assertFalse(memory.spin(0, 500, 100, 1000, TimeUnit.MILLISECONDS));
        executor.submit(new Callable<Object>() {


            @Override
            public Object call() throws Exception {
                Thread.sleep(300);
                memory.write(0, 500);
                return null;
            }
        });
        Assert.assertTrue(memory.spin(0, 500, 100, 3000, TimeUnit.MILLISECONDS));
        memory.write(0, 444);
        executor.submit(new Callable<Object>() {


            @Override
            public Object call() throws Exception {
                Thread.sleep(300);
                memory.write(0, 500);
                return null;
            }
        });
        Assert.assertTrue(memory.spinAndSet(0, 500, 100, 100, 3000, TimeUnit.MILLISECONDS));
        Assert.assertTrue(memory.readInt(0) == 100);
    }

    @Test
    public void testSpinCloseWithTimeout() throws Throwable {
        executor.submit(new Callable<Object>() {


            @Override
            public Object call() throws Exception {
                Thread.sleep(1000);
                memory.close();
                return null;
            }
        });
        try {
            memory.spin(0, 500, 100, 5000, TimeUnit.MILLISECONDS);
            Assert.fail();
        } catch (NullPointerException exc) {
        }
    }

    @Test
    public void testSpinCloseWithoutTimeout() throws Throwable {
        executor.submit(new Callable<Object>() {


            @Override
            public Object call() throws Exception {
                Thread.sleep(1000);
                memory.close();
                return null;
            }
        });
        try {
            memory.spin(0, 500, 100, TimeUnit.MILLISECONDS);
            Assert.fail();
        } catch (NullPointerException exc) {
        }
    }


}
