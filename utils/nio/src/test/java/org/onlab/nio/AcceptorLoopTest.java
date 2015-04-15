/*
 * Copyright 2014-2015 Open Networking Laboratory
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

import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.onlab.junit.TestTools.delay;

/**
 * Unit tests for AcceptLoop.
 */
public class AcceptorLoopTest extends AbstractLoopTest {

    private static final int PICK_EPHEMERAL = 0;

    private static final SocketAddress SOCK_ADDR = new InetSocketAddress("127.0.0.1", PICK_EPHEMERAL);

    private static class MyAcceptLoop extends AcceptorLoop {
        private final CountDownLatch loopStarted = new CountDownLatch(1);
        private final CountDownLatch loopFinished = new CountDownLatch(1);
        private final CountDownLatch runDone = new CountDownLatch(1);
        private final CountDownLatch ceaseLatch = new CountDownLatch(1);

        private int acceptCount = 0;

        MyAcceptLoop() throws IOException {
            super(500, SOCK_ADDR);
        }

        @Override
        protected void acceptConnection(ServerSocketChannel ssc) throws IOException {
            acceptCount++;
        }

        @Override
        public void loop() throws IOException {
            loopStarted.countDown();
            super.loop();
            loopFinished.countDown();
        }

        @Override
        public void run() {
            super.run();
            runDone.countDown();
        }

        @Override
        public void shutdown() {
            super.shutdown();
            ceaseLatch.countDown();
        }
    }

    @Test
    public void basic() throws IOException {
        MyAcceptLoop myAccLoop = new MyAcceptLoop();
        AcceptorLoop accLoop = myAccLoop;
        exec.execute(accLoop);
        waitForLatch(myAccLoop.loopStarted, "loopStarted");
        delay(200); // take a quick nap
        accLoop.shutdown();
        waitForLatch(myAccLoop.loopFinished, "loopFinished");
        waitForLatch(myAccLoop.runDone, "runDone");
        assertEquals(0, myAccLoop.acceptCount);
    }
}
