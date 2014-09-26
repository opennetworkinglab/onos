package org.onlab.nio;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.net.InetAddress;
import java.text.DecimalFormat;
import java.util.Random;

import static org.onlab.junit.TestTools.delay;

/**
 * Integration test for the select, accept and IO loops.
 */
public class IOLoopIntegrationTest {

    private static final int MILLION = 1000000;
    private static final int TIMEOUT = 60;

    private static final int THREADS = 6;
    private static final int MSG_COUNT = 20 * MILLION;
    private static final int MSG_SIZE = 128;

    private static final long MIN_MPS = 10 * MILLION;

    @Before
    public void warmUp() throws Exception {
        try {
            run(MILLION, MSG_SIZE, 15, 0);
        } catch (Throwable e) {
            System.err.println("Failed warmup but moving on.");
            e.printStackTrace();
        }
    }

    @Ignore
    @Test
    public void basic() throws Exception {
        run(MSG_COUNT, MSG_SIZE, TIMEOUT, MIN_MPS);
    }


    private void run(int count, int size, int timeout, double mps) throws Exception {
        DecimalFormat f = new DecimalFormat("#,##0");
        System.out.print(f.format(count * THREADS) +
                                 (mps > 0.0 ? " messages: " : " message warm-up: "));

        // Setup the test on a random port to avoid intermittent test failures
        // due to the port being already bound.
        int port = IOLoopServer.PORT + new Random().nextInt(100);

        InetAddress ip = InetAddress.getLoopbackAddress();
        IOLoopServer sss = new IOLoopServer(ip, THREADS, size, port);
        IOLoopClient ssc = new IOLoopClient(ip, THREADS, count, size, port);

        sss.start();
        ssc.start();
        delay(250);       // give the server and client a chance to go

        ssc.await(timeout);
        ssc.report();

        delay(1000);
        sss.stop();
        sss.report();
    }

}
