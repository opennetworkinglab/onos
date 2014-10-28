/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onlab.nio;

import com.google.common.collect.Lists;
import org.onlab.util.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.ByteChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.String.format;
import static java.lang.System.out;
import static org.onlab.util.Tools.delay;
import static org.onlab.util.Tools.namedThreads;

/**
 * Auxiliary test fixture to measure speed of NIO-based channels.
 */
public class IOLoopTestServer {

    private static Logger log = LoggerFactory.getLogger(IOLoopTestServer.class);

    private static final int PRUNE_FREQUENCY = 1000;

    static final int PORT = 9876;
    static final long TIMEOUT = 1000;

    static final boolean SO_NO_DELAY = false;
    static final int SO_SEND_BUFFER_SIZE = 128 * 1024;
    static final int SO_RCV_BUFFER_SIZE = 128 * 1024;

    static final DecimalFormat FORMAT = new DecimalFormat("#,##0");

    private final AcceptorLoop aloop;
    private final ExecutorService apool = Executors.newSingleThreadExecutor(namedThreads("accept"));

    private final List<CustomIOLoop> iloops = new ArrayList<>();
    private final ExecutorService ipool;

    private final int workerCount;
    private final int msgLength;
    private int lastWorker = -1;

    Counter messages;
    Counter bytes;

    /**
     * Main entry point to launch the server.
     *
     * @param args command-line arguments
     * @throws java.io.IOException if unable to crate IO loops
     */
    public static void main(String[] args) throws IOException {
        startStandalone(args);
        System.exit(0);
    }

    /**
     * Starts a standalone IO loop test server.
     *
     * @param args command-line arguments
     */
    public static void startStandalone(String[] args) throws IOException {
        InetAddress ip = InetAddress.getByName(args.length > 0 ? args[0] : "127.0.0.1");
        int wc = args.length > 1 ? Integer.parseInt(args[1]) : 6;
        int ml = args.length > 2 ? Integer.parseInt(args[2]) : 128;

        log.info("Setting up the server with {} workers, {} byte messages on {}... ",
                 wc, ml, ip);
        IOLoopTestServer server = new IOLoopTestServer(ip, wc, ml, PORT);
        server.start();

        // Start pruning clients and keep going until their number goes to 0.
        int remaining = -1;
        while (remaining == -1 || remaining > 0) {
            delay(PRUNE_FREQUENCY);
            int r = server.prune();
            remaining = remaining == -1 && r == 0 ? remaining : r;
        }
        server.stop();
    }

    /**
     * Creates a speed server.
     *
     * @param ip   optional ip of the adapter where to bind
     * @param wc   worker count
     * @param ml   message length in bytes
     * @param port listen port
     * @throws java.io.IOException if unable to create IO loops
     */
    public IOLoopTestServer(InetAddress ip, int wc, int ml, int port) throws IOException {
        this.workerCount = wc;
        this.msgLength = ml;
        this.ipool = Executors.newFixedThreadPool(workerCount, namedThreads("io-loop"));

        this.aloop = new CustomAcceptLoop(new InetSocketAddress(ip, port));
        for (int i = 0; i < workerCount; i++) {
            iloops.add(new CustomIOLoop());
        }
    }

    /**
     * Start the server IO loops and kicks off throughput tracking.
     */
    public void start() {
        messages = new Counter();
        bytes = new Counter();

        for (CustomIOLoop l : iloops) {
            ipool.execute(l);
        }
        apool.execute(aloop);

        for (CustomIOLoop l : iloops) {
            l.awaitStart(TIMEOUT);
        }
        aloop.awaitStart(TIMEOUT);
    }

    /**
     * Stop the server IO loops and freezes throughput tracking.
     */
    public void stop() {
        aloop.shutdown();
        for (CustomIOLoop l : iloops) {
            l.shutdown();
        }

        for (CustomIOLoop l : iloops) {
            l.awaitStop(TIMEOUT);
        }
        aloop.awaitStop(TIMEOUT);

        messages.freeze();
        bytes.freeze();
    }

    /**
     * Reports on the accumulated throughput and latency.
     */
    public void report() {
        DecimalFormat f = new DecimalFormat("#,##0");
        out.println(format("Server: %s messages; %s bytes; %s mps; %s MBs",
                           f.format(messages.total()), f.format(bytes.total()),
                           f.format(messages.throughput()),
                           f.format(bytes.throughput() / (1024 * msgLength))));
    }

    /**
     * Prunes the IO loops of stale message buffers.
     *
     * @return number of remaining IO loops among all workers.
     */
    public int prune() {
        int count = 0;
        for (CustomIOLoop l : iloops) {
            count += l.pruneStaleStreams();
        }
        return count;
    }

    // Get the next worker to which a client should be assigned
    private synchronized CustomIOLoop nextWorker() {
        lastWorker = (lastWorker + 1) % workerCount;
        return iloops.get(lastWorker);
    }

    // Loop for transfer of fixed-length messages
    private class CustomIOLoop extends IOLoop<TestMessage, TestMessageStream> {

        public CustomIOLoop() throws IOException {
            super(500);
        }

        @Override
        protected TestMessageStream createStream(ByteChannel channel) {
            return new TestMessageStream(msgLength, channel, this);
        }

        @Override
        protected void removeStream(MessageStream<TestMessage> stream) {
            super.removeStream(stream);
            messages.add(stream.messagesIn().total());
            bytes.add(stream.bytesIn().total());
        }

        @Override
        protected void processMessages(List<TestMessage> messages,
                                       MessageStream<TestMessage> stream) {
            try {
                stream.write(createResponses(messages));
            } catch (IOException e) {
                log.error("Unable to echo messages", e);
            }
        }

        private List<TestMessage> createResponses(List<TestMessage> messages) {
            List<TestMessage> responses = Lists.newArrayListWithCapacity(messages.size());
            for (TestMessage message : messages) {
                responses.add(new TestMessage(message.length(), message.requestorTime(),
                                              System.nanoTime(), message.padding()));
            }
            return responses;
        }
    }

    // Loop for accepting client connections
    private class CustomAcceptLoop extends AcceptorLoop {

        public CustomAcceptLoop(SocketAddress address) throws IOException {
            super(500, address);
        }

        @Override
        protected void acceptConnection(ServerSocketChannel channel) throws IOException {
            SocketChannel sc = channel.accept();
            sc.configureBlocking(false);

            Socket so = sc.socket();
            so.setTcpNoDelay(SO_NO_DELAY);
            so.setReceiveBufferSize(SO_RCV_BUFFER_SIZE);
            so.setSendBufferSize(SO_SEND_BUFFER_SIZE);

            nextWorker().acceptStream(sc);
            log.info("Connected client");
        }
    }

}
