package org.onlab.nio;

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

import static org.onlab.junit.TestTools.delay;
import static org.onlab.util.Tools.namedThreads;

/**
 * Auxiliary test fixture to measure speed of NIO-based channels.
 */
public class IOLoopClient {

    private static Logger log = LoggerFactory.getLogger(IOLoopClient.class);

    private final InetAddress ip;
    private final int port;
    private final int msgCount;
    private final int msgLength;

    private final List<CustomIOLoop> iloops = new ArrayList<>();
    private final ExecutorService ipool;
    private final ExecutorService wpool;

    Counter messages;
    Counter bytes;

    /**
     * Main entry point to launch the client.
     *
     * @param args command-line arguments
     * @throws IOException          if unable to connect to server
     * @throws InterruptedException if latch wait gets interrupted
     * @throws ExecutionException   if wait gets interrupted
     * @throws TimeoutException     if timeout occurred while waiting for completion
     */
    public static void main(String[] args)
            throws IOException, InterruptedException, ExecutionException, TimeoutException {
        InetAddress ip = InetAddress.getByName(args.length > 0 ? args[0] : "127.0.0.1");
        int wc = args.length > 1 ? Integer.parseInt(args[1]) : 6;
        int mc = args.length > 2 ? Integer.parseInt(args[2]) : 50 * 1000000;
        int ml = args.length > 3 ? Integer.parseInt(args[3]) : 128;
        int to = args.length > 4 ? Integer.parseInt(args[4]) : 30;

        log.info("Setting up client with {} workers sending {} {}-byte messages to {} server... ",
                 wc, mc, ml, ip);
        IOLoopClient sc = new IOLoopClient(ip, wc, mc, ml, IOLoopServer.PORT);

        sc.start();
        delay(2000);

        sc.await(to);
        sc.report();

        System.exit(0);
    }

    /**
     * Creates a speed client.
     *
     * @param ip   ip address of server
     * @param wc   worker count
     * @param mc   message count to send per client
     * @param ml   message length in bytes
     * @param port socket port
     * @throws IOException if unable to create IO loops
     */
    public IOLoopClient(InetAddress ip, int wc, int mc, int ml, int port) throws IOException {
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
     * @throws IOException if unable to open connection
     */
    public void start() throws IOException {
        messages = new Counter();
        bytes = new Counter();

        // First start up all the IO loops
        for (CustomIOLoop l : iloops) {
            ipool.execute(l);
        }

        // Wait for all of them to get going
//        for (CustomIOLoop l : iloops)
//            l.waitForStart(TIMEOUT);

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
     * @throws IOException if the socket could not be open or connected
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
     * @throws ExecutionException   if execution failed
     * @throws InterruptedException if interrupt occurred while waiting
     * @throws TimeoutException     if timeout occurred
     */
    public void await(int secs) throws InterruptedException,
            ExecutionException, TimeoutException {
        for (CustomIOLoop l : iloops) {
            if (l.worker.task != null) {
                l.worker.task.get(secs, TimeUnit.SECONDS);
            }
        }
        messages.freeze();
        bytes.freeze();
    }

    /**
     * Reports on the accumulated throughput trackers.
     */
    public void report() {
        DecimalFormat f = new DecimalFormat("#,##0");
        log.info("{} messages; {} bytes; {} mps; {} Mbs",
                 f.format(messages.total()),
                 f.format(bytes.total()),
                 f.format(messages.throughput()),
                 f.format(bytes.throughput() / (1024 * 128)));
    }


    // Loop for transfer of fixed-length messages
    private class CustomIOLoop extends IOLoop<TestMessage, TestMessageStream> {

        Worker worker = new Worker();

        public CustomIOLoop() throws IOException {
            super(500);
        }


        @Override
        protected TestMessageStream createStream(ByteChannel channel) {
            return new TestMessageStream(msgLength, channel, this);
        }

        @Override
        protected synchronized void removeStream(MessageStream<TestMessage> b) {
            super.removeStream(b);

            messages.add(b.messagesIn().total());
            bytes.add(b.bytesIn().total());
            b.messagesOut().reset();
            b.bytesOut().reset();
//
            log.info("Disconnected client; inbound {} mps, {} Mbps; outbound {} mps, {} Mbps",
                     IOLoopServer.FORMAT.format(b.messagesIn().throughput()),
                     IOLoopServer.FORMAT.format(b.bytesIn().throughput() / (1024 * 128)),
                     IOLoopServer.FORMAT.format(b.messagesOut().throughput()),
                     IOLoopServer.FORMAT.format(b.bytesOut().throughput() / (1024 * 128)));
        }

        @Override
        protected void processMessages(List<TestMessage> messages,
                                       MessageStream<TestMessage> b) {
            worker.release(messages.size());
        }

        @Override
        protected void connect(SelectionKey key) {
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

        private static final int BATCH_SIZE = 1000;
        private static final int PERMITS = 2 * BATCH_SIZE;

        private TestMessageStream b;
        private FutureTask<Worker> task;

        // Stuff to throttle pump
        private final Semaphore semaphore = new Semaphore(PERMITS);
        private int msgWritten;

        void pump(TestMessageStream b) {
            this.b = b;
            task = new FutureTask<>(this, this);
            wpool.execute(task);
        }

        @Override
        public void run() {
            try {
                log.info("Worker started...");

                List<TestMessage> batch = new ArrayList<>();
                for (int i = 0; i < BATCH_SIZE; i++) {
                    batch.add(new TestMessage(msgLength));
                }

                while (msgWritten < msgCount) {
                    msgWritten += writeBatch(b, batch);
                }

                // Now try to get all the permits back before sending poison pill
                semaphore.acquireUninterruptibly(PERMITS);
                b.close();

                log.info("Worker done...");

            } catch (IOException e) {
                log.error("Worker unable to perform I/O", e);
            }
        }


        private int writeBatch(TestMessageStream b, List<TestMessage> batch)
                throws IOException {
            int count = Math.min(BATCH_SIZE, msgCount - msgWritten);
            acquire(count);
            if (count == BATCH_SIZE) {
                b.write(batch);
            } else {
                for (int i = 0; i < count; i++) {
                    b.write(batch.get(i));
                }
            }
            return count;
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
