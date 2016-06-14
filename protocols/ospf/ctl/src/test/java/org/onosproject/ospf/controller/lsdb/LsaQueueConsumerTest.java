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
package org.onosproject.ospf.controller.lsdb;

import org.easymock.EasyMock;
import org.jboss.netty.channel.Channel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.ospf.controller.LsaWrapper;
import org.onosproject.ospf.controller.OspfArea;
import org.onosproject.ospf.controller.OspfLsaType;
import org.onosproject.ospf.controller.area.OspfAreaImpl;
import org.onosproject.ospf.controller.area.OspfInterfaceImpl;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.lsa.types.RouterLsa;
import org.onosproject.ospf.protocol.util.OspfInterfaceState;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test class for LsaQueueConsumer.
 */
public class LsaQueueConsumerTest {
    private LsaQueueConsumer lsaQueueConsumer;
    private BlockingQueue<LsaWrapper> blockingQueue;
    private Channel channel;
    private LsaWrapperImpl lsaWrapper;
    private OspfArea ospfArea;
    private RouterLsa routerLsa;
    private OspfInterfaceImpl ospfInterface;
    private LsaHeader lsaHeader;
    private LsdbAgeImpl lsdbAge;

    @Before
    public void setUp() throws Exception {
        lsaQueueConsumer = EasyMock.createMock(LsaQueueConsumer.class);
    }

    @After
    public void tearDown() throws Exception {
        lsaQueueConsumer = null;
        blockingQueue = null;
        channel = null;
        lsaWrapper = null;
        lsdbAge = null;
        lsaHeader = null;
        ospfInterface = null;
        ospfArea = null;
        routerLsa = null;
    }

    /**
     * Tests run() method.
     */
    @Test
    public void testRun() throws Exception {
        blockingQueue = new ArrayBlockingQueue(5);
        ospfArea = new OspfAreaImpl();
        lsdbAge = new LsdbAgeImpl(ospfArea);
        channel = EasyMock.createMock(Channel.class);
        lsaWrapper = new LsaWrapperImpl();
        lsaWrapper.setLsaProcessing("verifyChecksum");
        blockingQueue.add(lsaWrapper);
        lsaQueueConsumer = new LsaQueueConsumer(blockingQueue, channel, ospfArea);
        lsaQueueConsumer.run();
        assertThat(lsaQueueConsumer, is(notNullValue()));
    }

    /**
     * Tests run() method.
     */
    @Test
    public void testRun1() throws Exception {
        blockingQueue = new ArrayBlockingQueue(5);
        channel = EasyMock.createMock(Channel.class);
        ospfArea = new OspfAreaImpl();
        lsaWrapper = new LsaWrapperImpl();
        routerLsa = new RouterLsa();
        routerLsa.setLsType(1);
        lsaWrapper.addLsa(OspfLsaType.ROUTER, routerLsa);
        ospfInterface = new OspfInterfaceImpl();
        ospfInterface.setState(OspfInterfaceState.DR);
        lsaWrapper.setOspfInterface(ospfInterface);
        lsaWrapper.setIsSelfOriginated(true);
        lsaHeader = new LsaHeader();
        lsaHeader.setLsType(1);
        lsaWrapper.setLsaHeader(lsaHeader);
        lsaWrapper.setLsaProcessing("refreshLsa");
        lsaWrapper.setLsdbAge(new LsdbAgeImpl(ospfArea));
        blockingQueue.add(lsaWrapper);
        lsaQueueConsumer = new LsaQueueConsumer(blockingQueue, channel, ospfArea);
        lsaQueueConsumer.run();
        assertThat(lsaQueueConsumer, is(notNullValue()));
    }

    @Test
    public void testRun3() throws Exception {
        blockingQueue = new ArrayBlockingQueue(5);
        channel = EasyMock.createMock(Channel.class);
        ospfArea = new OspfAreaImpl();
        lsaWrapper = new LsaWrapperImpl();
        routerLsa = new RouterLsa();
        routerLsa.setLsType(2);
        lsaWrapper.addLsa(OspfLsaType.NETWORK, routerLsa);
        ospfInterface = new OspfInterfaceImpl();
        ospfInterface.setState(OspfInterfaceState.BDR);
        lsaWrapper.setOspfInterface(ospfInterface);
        lsaWrapper.setIsSelfOriginated(true);
        lsaHeader = new LsaHeader();
        lsaHeader.setLsType(2);
        lsaWrapper.setLsaHeader(lsaHeader);
        lsaWrapper.setLsaProcessing("refreshLsa");
        lsaWrapper.setLsdbAge(new LsdbAgeImpl(ospfArea));
        blockingQueue.add(lsaWrapper);
        lsaQueueConsumer = new LsaQueueConsumer(blockingQueue, channel, ospfArea);
        lsaQueueConsumer.run();
        assertThat(lsaQueueConsumer, is(notNullValue()));
    }

    /**
     * Tests run() method.
     */
    @Test
    public void testRun5() throws Exception {
        blockingQueue = new ArrayBlockingQueue(5);
        channel = EasyMock.createMock(Channel.class);
        ospfArea = new OspfAreaImpl();
        lsaWrapper = new LsaWrapperImpl();
        routerLsa = new RouterLsa();
        routerLsa.setLsType(2);
        lsaWrapper.addLsa(OspfLsaType.NETWORK, routerLsa);
        ospfInterface = new OspfInterfaceImpl();
        ospfInterface.setState(OspfInterfaceState.DR);
        lsaWrapper.setOspfInterface(ospfInterface);
        lsaWrapper.setIsSelfOriginated(true);
        lsaHeader = new LsaHeader();
        lsaHeader.setLsType(2);
        lsaWrapper.setLsaHeader(lsaHeader);
        lsaWrapper.setLsaProcessing("maxAgeLsa");
        lsaWrapper.setLsdbAge(new LsdbAgeImpl(ospfArea));
        blockingQueue.add(lsaWrapper);
        lsaQueueConsumer = new LsaQueueConsumer(blockingQueue, channel, ospfArea);
        lsaQueueConsumer.run();
        assertThat(lsaQueueConsumer, is(notNullValue()));
    }

    /**
     * Tests setChannel() method.
     */
    @Test
    public void testSetChannel() throws Exception {
        channel = EasyMock.createMock(Channel.class);
        lsdbAge = new LsdbAgeImpl(ospfArea);
        lsdbAge.startDbAging();
        lsdbAge.setChannel(channel);
        assertThat(lsaQueueConsumer, is(notNullValue()));
    }

}