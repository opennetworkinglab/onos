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
package org.onosproject.lisp.ctl.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.CoreService;
import org.onosproject.lisp.ctl.LispMessageListener;
import org.onosproject.lisp.ctl.LispRouter;
import org.onosproject.lisp.ctl.LispRouterAdapter;
import org.onosproject.lisp.ctl.LispRouterAgent;
import org.onosproject.lisp.ctl.LispRouterId;
import org.onosproject.lisp.ctl.LispRouterListener;
import org.onosproject.lisp.msg.protocols.DefaultLispMapNotify.DefaultNotifyBuilder;
import org.onosproject.lisp.msg.protocols.DefaultLispMapRecord.DefaultMapRecordBuilder;
import org.onosproject.lisp.msg.protocols.DefaultLispMapRegister.DefaultRegisterBuilder;
import org.onosproject.lisp.msg.protocols.LispMapNotify;
import org.onosproject.lisp.msg.protocols.LispMapNotify.NotifyBuilder;
import org.onosproject.lisp.msg.protocols.LispMapRecord;
import org.onosproject.lisp.msg.protocols.LispMapRecord.MapRecordBuilder;
import org.onosproject.lisp.msg.protocols.LispMapRegister;
import org.onosproject.lisp.msg.protocols.LispMapRegister.RegisterBuilder;
import org.onosproject.lisp.msg.protocols.LispMapReplyAction;
import org.onosproject.lisp.msg.protocols.LispMessage;
import org.onosproject.lisp.msg.types.LispIpv4Address;
import org.osgi.service.component.ComponentContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static junit.framework.TestCase.fail;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit tests for the LISP controller implementation class.
 */
public class LispControllerImplTest {

    private LispRouterId routerId1;
    private LispRouterId routerId2;
    private LispRouterId routerId3;
    private LispRouter router1;
    private LispRouter router2;
    private LispRouter router3;

    private LispControllerImpl controller;
    private LispRouterAgent agent;
    private TestRouterListener routerListener;
    private TestMessageListener messageListener;

    /**
     * Tests harness for a router routerListener.
     */
    static final class TestRouterListener implements LispRouterListener {

        final List<LispRouterId> removedIds = Lists.newArrayList();
        final List<LispRouterId> addedIds = Lists.newArrayList();
        final List<LispRouterId> changedIds = Lists.newArrayList();

        @Override
        public void routerAdded(LispRouterId routerId) {
            addedIds.add(routerId);
        }

        @Override
        public void routerRemoved(LispRouterId routerId) {
            removedIds.add(routerId);
        }

        @Override
        public void routerChanged(LispRouterId routerId) {
            changedIds.add(routerId);
        }
    }

    /**
     * Tests harness for a router messageListener.
     */
    static final class TestMessageListener implements LispMessageListener {

        final List<LispMessage> incomingMessages = Lists.newArrayList();
        final List<LispMessage> outgoingMessages = Lists.newArrayList();

        CountDownLatch incomingLatch = new CountDownLatch(1);
        CountDownLatch outgoingLatch = new CountDownLatch(1);

        @Override
        public void handleIncomingMessage(LispRouterId routerId, LispMessage msg) {
            synchronized (incomingMessages) {
                incomingMessages.add(msg);
                incomingLatch.countDown();
            }
        }

        @Override
        public void handleOutgoingMessage(LispRouterId routerId, LispMessage msg) {
            synchronized (outgoingMessages) {
                outgoingMessages.add(msg);
                outgoingLatch.countDown();
            }
        }

        void waitUntilUpdateIsCalled() throws InterruptedException {
            incomingLatch.await();
            outgoingLatch.await();
        }
    }

    /**
     * Sets up routers to use as data, mocks and launches a controller instance.
     */
    @Before
    public void setUp() {
        try {
            router1 = new LispRouterAdapter();
            routerId1 = LispRouterId.routerId(new URI("lisp:10.1.1.1"));
            router2 = new LispRouterAdapter();
            routerId2 = LispRouterId.routerId(new URI("lisp:10.1.1.2"));
            router3 = new LispRouterAdapter();
            routerId3 = LispRouterId.routerId(new URI("lisp:10.1.1.3"));

        } catch (URISyntaxException e) {
            // this will never happen...
            fail();
        }

        controller = new LispControllerImpl();
        agent = controller.agent;

        routerListener = new TestRouterListener();
        controller.addRouterListener(routerListener);

        messageListener = new TestMessageListener();
        controller.addMessageListener(messageListener);

        controller.coreService = createMock(CoreService.class);

        ComponentConfigService mockConfigService =
                                createMock(ComponentConfigService.class);
        expect(mockConfigService.getProperties(anyObject())).andReturn(ImmutableSet.of());
        mockConfigService.registerProperties(controller.getClass());
        expectLastCall();
        mockConfigService.unregisterProperties(controller.getClass(), false);
        expectLastCall();
        expect(mockConfigService.getProperties(anyObject())).andReturn(ImmutableSet.of());
        controller.cfgService = mockConfigService;
        replay(mockConfigService);

        ComponentContext mockContext = createMock(ComponentContext.class);
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("lispAuthKey", "onos");
        properties.put("lispAuthKeyId", 1);
        expect(mockContext.getProperties()).andReturn(properties);
        replay(mockContext);

        LispControllerBootstrap bootstrap = createMock(LispControllerBootstrap.class);
        bootstrap.start();
        expectLastCall();
        controller.bootstrap = bootstrap;
        replay(bootstrap);

        controller.activate(mockContext);
    }

