# Ivshmem4j Readme
Ivshmem4j is a Java library to use QEMU ivshmem (inter virtual machine shared memory) from a Java application running inside a JVM.

The primary purpose of Ivshmem4j is to facilitate fast communication using ivshmem between applications whose JVM's are running inside different QEMU virtual machines.

Ivshmem4j provides full support for msi/software-interrupts offered by ivshmem-doorbell. <br>
Additionally Ivshmem4j provides some useful features such as:
* Atomic numbers (like those in the java.util.concurrent.atomic package) that reside in shared memory  
* Implementation of java.util.concurrent.locks.Lock that facilitates synchronisation between applications.
* Implementation of InputStream/OutputStream using a Ring Buffer that resides in shared memory for communication between 2 applications.
* Performing various atmoic operations (xadd / xchg / cmpxchg / cmpxchg16b) on Shared Memory that are not available when using RandomAccessFile.
##License
Ivshmem4j is released under the GNU General Public License Version 3. <br>A copy of the GNU General Public License Version 3 can be found in the COPYING file.<br>


The file "mvnw" is part of the maven-wrapper project, released under the Apache License Version 2.<br>
See https://github.com/takari/maven-wrapper for more information regarding maven-wrapper.
##Supported Platforms
#####Host:
* Linux (amd64)
#####Guest:
* Windows (amd64)

Support for linux guests is planned but currently not yet implemented.
##Building / Installation
####Windows:
Building Ivshmem4j on Windows is currently not possible.
####Linux:
Requirements:
* mingw-cross
* gcc
* make
* bash
* linux amd64 JDK 7 or greater
* windows amd64 JDK 7 or greater (only for headers)

JDK 7 (Oracle), 8 (OpenJDK) and 11 (OpenJDK) were tested.

On Ubuntu all requirements can be installed by running:
````
sudo apt-get install build-essential gcc-mingw-w64 openjdk-8-jdk
````
Adjust for desired JDK version.

To get the Windows JDK either copy the JDK home directory from a windows installation or use other means 
(such as msiextract or unzip) to extract the JDK home directory from a windows setup file.

It is not required to run anything using the windows JDK. The only relevant files for Ivshmem4j 
are inside the "include" folder. 
It is recommended but not required to use the same JDK version for windows and linux because the 
JNI headers for windows are generated using the linux JDK's javah command to avoid having to run a windows binary.

Building:
* clone the git repository
* run: 
````
bash compile_native.sh
````
* edit the newly created config.sh file and enter the paths for your windows JDK home and linux JDK home.
* run: 
````
./mvnw -Dmaven.test.skip=true clean package
````
The compiled jar file for Ivshmem4j should now be located inside the target folder.<br>
To install Ivshmem4j to a local maven repository you may instead/additionally run:
````
./mvnw -Dmaven.test.skip=true clean install
````
##Runtime Dependencies
Windows:
* Ivshmem device driver which is contained in the virtio driver
https://fedorapeople.org/groups/virt/virtio-win/direct-downloads/upstream-virtio/
(Use version 0.1-161 or later)

Linux:
* No dependencies required
##Simple Examples
#####Common (loading native libraries):
````
//Ivshmem4j does not load the native from the java.library.path to avoid introducing additional constraints
//to your application. You may use this call to let Ivshmem4j handle loading the native libraries
//but you may also decide to load them yourself by other means if this is more suitable for your application.
//This only needs to be called once. Any repeaded calls are NOOPs.
NativeLibraryLoaderHelper.loadNativeLibraries();
````
#####Linux Host(ivshmem-plain):
````
//Create or open a shared memory file at "/dev/shm/test" 
//Behavior for file descriptors that are not on tempfs/ramfs is undefined as Ivshmem4j never calls msync.
//QEMU ivshmem-plain uses either /dev/shm or hubgelbtfs (not supported by Ivshmem4j)
SharedMemory memory = LinuxMappedFileSharedMemory.create("/dev/shm/test", 64);
````
#####Linux Host (ivshmem-doorbell):
````
//This requires running a ivshmem-server at "/tmp/test" which is part of the QEMU Project.
//ivshmem-server should already be installed on your system if you use it to run a QEMU VM.
SharedMemory tempClient = IvshmemLinuxClient.create("/tmp/test");
````
#####Windows (ivshmem-plain and ivshmem-doorbell):
````
//Enumerates all windows Ivshmem devices. QEMU supports multiple shared memories per virtual machine.
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
#####Common (Usage of SharedMemory):
Writing:
````
//Set the entire shared memory to 0.
memory.memset(0, (byte)0, memory.getSharedMemorySize());
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