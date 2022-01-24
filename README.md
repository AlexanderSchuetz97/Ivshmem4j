# Ivshmem4j Readme

Ivshmem4j is a Java library to use QEMU ivshmem (inter virtual machine shared memory) from a Java application running
inside a JVM.

The primary purpose of Ivshmem4j is to facilitate fast communication using ivshmem between applications whose JVM's are
running inside different QEMU virtual machines.

Ivshmem4j provides full support for msi/software-interrupts offered by ivshmem-doorbell.

## License

Ivshmem4j is released under the GNU Lesser General Public License Version 3. <br>A copy of the GNU Lesser General Public
License Version 3 can be found in the COPYING file.<br>

## Maven

````
<dependency>
    <groupId>io.github.alexanderschuetz97</groupId>
    <artifactId>ivshmem4j</artifactId>
    <version>1.2</version>
</dependency>
````

## Supported operating systems

##### Host:

* Linux (amd64/i386)

##### Guest:

* Windows (amd64/i386)
* Linux (amd64/i386)

##### Limitations of Linux guests:

Since there is no stable linux kernel module to interact with the emulated ivshmem pci device and since its is not possible to receive interrupts without one, interrupts are not supported on Linux guests.
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
##### Linux Host(ivshmem-plain):
````
//Create or open a shared memory file at "/dev/shm/test" 
//Behavior for file descriptors that are not on tempfs/ramfs is undefined as Ivshmem4j never calls msync.
//QEMU ivshmem-plain uses either /dev/shm or hubgelbtfs (not supported by Ivshmem4j)
IvshmemMemory memory = Ivshmem.plain("/dev/shm/test", 64);
````
##### Linux Host (ivshmem-doorbell):

````
//This requires running a ivshmem-server at "/tmp/test" which is part of the QEMU Project.
//ivshmem-server should already be installed on your system if you use it to run a QEMU VM.
//The second parameter is how long the 
IvshmemMemory tempClient = Ivshmem.doorbell("/tmp/test", 5000);
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
IvshmemMemory memory = Ivshmem.plain("/sys/bus/pci/devices/0000:00:0e.0/resource2_wc");
````
##### Windows (ivshmem-plain and ivshmem-doorbell):

````
//Enumerates all Windows Ivshmem devices. QEMU supports multiple shared memories per virtual machine.
Collection<WindowsIvshmemPCIDevice> devices = Ivshmem.windowsListPCI();

IvshmemMemory memory = null;
for (WindowsIvshmemPCIDevice device : devices) {
    //Print the device size (in bytes) and device name. 
    //The Device name contains the PCI Bus ID which can be set in QEMU.
    System.out.println(device.getSize() + " " + device.getName());
    try {
        //Try to open the device.
        memory = Ivshmem.windowsPCI(device);
        System.out.println("Success!");
        break;
    } catch (IvshmemException exc) {
        //If a device is already in use by another process the call to open will fail.
        exc.printStackTrace();
        continue;
    }
}
````

##### Common (Usage of SharedMemory):

Setup:

````
IvshmemMemory shmem = //See above
NativeMemory memory = shmem.getMemory();
````

Writing:

````
//Set the entire shared memory to 0.
memory.set(0, (byte)0, memory.size());
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
//Register a new interrupt service routine to listen for all incomming interrupts on vector 0.
shmem.registerInterruptServiceRoutine(0, new InterruptServiceRoutine() {
    @Override
    public void onInterrupt(int vector) {
        System.out.println("Interrupt! Vector: " + vector);
    }
});

//Print out my peer id. A peer id identifies and application when using ivshmem-doorbell.
System.out.println(shmem.getOwnPeerID());

//This needs to be communicated, for example by reserving a special address inside
//the Shared Memory to communicate this. The Linux host ivshmem-doorbell client may call
//shmem.getPeers(); to enumerate all connected peers. The Windows QEMU PCI driver does not support this.
int tempOtherPeerID = 1;

//Send interrupt on interrupt vector 0 to peer id.
shmem.sendInterrupt(1, 0);
````