    @After
    public void tearDown() {

        LispControllerBootstrap bootstrap = createMock(LispControllerBootstrap.class);
        bootstrap.stop();
        expectLastCall();
        controller.bootstrap = bootstrap;
        replay(bootstrap);

        controller.removeRouterListener(routerListener);
        controller.removeMessageListener(messageListener);

        controller.deactivate();
    }

    /**
     * Tests adding and removing connected routers.
     */
    @Test
    public void testAddRemoveConnectedRouter() {

        // Test adding connected routers into agent
        boolean addRouter1 = agent.addConnectedRouter(routerId1, router1);
        assertThat(addRouter1, is(true));
        boolean addRouter2 = agent.addConnectedRouter(routerId2, router2);
        assertThat(addRouter2, is(true));
        boolean addRouter3 = agent.addConnectedRouter(routerId3, router3);
        assertThat(addRouter3, is(true));

        // Test the callback methods that contained in router listener is fired
        assertThat(routerListener.addedIds, hasSize(3));
        assertThat(routerListener.addedIds, hasItems(routerId1, routerId2, routerId3));

        // Test adding a router twice (duplicated router)
        // this should return false to indicate that there is already a router
        // has been added previously
        boolean addBadRouter1 = agent.addConnectedRouter(routerId1, router1);
        assertThat(addBadRouter1, is(false));

        // Also make sure that the duplicated router will never increase the counter
        assertThat(controller.connectedRouters.size(), is(3));

        // Test querying the router list
        Stream<LispRouter> queriedRouters = makeIntoStream(controller.getRouters());
        long routerCount = queriedRouters.count();
        assertThat(routerCount, is(3L));

        // Test querying the individual router
        LispRouter queriedRouter = controller.getRouter(routerId1);
        assertThat(queriedRouter, is(router1));

        // Test removing a router from connected router collection
        agent.removeConnectedRouter(routerId2);
        Stream<LispRouter> queriedRoutersAfterRemoval =
                            makeIntoStream(controller.getRouters());
        long routerCountAfterRemoval = queriedRoutersAfterRemoval.count();
        assertThat(routerCountAfterRemoval, is(2L));

        // Test the callback methods that contained in router listener is fired
        assertThat(routerListener.removedIds, hasSize(1));
        assertThat(routerListener.removedIds, hasItems(routerId2));

        // Test querying the removed switch
        LispRouter queriedRouterAfterRemoval = controller.getRouter(routerId2);
        assertThat(queriedRouterAfterRemoval, nullValue());
    }

    /**
     * Tests adding and removing subscribed routers.
     */
    @Test
    public void testAddRemoveSubscribedRouter() {
        router1.setSubscribed(true);
        router2.setSubscribed(true);

        // Test adding connected routers into agent
        boolean addRouter1 = agent.addConnectedRouter(routerId1, router1);
        assertThat(addRouter1, is(true));
        boolean addRouter2 = agent.addConnectedRouter(routerId2, router2);
        assertThat(addRouter2, is(true));

        assertThat(Lists.newArrayList(
                controller.getSubscribedRouters()), hasSize(2));
        assertThat(Lists.newArrayList(
                controller.getSubscribedRouters()), hasItems(router1, router2));
    }

    /**
     * Tests adding and removing LISP messages.
     */
    @Test
    public void testLispMessagePopulate() throws InterruptedException {

        RegisterBuilder registerBuilder = new DefaultRegisterBuilder();
        List<LispMapRecord> records = ImmutableList.of(getMapRecord(), getMapRecord());
        LispMapRegister register = registerBuilder
                                        .withIsProxyMapReply(true)
                                        .withIsWantMapNotify(false)
                                        .withKeyId((short) 1)
                                        .withAuthKey("onos")
                                        .withNonce(1L)
                                        .withMapRecords(records)
                                        .build();

        NotifyBuilder notifyBuilder = new DefaultNotifyBuilder();
        LispMapNotify notify = notifyBuilder
                                        .withKeyId((short) 1)
                                        .withAuthKey("onos")
                                        .withNonce(1L)
                                        .withMapRecords(records)
                                        .build();

        // Test the callback methods that contained in message listener is fired
        agent.processUpstreamMessage(routerId1, register);
        // Following line will be ignored
        agent.processUpstreamMessage(routerId1, notify);

        agent.processDownstreamMessage(routerId1, notify);
        // Following line will be ignored
        agent.processDownstreamMessage(routerId1, register);

        messageListener.waitUntilUpdateIsCalled();

        assertThat(messageListener.incomingMessages, hasSize(1));
        assertThat(messageListener.incomingMessages, hasItems(register));

        assertThat(messageListener.outgoingMessages, hasSize(1));
        assertThat(messageListener.outgoingMessages, hasItems(notify));
    }

    /**
     * Generates and returns a map record.
     *
     * @return a map record
     */
    private LispMapRecord getMapRecord() {
        MapRecordBuilder builder1 = new DefaultMapRecordBuilder();

        LispIpv4Address ipv4Locator1 = new LispIpv4Address(IpAddress.valueOf("192.168.1.1"));

        return builder1
                .withRecordTtl(100)
                .withIsAuthoritative(true)
                .withMapVersionNumber((short) 1)
                .withMaskLength((byte) 0x01)
                .withAction(LispMapReplyAction.NativelyForward)
                .withEidPrefixAfi(ipv4Locator1)
                .build();
    }

    /**
     * Converts an Iterable of some type into a stream of that type.
     *
     * @param items Iterable of objects
     * @param <T> type of the items in the iterable
     * @return stream of objects of type T
     */
    private <T> Stream<T> makeIntoStream(Iterable<T> items) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        items.iterator(), Spliterator.ORDERED), false);
    }
}
