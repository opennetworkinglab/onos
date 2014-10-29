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
import java.net.SocketAddress;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;
import static java.lang.System.nanoTime;
import static java.lang.System.out;
import static org.onlab.nio.IOLoopTestServer.PORT;
import static org.onlab.util.Tools.delay;
import static org.onlab.util.Tools.namedThreads;

/**
 * Auxiliary test fixture to measure speed of NIO-based channels.
 */
public class IOLoopTestClient {

    private static Logger log = LoggerFactory.getLogger(IOLoopTestClient.class);

    private final InetAddress ip;
    private final int port;
    private final int msgCount;
    private final int msgLength;

    private final List<CustomIOLoop> iloops = new ArrayList<>();
    private final ExecutorService ipool;
    private final ExecutorService wpool;

    Counter messages;
    Counter bytes;
    long latencyTotal = 0;
    long latencyCount = 0;


    /**
     * Main entry point to launch the client.
     *
     * @param args command-line arguments
     * @throws java.io.IOException                     if unable to connect to server
     * @throws InterruptedException                    if latch wait gets interrupted
     * @throws java.util.concurrent.ExecutionException if wait gets interrupted
     * @throws java.util.concurrent.TimeoutException   if timeout occurred while waiting for completion
     */
    public static void main(String[] args)
            throws IOException, InterruptedException, ExecutionException, TimeoutException {
        startStandalone(args);

        System.exit(0);
    }

    /**
     * Starts a standalone IO loop test client.
     *
     * @param args command-line arguments
     */
    public static void startStandalone(String[] args)
            throws IOException, InterruptedException, ExecutionException, TimeoutException {
        InetAddress ip = InetAddress.getByName(args.length > 0 ? args[0] : "127.0.0.1");
        int wc = args.length > 1 ? Integer.parseInt(args[1]) : 6;
        int mc = args.length > 2 ? Integer.parseInt(args[2]) : 50 * 1000000;
        int ml = args.length > 3 ? Integer.parseInt(args[3]) : 128;
        int to = args.length > 4 ? Integer.parseInt(args[4]) : 60;

        log.info("Setting up client with {} workers sending {} {}-byte messages to {} server... ",
                 wc, mc, ml, ip);
        IOLoopTestClient client = new IOLoopTestClient(ip, wc, mc, ml, PORT);

        client.start();
        delay(500);

        client.await(to);
        client.report();
    }

    /**
     * Creates a speed client.
     *
     * @param ip   ip address of server
     * @param wc   worker count
     * @param mc   message count to send per client
     * @param ml   message length in bytes
     * @param port socket port
     * @throws java.io.IOException if unable to create IO loops
     */
    public IOLoopTestClient(InetAddress ip, int wc, int mc, int ml, int port) throws IOException {
        this.ip = ip;
        this.port = port;
        this.msgCount = mc;
        this.msgLength = ml;
        this.wpool = Executors.newFixedThreadPool(wc, namedThreads("worker"));
        this.ipool = Executors.newFixedThreadPool(wc, namedThreads("io-loop"));

        for (int i = 0; i < wc; i++) {
            iloops.add(new CustomIOLoop());
        }
    }

    /**
     * Starts the client workers.
     *
     * @throws java.io.IOException if unable to open connection
     */
    public void start() throws IOException {
        messages = new Counter();
        bytes = new Counter();

        // First start up all the IO loops
        for (CustomIOLoop l : iloops) {
            ipool.execute(l);
        }

        // Wait for all of them to get going
        for (CustomIOLoop l : iloops) {
            l.awaitStart(1000);
        }

        // ... and Next open all connections; one-per-loop
        for (CustomIOLoop l : iloops) {
            openConnection(l);
        }
    }


    /**
     * Initiates open connection request and registers the pending socket
     * channel with the given IO loop.
     *
     * @param loop loop with which the channel should be registered
     * @throws java.io.IOException if the socket could not be open or connected
     */
    private void openConnection(CustomIOLoop loop) throws IOException {
        SocketAddress sa = new InetSocketAddress(ip, port);
        SocketChannel ch = SocketChannel.open();
        ch.configureBlocking(false);
        loop.connectStream(ch);
        ch.connect(sa);
    }


