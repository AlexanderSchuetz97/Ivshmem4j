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

package de.aschuetz.ivshmem4j.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility to load the native libraries. It can be used to unpack the .so/.dll files from the jar and load them into the jvm.
 * If you wish to manually load the native libraries by other means then dont use it otherwise call loadNativeLibraries before
 * using Ivshmem4j.
 */
public class NativeLibraryLoaderHelper {

    /**
     * Flag to indicate if already loaded.
     */
    private static volatile boolean loaded = false;

    /**
     * Will load the native libraries if necessary.
     * If the loading fails then a LinkageError is thrown.
     * This method will use the temporary directory to unpack the .dll/.so files (this is necessary as native libraries
     * cannot be loaded from inside the jar) On Windows this is %TEMP% on linux this is /tmp.
     *
     * @throws LinkageError
     */
    public static void loadNativeLibraries() throws LinkageError {
        loadNativeLibraries(System.getProperty("java.io.tmpdir"));
    }

    /**
     * Will load the native libraries if necessary.
     * If the loading fails then a LinkageError is thrown.
     * The given path will be used as a directory to unpack the native libraries to.
     * If the directory at the given path does not exist it will be created.
     * If that fails that fails a LinkageError is thrown.
     *
     * @throws LinkageError
     */
    public synchronized static void loadNativeLibraries(String aPath) throws LinkageError {
        if (loaded) {
            return;
        }

        boolean tempSuccess = false;
        long tempVersion = -1;

        try {
            tempVersion = CommonSharedMemory.getNativeLibVersion();
            tempSuccess = true;
        } catch (UnsatisfiedLinkError err) {

        }

        if (tempSuccess && tempVersion == CommonSharedMemory.EXPECTED_NATIVE_LIB_VERSION) {
            loaded = true;
            return;
        }

        if (tempSuccess && tempVersion != CommonSharedMemory.EXPECTED_NATIVE_LIB_VERSION) {
            throw new UnsatisfiedLinkError("Wrong ivshmem4j lib version was loaded expected " + CommonSharedMemory.EXPECTED_NATIVE_LIB_VERSION + " got " + tempVersion);
        }

        if (aPath == null) {
            throw new UnsatisfiedLinkError("Temporary file path to unpack native libraries to is null!");
        }

        File tempFile = new File(aPath);

        if (!tempFile.exists()) {
            tempFile.mkdirs();
        }

        if (!tempFile.exists() || !tempFile.isDirectory()) {
            throw new UnsatisfiedLinkError("Temporary file path doesnt exist and cannot be created! Path: " + tempFile.getAbsolutePath());
        }

        String tempArch = System.getProperty("os.arch");
        if (!"amd64".equalsIgnoreCase(tempArch)) {
            throw new UnsatisfiedLinkError("Cannot load native libraries because the system architecture is not amd64!");
        }

        String tempOS = System.getProperty("os.name");
        if (tempOS == null) {
            throw new UnsatisfiedLinkError("Cannot load native libraries because the operating system couldn't be detected!");
        }

        tempOS = tempOS.toLowerCase();
        try {
            if (tempOS.contains("linux")) {
                loadLib(tempFile, "ivshmem4j.so");
            } else if (tempOS.contains("windows")) {
                loadLib(tempFile, "ivshmem4j.dll");
            } else {
                throw new UnsatisfiedLinkError("Operating system is not windows or linux and thus not supported!");
            }
        } catch (Exception e) {
            throw new LinkageError("IO Error while writing native library to a temporary file!", e);
        }

        tempVersion = CommonSharedMemory.getNativeLibVersion();
        if (tempVersion != CommonSharedMemory.EXPECTED_NATIVE_LIB_VERSION) {
            throw new UnsatisfiedLinkError("Wrong ivshmem4j lib version was loaded expected " + CommonSharedMemory.EXPECTED_NATIVE_LIB_VERSION + " got " + tempVersion);
        }

        loaded = true;
    }


    private static void loadLib(File aBase, String aLibName) throws IOException {
        File tempLibFile = new File(aBase, aLibName);
        StringBuilder tempBuilder = new StringBuilder();
        while (tempLibFile.exists()) {
            tempBuilder.append('X');
            tempLibFile = new File(aBase, tempBuilder.toString() + aLibName);
        }

        if (!tempLibFile.createNewFile()) {
            throw new IOException("Could not create temporary library file!");
        }

        tempLibFile.deleteOnExit();

        FileOutputStream tempFaos = new FileOutputStream(tempLibFile);

        byte[] tempBuf = new byte[512];

        InputStream tempInput = NativeLibraryLoaderHelper.class.getResourceAsStream("/" + aLibName);
        if (tempInput == null) {
            throw new IOException("The shared library file " + aLibName + " was not found by getResourceAsStream " +
                    "its either not there or this class was loaded by a classloader that doesnt support resources well!");
        }
        int i = 0;
        while (i != -1) {
            i = tempInput.read(tempBuf);
            if (i > 0) {
                tempFaos.write(tempBuf, 0, i);
            }
        }

        tempFaos.flush();
        tempFaos.close();
        tempInput.close();

        System.load(tempLibFile.getAbsolutePath());
        tempLibFile.delete();
    }
}
