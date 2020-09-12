# Ivshmem4j Readme
Ivshmem4j is a Java library to use QEMU ivshmem (inter virtual machine shared memory) from a Java application running inside a JVM.

The primary purpose of Ivshmem4j is to facilitate fast communication using ivshmem between applications whose JVM's are running inside different QEMU virtual machines.

Ivshmem4j provides full support for msi/software-interrupts offered by ivshmem-doorbell. <br>
Additionally Ivshmem4j provides some useful features such as:
* Atomic numbers (like those in the java.util.concurrent.atomic package) that reside in shared memory  
* Implementation of java.util.concurrent.locks.Lock that facilitates synchronisation between applications.
* Implementation of InputStream/OutputStream using a Ring Buffer that resides in shared memory for communication between 2 applications.
* Performing various atmoic operations (xadd / xchg / cmpxchg / cmpxchg16b) on Shared Memory that are not available when using RandomAccessFile.
## License
Ivshmem4j is released under the GNU General Public License Version 3. <br>A copy of the GNU General Public License Version 3 can be found in the COPYING file.<br>


The file "mvnw" is part of the maven-wrapper project, released under the Apache License Version 2.<br>
See https://github.com/takari/maven-wrapper for more information regarding maven-wrapper.
## Supported operating systems
##### Host:
* Linux (amd64/i386)
##### Guest:
* Windows (amd64/i386)
* Linux (amd64/i386)

##### Limitations of Linux guests:
Since there is no stable linux kernel module to interact with the emulated ivshmem pci device 
and since its is not possible to receive interrupts without one, interrupts are not supported on Linux guests.
## Building / Installation
#### Windows:
Building Ivshmem4j on Windows is currently not possible.
#### Linux:
Requirements:
* mingw-cross (x86)
* mingw-cross (i386)
* gcc compiler (x86)
* gcc compiler (i386)
* make
* bash
* linux JDK 7 or newer
* Windows JDK 7 or newer (only for headers)

JDK 7 (Oracle), 8 (OpenJDK) and 11 (OpenJDK) were tested.

On a 64 bit Ubuntu all requirements for building can be installed by running:
````
sudo apt-get install build-essential gcc-mingw-w64 openjdk-8-jdk gcc-mingw-w64-i686 gcc-mingw-w64-x86-64 gcc-i686-linux-gnu 
````
You may have to adjust this command if you desire a different JDK version.

To get the Windows JDK either copy the JDK home directory from a Windows installation or use other means 
(such as msiextract or unzip) to extract the JDK home directory from a Windows setup file.

It is not required to run anything using the Windows JDK. The only relevant files for Ivshmem4j 
are inside the "include" folder. 
It is recommended but not required to use the same JDK version for Windows and linux because the 
JNI headers for Windows are generated using the linux JDK's javah command to avoid having to run a Windows binary.

Building:
* clone the git repository
````
git clone -b 1.1 https://github.com/AlexanderSchuetz97/Ivshmem4j.git
````
* run: 
````
bash compile_native.sh
````
* edit the newly created config.sh file and enter the paths for your Windows JDK home and linux JDK home.
* run: 
````
./mvnw -Dmaven.test.skip=true clean package
````
The compiled jar file for Ivshmem4j should now be located inside the target folder.<br>
To install Ivshmem4j to a local maven repository you may instead/additionally run:
````
./mvnw -Dmaven.test.skip=true clean install
````
## Runtime dependencies
##### Windows guest
* Ivshmem device driver which is contained in the virtio driver
https://fedorapeople.org/groups/virt/virtio-win/direct-downloads/upstream-virtio/
(Use version 0.1-161 or later)

* Note: The Windows 7 Ivshmem driver seems broken and will always lead to a BSOD. Only Windows 10 was tested. 
Other versions of Windows may or may not work.

##### Linux host
* ivshmem-plain
    * No dependencies required.
* ivshmem-doorbell
    * Ivshmem-server (one should come bundled with QEMU, but QEMU recommends not using it for production)

