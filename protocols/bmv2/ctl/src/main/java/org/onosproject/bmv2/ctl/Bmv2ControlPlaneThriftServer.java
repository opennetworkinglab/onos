/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.bmv2.ctl;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.thrift.TProcessor;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TNonblockingSocket;
import org.apache.thrift.transport.TNonblockingTransport;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * A Thrift TThreadedSelectorServer that keeps track of the clients' IP address.
 */
final class Bmv2ControlPlaneThriftServer extends TThreadedSelectorServer {

    private static final int MAX_WORKER_THREADS = 20;
    private static final int MAX_SELECTOR_THREADS = 4;
    private static final int ACCEPT_QUEUE_LEN = 8;

    private final Map<TTransport, InetAddress> clientAddresses = Maps.newConcurrentMap();
    private final Set<TrackingSelectorThread> selectorThreads = Sets.newHashSet();

    private AcceptThread acceptThread;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Creates a new server.
     *
     * @param port            a listening port
     * @param processor       a processor
     * @param executorService an executor service
     * @throws TTransportException
     */
    public Bmv2ControlPlaneThriftServer(int port, TProcessor processor, ExecutorService executorService)
            throws TTransportException {
        super(new TThreadedSelectorServer.Args(new TNonblockingServerSocket(port))
                      .workerThreads(MAX_WORKER_THREADS)
                      .selectorThreads(MAX_SELECTOR_THREADS)
                      .acceptQueueSizePerThread(ACCEPT_QUEUE_LEN)
                      .executorService(executorService)
                      .processor(processor));
    }

    /**
     * Returns the IP address of the client associated with the given input framed transport.
     *
     * @param inputTransport a framed transport instance
     * @return the IP address of the client or null
     */
    InetAddress getClientAddress(TFramedTransport inputTransport) {
        return clientAddresses.get(inputTransport);
    }

    @Override
    protected boolean startThreads() {
        try {
            for (int i = 0; i < MAX_SELECTOR_THREADS; ++i) {
                selectorThreads.add(new TrackingSelectorThread(ACCEPT_QUEUE_LEN));
            }
            acceptThread = new AcceptThread((TNonblockingServerTransport) serverTransport_,
                                            createSelectorThreadLoadBalancer(selectorThreads));
            selectorThreads.forEach(Thread::start);
            acceptThread.start();
            return true;
        } catch (IOException e) {
            log.error("Failed to start threads!", e);
            return false;
        }
    }

    @Override
    protected void joinThreads() throws InterruptedException {
        // Wait until the io threads exit.
        acceptThread.join();
        for (TThreadedSelectorServer.SelectorThread thread : selectorThreads) {
            thread.join();
        }
    }

    @Override
    public void stop() {
        stopped_ = true;
        // Stop queuing connect attempts asap.
        stopListening();
        if (acceptThread != null) {
            acceptThread.wakeupSelector();
        }
        if (selectorThreads != null) {
            selectorThreads.stream()
                    .filter(thread -> thread != null)
                    .forEach(TrackingSelectorThread::wakeupSelector);
        }
    }

    private class TrackingSelectorThread extends TThreadedSelectorServer.SelectorThread {

        TrackingSelectorThread(int maxPendingAccepts) throws IOException {
            super(maxPendingAccepts);
        }

        @Override
        protected FrameBuffer createFrameBuffer(TNonblockingTransport trans, SelectionKey selectionKey,
                                                AbstractSelectThread selectThread) {
            TrackingFrameBuffer frameBuffer = new TrackingFrameBuffer(trans, selectionKey, selectThread);
            if (trans instanceof TNonblockingSocket) {
                try {
                    SocketChannel socketChannel = ((TNonblockingSocket) trans).getSocketChannel();
                    InetAddress addr = ((InetSocketAddress) socketChannel.getRemoteAddress()).getAddress();
                    clientAddresses.put(frameBuffer.getInputFramedTransport(), addr);
                } catch (IOException e) {
                    log.warn("Exception while tracking client address", e);
                    clientAddresses.remove(frameBuffer.getInputFramedTransport());
                }
            } else {
                log.warn("Unknown TNonblockingTransport instance: {}", trans.getClass().getName());
                clientAddresses.remove(frameBuffer.getInputFramedTransport());
            }
            return frameBuffer;
        }
    }

    private class TrackingFrameBuffer extends FrameBuffer {

        TrackingFrameBuffer(TNonblockingTransport trans, SelectionKey selectionKey,
                            AbstractSelectThread selectThread) {
            super(trans, selectionKey, selectThread);
        }

        TTransport getInputFramedTransport() {
            return this.inTrans_;
        }
    }
}
