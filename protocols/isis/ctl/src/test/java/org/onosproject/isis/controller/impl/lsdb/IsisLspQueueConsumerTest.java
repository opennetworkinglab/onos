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
package org.onosproject.isis.controller.impl.lsdb;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.isis.controller.impl.DefaultIsisInterface;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit test case for IsisLspQueueConsumer.
 */
public class IsisLspQueueConsumerTest {

    private IsisLspQueueConsumer isisLspQueueConsumer;
    private BlockingQueue blockingQueue = new ArrayBlockingQueue(1024);
    private DefaultLspWrapper lspWrapper;
    private DefaultLspWrapper lspWrapper1;
    private SocketAddress socketAddress = InetSocketAddress.createUnresolved("127.0.0.1", 7000);

    @Before
    public void setUp() throws Exception {
        lspWrapper = new DefaultLspWrapper();
        lspWrapper.setLspProcessing("refreshLsp");
        lspWrapper.setSelfOriginated(true);
        lspWrapper.setIsisInterface(new DefaultIsisInterface());
        lspWrapper1 = new DefaultLspWrapper();
        lspWrapper1.setLspProcessing("maxAgeLsp");
        lspWrapper1.setIsisInterface(new DefaultIsisInterface());
        blockingQueue.add(lspWrapper);
        blockingQueue.add(lspWrapper1);
        isisLspQueueConsumer = new IsisLspQueueConsumer(blockingQueue);
    }

    @After
    public void tearDown() throws Exception {
        isisLspQueueConsumer = null;
    }

    /**
     * Tests run() method.
     */
    @Test
    public void testRun() throws Exception {
        isisLspQueueConsumer.run();
        assertThat(isisLspQueueConsumer, is(notNullValue()));
    }
}