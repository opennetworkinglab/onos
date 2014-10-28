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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.net.InetAddress;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.onlab.junit.TestTools.delay;

/**
 * Integration test for the select, accept and IO loops.
 */
public class IOLoopIntegrationTest {

    private static final int THREADS = 6;
    private static final int TIMEOUT = 60;
    private static final int MESSAGE_LENGTH = 128;

    private static final int MILLION = 1000000;
    private static final int MSG_COUNT = 40 * MILLION;

    @Before
    public void warmUp() throws Exception {
        Logger.getLogger("").setLevel(Level.SEVERE);
        try {
            runTest(MILLION, MESSAGE_LENGTH, 15);
        } catch (Throwable e) {
            System.err.println("Failed warmup but moving on.");
            e.printStackTrace();
        }
    }

    // TODO: this test can not pass in some environments, need to be improved
    @Ignore
    @Test
    public void basic() throws Exception {
        runTest(MILLION, MESSAGE_LENGTH, TIMEOUT);
    }

    public void longHaul() throws Exception {
        runTest(MSG_COUNT, MESSAGE_LENGTH, TIMEOUT);
    }

    private void runTest(int count, int size, int timeout) throws Exception {
        // Use a random port to prevent conflicts.
        int port = IOLoopTestServer.PORT + new Random().nextInt(100);

        InetAddress ip = InetAddress.getLoopbackAddress();
        IOLoopTestServer server = new IOLoopTestServer(ip, THREADS, size, port);
        IOLoopTestClient client = new IOLoopTestClient(ip, THREADS, count, size, port);

        server.start();
        client.start();
        delay(100); // Pause to allow loops to get going

        client.await(timeout);
        client.report();

        server.stop();
        server.report();
    }

}
