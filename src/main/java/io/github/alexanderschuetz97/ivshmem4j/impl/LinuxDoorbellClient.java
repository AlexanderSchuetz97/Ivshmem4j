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

import io.github.alexanderschuetz97.ivshmem4j.api.InterruptServiceRoutine;
import io.github.alexanderschuetz97.ivshmem4j.api.IvshmemException;
import io.github.alexanderschuetz97.ivshmem4j.api.IvshmemMemory;
import io.github.alexanderschuetz97.ivshmem4j.api.PeerConnectionListener;
import io.github.alexanderschuetz97.nativeutils.api.LinuxConst;
import io.github.alexanderschuetz97.nativeutils.api.LinuxNativeUtil;
import io.github.alexanderschuetz97.nativeutils.api.NativeMemory;
import io.github.alexanderschuetz97.nativeutils.api.NativeUtils;
import io.github.alexanderschuetz97.nativeutils.api.exceptions.UnknownNativeErrorException;
import io.github.alexanderschuetz97.nativeutils.api.structs.Cmsghdr;
import io.github.alexanderschuetz97.nativeutils.api.structs.Iovec;
import io.github.alexanderschuetz97.nativeutils.api.structs.Msghdr;
import io.github.alexanderschuetz97.nativeutils.api.structs.PollFD;
import io.github.alexanderschuetz97.nativeutils.api.structs.Stat;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LinuxDoorbellClient implements IvshmemMemory {

    private static final byte[] INTERRUPT_PACKET = new byte[]{1, 0, 0, 0, 0, 0, 0, 0};

    private volatile boolean closed;
    private final LinuxNativeUtil nativeUtil = NativeUtils.getLinuxUtil();
    private int sock;
    private int ownPeerID;
    private int[] ownVectors;
    private int memFD;
    private long memPtr;
    private long memSize;
    private NativeMemory memory;
    private final ReentrantReadWriteLock.ReadLock readLock;
    private final ReentrantReadWriteLock.WriteLock writeLock;
    private final Map<Integer, List<Integer>> otherVectors = new ConcurrentHashMap<>();
    private Collection<InterruptServiceRoutine>[] isrs;
    private final Collection<Integer> readOnlyOtherPeers = Collections.unmodifiableCollection(otherVectors.keySet());
    private final Thread.UncaughtExceptionHandler handler;


    private static final long MAGIC_NUMBER = littleEndian(new byte[]{-1, -1, -1, -1, -1, -1, -1, -1,});


    private int getFD(Msghdr msghdr) {
        Collection<Cmsghdr> cms = nativeUtil.parseCMSG_HDR(msghdr.getMsg_control(), msghdr.getMsg_controllen());
        for (Cmsghdr hdr : cms) {
            if (hdr.getType() == LinuxConst.SCM_RIGHTS && hdr.getLevel() != LinuxConst.SOL_SOCKET) {
                continue;
            }

            byte[] payload = hdr.getPayload();
            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                return (int) littleEndian(payload);
            } else {
                return (int) bigEndian(payload);
            }
        }

        return -1;
    }

    private static long bigEndian(byte[] buf) {
        long l = 0;
        for (int i = 0; i < buf.length; i++) {
            l <<= 8;
            l += buf[i] & 0xff;
        }

        return l;
    }

    private static long littleEndian(byte[] buf) {
        long l = 0;
        for (int i = buf.length - 1; i >= 0; i--) {
            l <<= 8;
            l += buf[i] & 0xff;
        }

        return l;
    }


    public static void main(String[] args) throws Throwable {
        try {
            ExecutorService exs = Executors.newCachedThreadPool();
            IvshmemMemory mem = new LinuxDoorbellClient("/tmp/shmemsock", 5000, exs, StdErrHandler.INSTANCE);


            System.out.println("OP " + mem.getOwnPeerID());

            NativeMemory nativeMemory = mem.getMemory();
            System.out.println("SIZE " + nativeMemory.size());
            System.out.println("VECS " + mem.getOwnVectors());

            System.out.println(nativeMemory.readInt(0));
            nativeMemory.write(0, 128);

            Thread.sleep(2000);
            mem.close();
            System.out.println("DONE");


            exs.shutdownNow();
        } catch (Throwable exc) {
            exc.printStackTrace();
        }

    }

    private void checkClosed() {
        if (isClosed()) {
            throw new IllegalStateException("closed");
        }
    }

    private Msghdr initalPacket;

    public LinuxDoorbellClient(String path, long grace, Executor executor, Thread.UncaughtExceptionHandler handler) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(handler);
        if (grace < 0) {
            throw new IllegalArgumentException("grace");
        }

        this.handler = handler;

        boolean succ = false;

        try {
            sock = nativeUtil.socket(LinuxConst.AF_UNIX, LinuxConst.SOCK_STREAM, 0);
            nativeUtil.connect(sock, nativeUtil.to_sockaddr_un(path));
            nativeUtil.setsockopt(sock, LinuxConst.SOL_SOCKET, LinuxConst.SO_RCVTIMEO, nativeUtil.to_struct_timeval(grace));
            Iovec buf = new Iovec(8);
            byte[] bufBytes = buf.getPayload();
            Msghdr msg = new Msghdr(new Iovec[]{buf}, new byte[512]);
            int i = 0;

            i = nativeUtil.recvmsg(sock, msg, 0);
            if (i != 8) {
                throw new IvshmemException("wrong packet size " + i);
            }

            if (littleEndian(bufBytes) != 0) {
                throw new IvshmemException("inital packet is not 0");
            }

            i = nativeUtil.recvmsg(sock, msg, 0);
            if (i != 8) {
                throw new IvshmemException("wrong packet size " + i);
            }

            long ownPeerID = littleEndian(bufBytes);
            if (ownPeerID < 0 || ownPeerID > 0xffff) {
                throw new IvshmemException("invalid own peer id " + ownPeerID);
            }
            this.ownPeerID = (int) ownPeerID;

            i = nativeUtil.recvmsg(sock, msg, 0);
            if (i != 8) {
                throw new IvshmemException("wrong packet size " + i);
            }

            long magic = littleEndian(bufBytes);
            if (magic != MAGIC_NUMBER) {
                throw new IvshmemException("wrong magic number " + magic);
            }

            memFD = getFD(msg);
            if (memFD == -1) {
                throw new IvshmemException("cmsg hdr of magic number packet did not contain file descriptor for mmap");
            }

            Stat stat = nativeUtil.fstat(memFD);
            memSize = stat.getSize();

            long ptr = nativeUtil.mmap(memFD, memSize, LinuxConst.MAP_SHARED, true, true, 0);
            memory = nativeUtil.pointer(ptr, memSize, new IvshmemPointerHandler(this));
            this.readLock = memory.readLock();
            this.writeLock = memory.writeLock();

            List<Integer> ownInterrupts = new ArrayList<>();
            boolean gotOwnVectors = false;
            //PEER FDS
            while (true) {
                i = nativeUtil.recvmsg(sock, msg, 0);
                if (i == 0) {
                    break;
                }

                if (i != 8) {
                    throw new IvshmemException("wrong packet size " + i);
                }

                long peer = littleEndian(bufBytes);
                if (peer != ownPeerID) {
                    if (gotOwnVectors) {
                        initalPacket = msg;
                        break;
                    }

                    int fd = getFD(msg);
                    if (fd == -1) {
                        throw new IvshmemException("received disconnect notification before connection was established");
                    }

                    List<Integer> vecs = otherVectors.get((int) peer);
                    if (vecs == null) {
                        vecs = new ArrayList<>();
                        otherVectors.put((int) peer, vecs);
                    }

                    vecs.add(fd);
                    continue;
                }

                int fd = getFD(msg);
                if (fd == -1) {
                    throw new IvshmemException("received disconnect notification before connection was established");
                }

                gotOwnVectors = true;
                ownInterrupts.add(fd);
            }

            nativeUtil.setsockopt(sock, LinuxConst.SOL_SOCKET, LinuxConst.SO_RCVTIMEO, nativeUtil.to_struct_timeval(2000));

            isrs = new Collection[ownInterrupts.size()];
            this.ownVectors = new int[ownInterrupts.size()];
            for (int j = 0; j < ownVectors.length; j++) {
                this.ownVectors[j] = ownInterrupts.get(j);
                isrs[j] = Collections.synchronizedSet(new LinkedHashSet<InterruptServiceRoutine>());
            }


            succ = true;
        } catch (IOException e) {
            throw new IvshmemException(e);
        } catch (UnknownNativeErrorException e) {
            throw new IvshmemException(nativeUtil.strerror_r((int) e.getCode()));
        } finally {
            if (!succ) {
                closeInternal();
            }
        }

        try {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    listenForNewConnections();
                }
            });

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    receiveInterrupts();
                }
            });

        } catch (Exception exc) {
            close();
            throw new IvshmemException(exc);
        }

    }

    @Override
    public NativeMemory getMemory() {
        return Objects.requireNonNull(memory);
    }

    @Override
    public boolean hasOwnPeerID() {
        return true;
    }

    @Override
    public int getOwnPeerID() {
        return ownPeerID;
    }

    @Override
    public boolean knowsOtherPeers() {
        readLock.lock();
        try {
            checkClosed();
            return ownVectors.length > 0;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isOtherPeerConnected(int aPeerId) {
        return otherVectors.containsKey(aPeerId);
    }

    @Override
    public Collection<Integer> getPeers() {
        return readOnlyOtherPeers;
    }

    @Override
    public boolean knowsOtherPeerVectors() {
        return true;
    }

    @Override
    public int getVectors(int peer) {
        readLock.lock();
        try {
            checkClosed();
            List<Integer> vecs = otherVectors.get(peer);
            if (vecs == null) {
                throw new IvshmemException("invalid peer");
            }

            return vecs.size();
        } finally {
            readLock.unlock();
        }

    }

    @Override
    public int getOwnVectors() {
        readLock.lock();
        try {
            checkClosed();
            return ownVectors.length;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isVectorValid(int vector) {
        readLock.lock();
        try {
            checkClosed();
            return ownVectors.length > vector;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        //MUST set here or will starve
        closed = true;
        NativeMemory mem = memory;
        if (mem != null) {
            mem.close();
        }
        writeLock.lock();
        try {
            closeInternal();
        } finally {
            writeLock.unlock();
        }

    }

    protected void closeInternal() {
        closed = true;
        if (initalPacket != null) {
            int fd = getFD(initalPacket);
            if (fd != -1) {
                try {
                    nativeUtil.close(fd);
                } catch (IOException e) {
                    handleUncaught(e);
                }
            }

            initalPacket = null;
        }

        if (memory != null && memory.isValid()) {
            memory.close();
            memory = null;
        }

        if (memPtr != 0) {
            long pp = memPtr;
            memPtr = 0;
            try {
                nativeUtil.munmap(pp, memSize);
            } catch (UnknownNativeErrorException e) {
                handleUncaught(e);
            }
        }

        if (ownVectors != null) {
            for (int fd : ownVectors) {
                try {
                    nativeUtil.close(fd);
                } catch (IOException e) {
                    handleUncaught(e);
                }
            }

            ownVectors = null;
        }

        for (List<Integer> fds : otherVectors.values()) {
            for (int fd : fds) {
                try {
                    nativeUtil.close(fd);
                } catch (IOException e) {
                    handleUncaught(e);
                }
            }
        }


        otherVectors.clear();

        if (memFD != -1) {
            try {
                nativeUtil.close(memFD);
            } catch (IOException e) {
                handleUncaught(e);
            }
            memFD = -1;
        }

        if (sock != -1) {
            try {
                nativeUtil.close(sock);
            } catch (IOException e) {
                handleUncaught(e);
            }
            sock = -1;
        }
    }


    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public boolean supportsInterrupts() {
        readLock.lock();
        try {
            checkClosed();
            return ownVectors.length > 0;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void sendInterrupt(int aPeer, int aVector) {
        if (aVector < 0) {
            throw new IvshmemException("invalid vector");
        }

        readLock.lock();
        try {
            checkClosed();
            if (aPeer == ownPeerID) {
                throw new IvshmemException("cant self interrupt");
            }

            List<Integer> vec = otherVectors.get(aPeer);
            if (vec == null) {
                throw new IvshmemException("invalid peer");
            }

            int fd;
            synchronized (vec) {
                if (vec.size() < aVector) {
                    throw new IvshmemException("invalid vector");
                }

                fd = vec.get(aVector);
            }

            try {
                nativeUtil.write(fd, INTERRUPT_PACKET, 0, INTERRUPT_PACKET.length);
            } catch (UnknownNativeErrorException e) {
                throw new IvshmemException(nativeUtil.strerror_r((int) e.getCode()));
            } catch (IOException e) {
                throw new IvshmemException(e);
            }

        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void registerInterruptServiceRoutine(int aVector, InterruptServiceRoutine isr) {
        if (aVector < 0 || aVector > isrs.length) {
            throw new IvshmemException("invalid vector");
        }

        isrs[aVector].add(Objects.requireNonNull(isr));
    }

    @Override
    public void removeInterruptServiceRoutine(int aVector, InterruptServiceRoutine isr) {
        if (aVector < 0 || aVector > isrs.length) {
            throw new IvshmemException("invalid vector");
        }

        isrs[aVector].remove(isr);
    }

    protected void receiveInterrupts() {
        byte[] buf = new byte[8];
        if (ownVectors.length == 0) {
            return;
        }
        try {
            PollFD[] pfds = new PollFD[ownVectors.length];
            for (int i = 0; i < pfds.length; i++) {
                pfds[i] = new PollFD(ownVectors[i], EnumSet.of(PollFD.PollEvent.POLLIN));
            }

            int toIntIdx = 0;
            int[] toInt = new int[pfds.length];


            while (!isClosed()) {
                try {
                    readLock.lock();
                    if (isClosed()) {
                        return;
                    }
                    int res = nativeUtil.poll(pfds, 2000);
                    if (res == 0) {
                        continue;
                    }

                    toIntIdx = 0;
                    for (int i = 0; i < pfds.length; i++) {
                        if (!pfds[i].test(PollFD.PollEvent.POLLIN)) {
                            continue;
                        }

                        res--;
                        int x = 0;
                        try {
                            x = nativeUtil.read(ownVectors[i], buf, 0, 8);
                        } catch (IOException e) {
                            throw new IvshmemException("failed to read interrupt", e);
                        }

                        if (x != 8) {
                            continue;
                        }

                        toInt[toIntIdx++] = i;
                    }

                    if (res != 0) {
                        //POLLERR must be set
                        throw new IvshmemException("poll interrupt failed");
                    }
                } finally {
                    readLock.unlock();
                }

                for (int i = 0; i < toIntIdx; i++) {
                    Collection<InterruptServiceRoutine> isrs = this.isrs[i];
                    synchronized (isrs) {
                        for (InterruptServiceRoutine isr : isrs) {
                            try {
                                isr.onInterrupt(i);
                            } catch (Throwable e) {
                                handleUncaught(e);
                            }
                        }
                    }
                }
            }
        } catch (UnknownNativeErrorException err) {
            handler.uncaughtException(Thread.currentThread(), err);
        } finally {
            close();
        }

    }

    protected void listenForNewConnections() {
        readLock.lock();
        try {
            if (initalPacket != null) {
                processPacket(initalPacket);
                initalPacket = null;
            }
        } finally {
            readLock.unlock();
        }

        Msghdr msg = new Msghdr(new Iovec[]{new Iovec(new byte[8])}, new byte[512]);
        while (!isClosed()) {
            try {
                readLock.lock();

                if (isClosed()) {
                    return;
                }

                int i = nativeUtil.recvmsg(sock, msg, 0);
                if (i == 0) {
                    continue;
                }

                if (i != 8) {
                    if (i == -1) {
                        throw new IvshmemException("socket closed by server");
                    }
                    throw new IvshmemException("wrong packet size " + i);
                }

                processPacket(msg);
            } finally {
                readLock.unlock();
            }
        }
    }

    protected void processPacket(Msghdr msg) {
        if (msg == null || isClosed()) {
            return;
        }

        long peer = littleEndian(msg.getMsg_iov()[0].getPayload());
        if (peer < 0 || peer > 0xffff) {
            throw new IvshmemException("invalid peer id " + peer);
        }

        if (peer == ownPeerID) {
            throw new IvshmemException("received late info for own peer");
        }

        int mPeer = (int) peer;

        int fd = getFD(msg);
        //IS THIS A DC?
        if (fd == -1) {
            List<Integer> vecs = otherVectors.remove(mPeer);
            if (vecs == null) {
                //NO VECS???
                return;
            }

            for (int vfd : vecs) {
                try {
                    nativeUtil.close(vfd);
                } catch (IOException e) {
                    handleUncaught(e);
                }
            }

            synchronized (peerConnectionListeners) {
                for (PeerConnectionListener pcl : peerConnectionListeners) {
                    pcl.onDisconnect(mPeer);
                }
            }

            return;
        }

        List<Integer> vecs = otherVectors.get(mPeer);
        if (vecs == null) {
            vecs = Collections.synchronizedList(new ArrayList<Integer>());
            vecs.add(fd);
            otherVectors.put(mPeer, vecs);

            synchronized (peerConnectionListeners) {
                for (PeerConnectionListener pcl : peerConnectionListeners) {
                    pcl.onConnect(mPeer, 1);
                }
            }

            return;
        }

        vecs.add(fd);
        int vSize = vecs.size();
        if (vSize > ownVectors.length) {
            throw new IvshmemException("too many interrupts for peer " + mPeer);
        }

        synchronized (peerConnectionListeners) {
            for (PeerConnectionListener pcl : peerConnectionListeners) {
                pcl.onConnect(mPeer, vSize);
            }
        }
    }

    protected void handleUncaught(Throwable t) {
        handler.uncaughtException(Thread.currentThread(), t);
    }

    private final Set<PeerConnectionListener> peerConnectionListeners = Collections.synchronizedSet(new LinkedHashSet<PeerConnectionListener>());

    @Override
    public void registerPeerConnectionListener(PeerConnectionListener listener) {
        checkClosed();
        peerConnectionListeners.add(listener);
    }

    @Override
    public void removePeerConnectionListener(PeerConnectionListener listener) {
        peerConnectionListeners.remove(listener);
    }

}