    /**
     * Waits for the client workers to complete.
     *
     * @param secs timeout in seconds
     * @throws java.util.concurrent.ExecutionException if execution failed
     * @throws InterruptedException                    if interrupt occurred while waiting
     * @throws java.util.concurrent.TimeoutException   if timeout occurred
     */
    public void await(int secs) throws InterruptedException,
            ExecutionException, TimeoutException {
        for (CustomIOLoop l : iloops) {
            if (l.worker.task != null) {
                l.worker.task.get(secs, TimeUnit.SECONDS);
                latencyTotal += l.latencyTotal;
                latencyCount += l.latencyCount;
            }
        }
        messages.freeze();
        bytes.freeze();
    }

    /**
     * Reports on the accumulated throughput and latency.
     */
    public void report() {
        DecimalFormat f = new DecimalFormat("#,##0");
        out.println(format("Client: %s messages; %s bytes; %s mps; %s MBs; %s ns latency",
                           f.format(messages.total()), f.format(bytes.total()),
                           f.format(messages.throughput()),
                           f.format(bytes.throughput() / (1024 * msgLength)),
                           f.format(latencyTotal / latencyCount)));
    }


    // Loop for transfer of fixed-length messages
    private class CustomIOLoop extends IOLoop<TestMessage, TestMessageStream> {

        Worker worker = new Worker();
        long latencyTotal = 0;
        long latencyCount = 0;


        public CustomIOLoop() throws IOException {
            super(500);
        }


        @Override
        protected TestMessageStream createStream(ByteChannel channel) {
            return new TestMessageStream(msgLength, channel, this);
        }

        @Override
        protected synchronized void removeStream(MessageStream<TestMessage> stream) {
            super.removeStream(stream);
            messages.add(stream.messagesIn().total());
            bytes.add(stream.bytesIn().total());
            stream.messagesOut().reset();
            stream.bytesOut().reset();
        }

        @Override
        protected void processMessages(List<TestMessage> messages,
                                       MessageStream<TestMessage> stream) {
            for (TestMessage message : messages) {
                // TODO: summarize latency data better
                latencyTotal += nanoTime() - message.requestorTime();
                latencyCount++;
            }
            worker.release(messages.size());
        }

        @Override
        protected void connect(SelectionKey key) throws IOException {
            super.connect(key);
            TestMessageStream b = (TestMessageStream) key.attachment();
            Worker w = ((CustomIOLoop) b.loop()).worker;
            w.pump(b);
        }

    }

    /**
     * Auxiliary worker to connect and pump batched messages using blocking I/O.
     */
    private class Worker implements Runnable {

        private static final int BATCH_SIZE = 50;
        private static final int PERMITS = 2 * BATCH_SIZE;

        private TestMessageStream stream;
        private FutureTask<Worker> task;

        // Stuff to throttle pump
        private final Semaphore semaphore = new Semaphore(PERMITS);
        private int msgWritten;

        void pump(TestMessageStream stream) {
            this.stream = stream;
            task = new FutureTask<>(this, this);
            wpool.execute(task);
        }

        @Override
        public void run() {
            try {
                log.info("Worker started...");

                while (msgWritten < msgCount) {
                    int size = Math.min(BATCH_SIZE, msgCount - msgWritten);
                    writeBatch(size);
                    msgWritten += size;
                }

                // Now try to get all the permits back before sending poison pill
                semaphore.acquireUninterruptibly(PERMITS);
                stream.close();

                log.info("Worker done...");

            } catch (IOException e) {
                log.error("Worker unable to perform I/O", e);
            }
        }


        private void writeBatch(int size) throws IOException {
            // Build a batch of messages
            List<TestMessage> batch = Lists.newArrayListWithCapacity(size);
            for (int i = 0; i < size; i++) {
                batch.add(new TestMessage(msgLength, nanoTime(), 0, stream.padding()));
            }
            acquire(size);
            stream.write(batch);
        }


        // Release permits based on the specified number of message credits
        private void release(int permits) {
            semaphore.release(permits);
        }

        // Acquire permit for a single batch
        private void acquire(int permits) {
            semaphore.acquireUninterruptibly(permits);
        }

    }

}