##### Linux guest
* No dependencies required.
## How to use Ivshmem4j
##### Common (loading native libraries):
````
//Ivshmem4j does not load the native from the java.library.path to avoid introducing additional constraints
//to your application. You may use this call to let Ivshmem4j handle loading the native libraries
//but you may also decide to load them yourself by other means if this is more suitable for your application.
//This only needs to be called once. Any additonal calls are NOOPs.
NativeLibraryLoaderHelper.loadNativeLibraries();
````
##### Linux Host(ivshmem-plain):
````
//Create or open a shared memory file at "/dev/shm/test" 
//Behavior for file descriptors that are not on tempfs/ramfs is undefined as Ivshmem4j never calls msync.
//QEMU ivshmem-plain uses either /dev/shm or hubgelbtfs (not supported by Ivshmem4j)
SharedMemory memory = LinuxMappedFileSharedMemory.createOrOpen("/dev/shm/test", 64);
````
##### Linux Host (ivshmem-doorbell):
````
//This requires running a ivshmem-server at "/tmp/test" which is part of the QEMU Project.
//ivshmem-server should already be installed on your system if you use it to run a QEMU VM.
SharedMemory tempClient = IvshmemLinuxClient.connect("/tmp/test");
````
##### Linux Guest (ivshmem-plain and "ivshmem-doorbell"):
See Limitations of Linux guests.
````
// Opens the PCI device file descriptor of the device at bus id 0000:00:0e.0.
// The Bus ID is configured in QEMU at the host and may be retrieved by parsing the output of lspci and looking for
// Red Hat, Inc. Inter-VM shared memory (rev 01).
// Example Output line from lspci:
// 00:0e.0 RAM memory: Red Hat, Inc. Inter-VM shared memory (rev 01)
// Hint: make sure the JVM process has permissions to this file. By default only root has.
SharedMemory memory = LinuxMappedFileSharedMemory.open("/sys/bus/pci/devices/0000:00:0e.0/resource2_wc");
````
##### Windows (ivshmem-plain and ivshmem-doorbell):
````
//Enumerates all Windows Ivshmem devices. QEMU supports multiple shared memories per virtual machine.
Collection<IvshmemWindowsDevice> devices = IvshmemWindowsDevice.getSharedMemoryDevices();

SharedMemory memory = null;
for (IvshmemWindowsDevice device : devices) {
    //Print the device size (in bytes) and device name. 
    //The Device name contains the PCI Bus ID which can be set in QEMU.
    System.out.println(device.getSharedMemorySize() + " " + device.getNameAsString());
    try {
        //Try to open the device.
        memory = device.open();
        System.out.println("Success!");
        break;
    } catch (SharedMemoryException exc) {
        //If a device is already in use by another process the call to open will fail.
        exc.printStackTrace();
        continue;
    }
}
````
##### Common (Usage of SharedMemory):
Writing:
````
//Set the entire shared memory to 0.
memory.set(0, (byte)0, memory.getSharedMemorySize());
//Write "Hello World!" to the start of the shared memory.
memory.write(0, "Hello World!".getBytes());

//Close the shared memory
memory.close();
````
Reading:
````
//Buffer big enough to fit "Hello World!"
byte[] buffer = new byte[12];
//Read the first 12 bytes from the shared memory back into a buffer.
memory.read(0, buffer, 0, 12);
//Should print Hello World!
System.out.println(new String(buffer));

//Close the shared memory
memory.close();
````
Interrupts (requires ivshmem-doorbell):
````
//This only needs to be called once after opening the shared memory.
//I recommend storing the Executor in a static variable. This may spawn 0 to 2 Threads.
memory.startNecessaryThreads(Executors.newCachedThreadPool());

//Register a new interrupt service routine to listen for all incomming interrupts on vector 0.
memory.registerInterruptServiceRoutine(0, new InterruptServiceRoutine() {
    @Override
    public void onInterrupt(int vector) {
        System.out.println("Interrupt! Vector: " + vector);
    }
});

//Print out my peer id. A peer id identifies and application when using ivshmem-doorbell.
System.out.println(memory.getOwnPeerID());

//This needs to be communicated, for example by reserving a special address inside
//the Shared Memory to communicate this. The Linux ivshmem-doorbell client may call
//memory.getPeers(); to enumerate all connected peers. The Windows QEMU PCI driver does not support this.
int tempOtherPeerID = 1;

//Send interrupt on interrupt vector 0 to peer id.
memory.sendInterrupt(1,0);
````
## Changelog
###### 1.0
Inital release
###### 1.1
1. Fixed a bug that prevented a call to "close" from actually closing the file descriptors and freeing allocated native memory.
2. Moved spin/spinAndSet to native code for better performance (less JNI calls)
3. Added more "convenience" methods for spin/spinAndSet when calling without a timeout.
4. Added support for i386 (x86 32 bit) jvms on linux(guest+host) and windows(guest).
5. Fixed a bug with compareAndSet (16 bytes CMPXCHG16B) where unaligned memory access will cause a segmentation fault and crash the jvm.