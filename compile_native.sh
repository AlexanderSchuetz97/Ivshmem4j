#!/usr/bin/env bash
#
# Copyright Alexander Schütz, 2020
#
# This file is part of Ivshmem4j.
#
# Ivshmem4j is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# Ivshmem4j is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# A copy of the GNU General Public License should be provided
# in the COPYING file in top level directory of Ivshmem4j.
# If not, see <https://www.gnu.org/licenses/>.
#



echo "Building the native libraries for ivshmem4j"

#load config
if [ -f config.sh ]
then
source config.sh
else
echo "Configuration file not found, this project requires a windows jdk and a linux jdk in order to build"
echo "Because of this, you will have to manually specify the path to both jdk homes in the file config.sh"
echo "This file will now be created, edit it, input your paths and then try again"
touch config.sh
echo "#!/usr/bin/env bash" > config.sh
echo "#You may want to change this to use the javah command from the LINUX_JDK if your system jdk is not the same as your LINUX_JDK" >> config.sh
echo "export JAVAH_COMMAND=javah" >> config.sh
echo "#If you are running a normal 64 bit linux change this to just gcc if you dont have gcc multi-lib installed" >> config.sh
echo "export LINUX_CC_AMD64=/usr/bin/x86_64-linux-gnu-gcc" >> config.sh
echo "export LINUX_CC_I386=/usr/bin/i686-linux-gnu-gcc" >> config.sh
echo "export LINUX_JDK=" >> config.sh
echo "#You will have to change this unless you are using mingw-cross to compile the windows dll" >> config.sh
echo "export WINDOWS_CC_AMD64=/usr/bin/x86_64-w64-mingw32-gcc-win32" >> config.sh
echo "export WINDOWS_CC_I386=/usr/bin/i686-w64-mingw32-gcc" >> config.sh
echo "export WINDOWS_JDK=" >> config.sh
echo "#You may change this to any combination of the following: \"windows_amd64 windows_i386 linux_amd64 linux_i386\"" >> config.sh
echo "export BUILD_TARGETS=\"all\"" >> config.sh
chmod +x config.sh
exit -1
fi

#Cleanup
rm -rf src/main/native/common/jni
mkdir -p src/main/native/common/jni
rm -f src/main/resources/*.dll
rm -f src/main/resources/*.so
rm -f src/main/native_src.tar
rm -f src/resources/native_src.tar

#Building headers
$JAVAH_COMMAND -cp src/main/java -d src/main/native/common/jni de.aschuetz.ivshmem4j.common.CommonSharedMemory
$JAVAH_COMMAND -cp src/main/java -d src/main/native/common/jni de.aschuetz.ivshmem4j.linux.doorbell.LinuxSharedMemory
$JAVAH_COMMAND -cp src/main/java -d src/main/native/common/jni de.aschuetz.ivshmem4j.linux.plain.LinuxSharedMemory
$JAVAH_COMMAND -cp src/main/java -d src/main/native/common/jni de.aschuetz.ivshmem4j.windows.WindowsSharedMemory



cd src/main/native
make clean

#Copy the native sources to the resources folder.
cd ..
tar -cvf native_src.tar native
mv native_src.tar resources/
cd native

make $BUILD_TARGETS
cd ../resources/

#Confirm success

if [[ $BUILD_TARGETS == *"windows_amd64"* ]] || [ $BUILD_TARGETS == "all" ]; then
    if [ -f ivshmem4j_amd64.dll ]; then
        echo "Building the windows amd64 dll succeeded"
    else
        echo "Building the windows amd64 dll failed!"
        exit -1
    fi
fi


if [[ $BUILD_TARGETS == *"windows_i386"* ]] || [ $BUILD_TARGETS == "all" ]; then
    if [ -f ivshmem4j_i386.dll ]; then
        echo "Building the windows i386 dll succeeded"
    else
        echo "Building the windows i386 dll failed!"
        exit -1
    fi
fi


if [[ $BUILD_TARGETS == *"linux_amd64"* ]] || [ $BUILD_TARGETS == "all" ]; then
    if [ -f ivshmem4j_amd64.so ]; then
        echo "Building the linux amd64 so succeeded"
    else
        echo "Building the linux amd64 so failed!"
        exit -1
    fi
fi


if [[ $BUILD_TARGETS == *"linux_i386"* ]] || [ $BUILD_TARGETS == "all" ]; then
    if [ -f ivshmem4j_i386.so ]; then
        echo "Building the linux i386 so succeeded"
    else
        echo "Building the linux i386 so failed!"
        exit -1
    fi
fi

cd ../../../
