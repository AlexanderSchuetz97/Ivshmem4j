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

package de.aschuetz.ivshmem4j;

import de.aschuetz.ivshmem4j.api.SharedMemory;
import de.aschuetz.ivshmem4j.api.SharedMemoryException;
import de.aschuetz.ivshmem4j.common.ErrorCodeEnum;
import de.aschuetz.ivshmem4j.common.NativeLibraryLoaderHelper;
import de.aschuetz.ivshmem4j.linux.plain.LinuxMappedFileSharedMemory;
import org.junit.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public class BasicSharedMemoryTest {

    private Random rng = new Random();

    private File shmemfile;

    private SharedMemory memory;

    @BeforeClass
    public static void setupJNI() {
        System.out.println("These Tests might take a lot of time!");
        NativeLibraryLoaderHelper.loadNativeLibraries();
    }

    @Before
    public void before() throws Throwable {
        rng.setSeed(System.currentTimeMillis());
        shmemfile = new File("/dev/shm/" + getClass().getSimpleName() + Math.abs(rng.nextInt()));
        if (shmemfile.exists()) {
            shmemfile.delete();
        }
        memory = LinuxMappedFileSharedMemory.createOrOpen(shmemfile.getAbsolutePath(), 4096);
        shmemfile.deleteOnExit();
        rng.setSeed(0);

        for (long i = 0; i < memory.getSharedMemorySize(); i++) {
            memory.write(i, (byte) 0);
        }
    }

    @After
    public void after() {
        memory.close();
        shmemfile.delete();
    }

    @Test
    public void testSingleByteWriteAndRead() throws Throwable {
        ArrayList<Integer> tempInt = new ArrayList<>(0xff);
        for (int i = 0; i < 0xff; i++) {
            tempInt.add(i);
        }

        Collections.shuffle(tempInt);

        for (int value : tempInt) {
            for (long i = 0; i < memory.getSharedMemorySize(); i++) {
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
            for (long i = 0; i + 1 < memory.getSharedMemorySize(); i++) {
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
            for (long i = 0; i + 3 < memory.getSharedMemorySize(); i++) {
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
            for (long i = 0; i + 7 < memory.getSharedMemorySize(); i++) {
                //System.out.println(i);
                memory.write(i, value);
                Assert.assertEquals(value, memory.readLong(i));
            }
        }
    }

    @Test
    public void xadd1Byte() throws Throwable {
        for (long i = 0; i < memory.getSharedMemorySize(); i++) {
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
        for (long i = 0; i + 1 < memory.getSharedMemorySize(); i++) {
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
        for (long i = 0; i + 3 < memory.getSharedMemorySize(); i++) {
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
        for (long i = 0; i + 7 < memory.getSharedMemorySize(); i++) {
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
        for (long i = 0; i < memory.getSharedMemorySize(); i++) {
            for (int j = 0; j < 0xff; j++) {
                Assert.assertTrue(memory.compareAndSet(i, (byte) j, (byte) (j + 1)));
                Assert.assertFalse(memory.compareAndSet(i, (byte) j, (byte) (j + 2)));
            }
        }
    }

    @Test
    public void cmpxchg2b() throws Throwable {
        for (long i = 0; i + 1 < memory.getSharedMemorySize(); i++) {
            memory.write(i, (short) 0);
            for (int j = 0; j < 0xffff; j++) {
                Assert.assertTrue(memory.compareAndSet(i, (short) j, (short) (j + 1)));
                Assert.assertFalse(memory.compareAndSet(i, (short) j, (short) (j + 2)));
            }
        }
    }

    @Test
    public void cmpxchg4b() throws Throwable {
        for (long i = 0; i + 3 < memory.getSharedMemorySize(); i++) {
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
        for (long i = 0; i + 7 < memory.getSharedMemorySize(); i++) {
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
    public void xchg8b() throws Throwable {
        for (long i = 0; i + 7 < memory.getSharedMemorySize(); i++) {
            long tempBase = rng.nextLong();
            long tempSet = rng.nextLong();
            memory.write(i, tempBase);
            Assert.assertEquals(tempBase, memory.getAndSet(i, tempSet));
            Assert.assertEquals(tempSet, memory.readLong(i));
        }
    }

    @Test
    public void xchg4b() throws Throwable {
        for (long i = 0; i + 7 < memory.getSharedMemorySize(); i++) {
            int tempBase = rng.nextInt();
            int tempSet = rng.nextInt();
            memory.write(i, tempBase);
            Assert.assertEquals(tempBase, memory.getAndSet(i, tempSet));
            Assert.assertEquals(tempSet, memory.readInt(i));
        }
    }

    @Test
    public void xchg2b() throws Throwable {
        for (long i = 0; i + 7 < memory.getSharedMemorySize(); i++) {
            short tempBase = (short) rng.nextInt();
            short tempSet = (short) rng.nextInt();
            memory.write(i, tempBase);
            Assert.assertEquals(tempBase, memory.getAndSet(i, tempSet));
            Assert.assertEquals(tempSet, memory.readShort(i));
        }
    }

    @Test
    public void xchg1b() throws Throwable {
        for (long i = 0; i + 7 < memory.getSharedMemorySize(); i++) {
            byte tempBase = (byte) rng.nextInt();
            byte tempSet = (byte) rng.nextInt();
            memory.write(i, tempBase);
            Assert.assertEquals(tempBase, memory.getAndSet(i, tempSet));
            Assert.assertEquals(tempSet, memory.read(i));
        }
    }

    @Test
    public void testWriteAndReadByteArrays() throws Throwable {
        int size = (int) memory.getSharedMemorySize() / 8;

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
            memory.write(memory.getSharedMemorySize(), (byte) 0);
            Assert.fail();
        } catch (SharedMemoryException e) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, e.getCode());
        }
        try {
            memory.write(memory.getSharedMemorySize() + 1, (byte) 0);
            Assert.fail();
        } catch (SharedMemoryException e) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, e.getCode());
        }
        memory.write(memory.getSharedMemorySize() - 1, (byte) 0);

        try {
            memory.write(memory.getSharedMemorySize() - 1, (short) 0);
            Assert.fail();
        } catch (SharedMemoryException e) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, e.getCode());
        }
        try {
            memory.write(memory.getSharedMemorySize(), (short) 0);
            Assert.fail();
        } catch (SharedMemoryException e) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, e.getCode());
        }
        try {
            memory.write(memory.getSharedMemorySize() + 1, (short) 0);
            Assert.fail();
        } catch (SharedMemoryException e) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, e.getCode());
        }
        memory.write(memory.getSharedMemorySize() - 2, (short) 0);

        try {
            memory.write(memory.getSharedMemorySize() - 3, 0);
            Assert.fail();
        } catch (SharedMemoryException e) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, e.getCode());
        }
        try {
            memory.write(memory.getSharedMemorySize(), 0);
            Assert.fail();
        } catch (SharedMemoryException e) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, e.getCode());
        }
        try {
            memory.write(memory.getSharedMemorySize() + 1, 0);
            Assert.fail();
        } catch (SharedMemoryException e) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, e.getCode());
        }
        memory.write(memory.getSharedMemorySize() - 4, 0);

        try {
            memory.write(memory.getSharedMemorySize() - 7, (long) 0);
            Assert.fail();
        } catch (SharedMemoryException e) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, e.getCode());
        }
        try {
            memory.write(memory.getSharedMemorySize(), (long) 0);
            Assert.fail();
        } catch (SharedMemoryException e) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, e.getCode());
        }
        try {
            memory.write(memory.getSharedMemorySize() + 1, (long) 0);
            Assert.fail();
        } catch (SharedMemoryException e) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, e.getCode());
        }
        memory.write(memory.getSharedMemorySize() - 8, (long) 0);

        try {
            memory.write(-1, (long) 0);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.write(-64, (long) 0);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.write(-64, 0);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.write(-1, 0);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.write(-64, (short) 0);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.write(-1, (short) 0);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.write(-64, (byte) 0);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.write(-1, (byte) 0);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
    }

    @Test
    public void testOutOfBoundsRead() throws Throwable {
        try {
            memory.read(memory.getSharedMemorySize());
            Assert.fail();
        } catch (SharedMemoryException e) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, e.getCode());
        }
        try {
            memory.read(memory.getSharedMemorySize() + 1);
            Assert.fail();
        } catch (SharedMemoryException e) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, e.getCode());
        }
        memory.read(memory.getSharedMemorySize() - 1);

        try {
            memory.readShort(memory.getSharedMemorySize() - 1);
            Assert.fail();
        } catch (SharedMemoryException e) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, e.getCode());
        }
        try {
            memory.readShort(memory.getSharedMemorySize());
            Assert.fail();
        } catch (SharedMemoryException e) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, e.getCode());
        }
        try {
            memory.readShort(memory.getSharedMemorySize() + 1);
            Assert.fail();
        } catch (SharedMemoryException e) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, e.getCode());
        }
        memory.readShort(memory.getSharedMemorySize() - 2);

        try {
            memory.readInt(memory.getSharedMemorySize() - 3);
            Assert.fail();
        } catch (SharedMemoryException e) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, e.getCode());
        }
        try {
            memory.readInt(memory.getSharedMemorySize());
            Assert.fail();
        } catch (SharedMemoryException e) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, e.getCode());
        }
        try {
            memory.readInt(memory.getSharedMemorySize() + 1);
            Assert.fail();
        } catch (SharedMemoryException e) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, e.getCode());
        }
        memory.readInt(memory.getSharedMemorySize() - 4);

        try {
            memory.readLong(memory.getSharedMemorySize() - 7);
            Assert.fail();
        } catch (SharedMemoryException e) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, e.getCode());
        }
        try {
            memory.readLong(memory.getSharedMemorySize());
            Assert.fail();
        } catch (SharedMemoryException e) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, e.getCode());
        }
        try {
            memory.readLong(memory.getSharedMemorySize() + 1);
            Assert.fail();
        } catch (SharedMemoryException e) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, e.getCode());
        }
        memory.readLong(memory.getSharedMemorySize() - 8);

        try {
            memory.readLong(-1);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.readLong(-64);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.readInt(-64);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.readInt(-1);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.readShort(-64);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.readShort(-1);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.read(-64);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.read(-1);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
    }

    @Test
    public void testOutOfBoundsCMPXCHG() throws Throwable {

        //CMGPXCHG byte
        try {
            memory.compareAndSet(memory.getSharedMemorySize(), (byte) 0, (byte) 0);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.compareAndSet(memory.getSharedMemorySize() + 1, (byte) 0, (byte) 0);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.compareAndSet(-64, (byte) 0, (byte) 0);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.compareAndSet(-1, (byte) 0, (byte) 0);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        memory.compareAndSet(memory.getSharedMemorySize() - 1, (byte) 0, (byte) 0);

        //CMGPXCHG short
        try {
            memory.compareAndSet(memory.getSharedMemorySize() - 1, (short) 0, (short) 0);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.compareAndSet(memory.getSharedMemorySize(), (short) 0, (short) 0);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.compareAndSet(memory.getSharedMemorySize() + 1, (short) 0, (short) 0);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.compareAndSet(-64, (short) 0, (short) 0);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.compareAndSet(-1, (short) 0, (short) 0);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        memory.compareAndSet(memory.getSharedMemorySize() - 2, (short) 0, (short) 0);

        //CMGPXCHG int
        try {
            memory.compareAndSet(memory.getSharedMemorySize() - 3, 0, 0);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.compareAndSet(memory.getSharedMemorySize(), 0, 0);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.compareAndSet(memory.getSharedMemorySize() + 1, 0, 0);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.compareAndSet(-64, 0, 0);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.compareAndSet(-1, 0, 0);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        memory.compareAndSet(memory.getSharedMemorySize() - 4, 0, 0);

        //CMGPXCHG long
        try {
            memory.compareAndSet(memory.getSharedMemorySize() - 7, (long) 0, (long) 0);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.compareAndSet(memory.getSharedMemorySize(), (long) 0, (long) 0);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.compareAndSet(memory.getSharedMemorySize() + 1, (long) 0, (long) 0);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.compareAndSet(-64, (long) 0, (long) 0);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.compareAndSet(-1, (long) 0, (long) 0);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        memory.compareAndSet(memory.getSharedMemorySize() - 8, (long) 0, (long) 0);

        //CMPXCHG16B
        byte[] tempBuf = new byte[32];
        try {
            memory.compareAndSet(memory.getSharedMemorySize() - 15, tempBuf);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.compareAndSet(memory.getSharedMemorySize(), tempBuf);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.compareAndSet(memory.getSharedMemorySize() + 1, tempBuf);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.compareAndSet(-64, tempBuf);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        try {
            memory.compareAndSet(-7, tempBuf);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }
        memory.compareAndSet(memory.getSharedMemorySize() - 16, tempBuf);
    }

    @Test
    public void testOutOfBoundsXADD() throws Throwable {
        //UNDERFLOW

        try {
            memory.getAndAdd(-1, (long) 1);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }

        try {
            memory.getAndAdd(-1, 1);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }

        try {
            memory.getAndAdd(-1, (short) 1);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }

        try {
            memory.getAndAdd(-1, (byte) 1);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }

        //TEST OOB TOO BIG
        try {
            memory.getAndAdd(memory.getSharedMemorySize() - 7, (long) 1);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }

        try {
            memory.getAndAdd(memory.getSharedMemorySize() - 3, 1);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }

        try {
            memory.getAndAdd(memory.getSharedMemorySize() - 1, (short) 1);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }

        try {
            memory.getAndAdd(memory.getSharedMemorySize(), (byte) 1);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }

        //Test SUCCESS at edge
        memory.getAndAdd(memory.getSharedMemorySize() - 8, (long) 1);
        memory.getAndAdd(memory.getSharedMemorySize() - 4, 1);
        memory.getAndAdd(memory.getSharedMemorySize() - 2, (short) 1);
        memory.getAndAdd(memory.getSharedMemorySize() - 1, (byte) 1);


    }

    @Test
    public void testOutOfBoundsXCHG() throws Throwable {
        //UNDERFLOW

        try {
            memory.getAndSet(-1, (long) 1);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }

        try {
            memory.getAndSet(-1, 1);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }

        try {
            memory.getAndSet(-1, (short) 1);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }

        try {
            memory.getAndSet(-1, (byte) 1);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }

        //TEST OOB TOO BIG
        try {
            memory.getAndSet(memory.getSharedMemorySize() - 7, (long) 1);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }

        try {
            memory.getAndSet(memory.getSharedMemorySize() - 3, 1);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }

        try {
            memory.getAndSet(memory.getSharedMemorySize() - 1, (short) 1);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }

        try {
            memory.getAndSet(memory.getSharedMemorySize(), (byte) 1);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertEquals(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS, exc.getCode());
        }

        //Test SUCCESS at edge
        memory.getAndSet(memory.getSharedMemorySize() - 8, (long) 1);
        memory.getAndSet(memory.getSharedMemorySize() - 4, 1);
        memory.getAndSet(memory.getSharedMemorySize() - 2, (short) 1);
        memory.getAndSet(memory.getSharedMemorySize() - 1, (byte) 1);
    }


    @Test
    public void testMemset() throws Throwable {
        byte tempVal = 0;
        for (long l = 0; l < memory.getSharedMemorySize(); l++) {
            tempVal++;
            long tempLen = Math.min(memory.getSharedMemorySize() - l, Math.abs(rng.nextLong()) % 32);


            if (memory.isAddressValid(l - 1)) {
                memory.write(l - 1, (byte) (tempVal - 1));
            }

            if (memory.isAddressValid(l + tempLen)) {
                memory.write(l + tempLen, (byte) (tempVal + 1));
            }

            for (long i = 0; i < tempLen; i++) {
                memory.write(l + i, (byte) (tempVal - 2));
            }

            memory.write(l, tempVal, tempLen);

            if (memory.isAddressValid(l - 1)) {
                Assert.assertEquals((byte) (tempVal - 1), memory.read(l - 1));
            }


            if (memory.isAddressValid(l + tempLen)) {
                Assert.assertEquals((byte) (tempVal + 1), memory.read(l + tempLen));
            }


            for (long i = 0; i < tempLen; i++) {
                Assert.assertEquals(tempVal, memory.read(l + i));
            }

            memory.write(l, (byte) 2);
            memory.write(l, (byte) 1, 0);
            Assert.assertEquals((byte) 2, memory.read(l));

            try {
                memory.write(l, (byte) 0, (memory.getSharedMemorySize() - l) + 1);
                Assert.fail();
            } catch (SharedMemoryException exc) {
                Assert.assertTrue(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS == exc.getCode());
            }

            try {
                memory.write(l, (byte) 0, -1);
                Assert.fail();
            } catch (SharedMemoryException exc) {
                Assert.assertTrue(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS == exc.getCode());
            }

        }

        memory.write(memory.getSharedMemorySize() - 1, (byte) 0, 1);

        try {
            memory.write(memory.getSharedMemorySize(), (byte) 0, 1);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertTrue(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS == exc.getCode());
        }


        try {
            memory.write(memory.getSharedMemorySize(), (byte) 0, -1);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertTrue(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS == exc.getCode());
        }


        try {
            memory.write(memory.getSharedMemorySize() + 5, (byte) 0, -8);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertTrue(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS == exc.getCode());
        }


        try {
            memory.write(memory.getSharedMemorySize() + 5, (byte) 0, 1);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertTrue(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS == exc.getCode());
        }


        try {
            memory.write(memory.getSharedMemorySize(), (byte) 0, 0);
            Assert.fail();
        } catch (SharedMemoryException exc) {
            Assert.assertTrue(ErrorCodeEnum.MEMORY_OUT_OF_BOUNDS == exc.getCode());
        }
    }
}
