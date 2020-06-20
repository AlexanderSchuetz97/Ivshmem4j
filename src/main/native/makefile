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

#Uncomment if not using env variables
#LINUX_CC=gcc
#LINUX_JDK=/opt/jdk1.7.0_80
#WINDOWS_CC=/usr/bin/x86_64-w64-mingw32-gcc-win32
#WINDOWS_JDK=/opt/jdk7Windows


BUILD_BASE_DIR=build

WINDOWS_BUILD_DIR=$(BUILD_BASE_DIR)/windows
WINDOWS_BUILD_DIR_COMMON=$(WINDOWS_BUILD_DIR)/common

LINUX_BUILD_DIR=$(BUILD_BASE_DIR)/linux
LINUX_BUILD_DIR_COMMON=$(LINUX_BUILD_DIR)/common

TARGET_DIR=../resources

WINDOWS_CFLAGS=-I. -I$(WINDOWS_JDK)/include -I$(WINDOWS_JDK)/include/win32

#-U_FORTIFY_SOURCE -D_FORTIFY_SOURCE=0 -fno-stack-protector are added to prevent dependencies to new GLIBC versions.
LINUX_CFLAGS=-I. -I$(LINUX_JDK)/include -I$(LINUX_JDK)/include/linux -fPIC -U_FORTIFY_SOURCE -D_FORTIFY_SOURCE=0 -fno-stack-protector


WINDOWS_LIBS =-lhid -lsetupapi


COMMON_SOURCES=$(shell find common -name "*.c")
COMMON_HEADERS=$(shell find common -name "*.h")
WINDOWS_SOURCES=$(shell find windows -name "*.c")
WINDOWS_HEADERS=$(shell find windows -name "*.h")
LINUX_SOURCES=$(shell find linux -name "*.c")
LINUX_HEADERS=$(shell find linux -name "*.h")

WINDOWS_OBJECTS=$(addprefix $(WINDOWS_BUILD_DIR)/,$(patsubst %.c,%.o,$(notdir $(WINDOWS_SOURCES)))) $(addprefix $(WINDOWS_BUILD_DIR_COMMON)/,$(patsubst %.c,%.o,$(notdir $(COMMON_SOURCES))))
LINUX_OBJECTS=$(addprefix $(LINUX_BUILD_DIR)/,$(patsubst %.c,%.o,$(notdir $(LINUX_SOURCES)))) $(addprefix $(LINUX_BUILD_DIR_COMMON)/,$(patsubst %.c,%.o,$(notdir $(COMMON_SOURCES))))


$(WINDOWS_BUILD_DIR_COMMON)/%.o: common/%.c $(COMMON_HEADERS)
	@echo "Compiling common file $@ for windows" 	
	@mkdir -p $(WINDOWS_BUILD_DIR_COMMON)
	$(WINDOWS_CC) -O3 -c -o $@ $< $(WINDOWS_CFLAGS)

$(WINDOWS_BUILD_DIR_COMMON)/%.o: common/**/%.c $(COMMON_HEADERS)
	@echo "Compiling common file $@ for windows" 	
	@mkdir -p $(WINDOWS_BUILD_DIR_COMMON)
	$(WINDOWS_CC) -O3 -c -o $@ $< $(WINDOWS_CFLAGS)


$(WINDOWS_BUILD_DIR)/%.o: windows/%.c $(COMMON_HEADERS) $(WINDOWS_HEADERS)
	@echo "Compiling windows file $@"
	@mkdir -p $(WINDOWS_BUILD_DIR)
	$(WINDOWS_CC) -O3 -c -o $@ $< $(WINDOWS_CFLAGS)

$(WINDOWS_BUILD_DIR)/%.o: windows/**/%.c $(COMMON_HEADERS) $(WINDOWS_HEADERS)
	@echo "Compiling windows file $@"
	@mkdir -p $(WINDOWS_BUILD_DIR)
	$(WINDOWS_CC) -O3 -c -o $@ $< $(WINDOWS_CFLAGS)

$(LINUX_BUILD_DIR_COMMON)/%.o: common/%.c $(COMMON_HEADERS)
	@echo "Compiling common file $@ for linux" 	
	@mkdir -p $(LINUX_BUILD_DIR_COMMON)
	$(LINUX_CC) -O3 -c -o $@ $< $(LINUX_CFLAGS)

$(LINUX_BUILD_DIR_COMMON)/%.o: common/**/%.c $(COMMON_HEADERS)
	@echo "Compiling common file $@ for linux" 	
	@mkdir -p $(LINUX_BUILD_DIR_COMMON)
	$(LINUX_CC) -O3 -c -o $@ $< $(LINUX_CFLAGS)

$(LINUX_BUILD_DIR)/%.o: linux/%.c $(COMMON_HEADERS) $(LINUX_HEADERS)
	@echo "Compiling linux file $@"
	@mkdir -p $(LINUX_BUILD_DIR)
	$(LINUX_CC) -O3 -c -o $@ $< $(LINUX_CFLAGS)

$(LINUX_BUILD_DIR)/%.o: linux/**/%.c $(COMMON_HEADERS) $(LINUX_HEADERS)
	@echo "Compiling linux file $@"
	@mkdir -p $(LINUX_BUILD_DIR)
	$(LINUX_CC) -O3 -c -o $@ $< $(LINUX_CFLAGS)

both: windows linux
	rm -rf $(BUILD_BASE_DIR)
	@echo "DONE"

windows: $(WINDOWS_OBJECTS)
	@echo "Linking windows dynamic linker library"
	@mkdir -p $(TARGET_DIR)
	$(WINDOWS_CC) -shared -o $(TARGET_DIR)/ivshmem4j.dll $(WINDOWS_OBJECTS) $(WINDOWS_LIBS)

linux: $(LINUX_OBJECTS)
	@echo "Linking linux shared object"
	@mkdir -p $(TARGET_DIR)
	$(LINUX_CC) -shared -o $(TARGET_DIR)/ivshmem4j.so $(LINUX_OBJECTS)

clean:
	rm -rf $(TARGET_DIR)/ivshmem4j.dll
	rm -rf $(TARGET_DIR)/ivshmem4j.so
	rm -rf $(BUILD_BASE_DIR)
	@echo "DONE"

info:
	@echo "INFO"
	@echo "Linux compiler $(LINUX_CC)"
	@echo "Windows compiler $(WINDOWS_CC)"
	@echo "Linux JDK Path $(LINUX_JDK)"
	@echo "Windows JDK Path $(WINDOWS_JDK)"
	@echo "Common Sources $(COMMON_SOURCES)"
	@echo "Common Headers $(COMMON_HEADERS)"
	@echo "Windows Sources $(WINDOWS_SOURCES)"
	@echo "Windows Headers $(WINDOWS_HEADERS)"
	@echo "Linux Sources $(LINUX_SOURCES)"
	@echo "Linux Headers $(LINUX_HEADERS)"
	@echo "Windows Objects $(WINDOWS_OBJECTS)"
	@echo "Linux Objects $(LINUX_OBJECTS)"
	

.PHONY: clean
