/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net.intent.impl;

import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentStore;
import org.onosproject.net.intent.IntentStoreDelegate;
import org.onosproject.store.Timestamp;
import org.onosproject.store.trivial.SimpleIntentStore;
import org.onosproject.store.trivial.SystemClockTimestamp;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.intent.IntentState.*;
import static org.onosproject.net.intent.IntentTestsMocks.MockIntent;

/**
 * Test intent cleanup using Mocks.
 * FIXME remove this or IntentCleanupTest
 */
public class IntentCleanupTestMock extends AbstractIntentTest {

    private IntentCleanup cleanup;
    private IntentService service;
    private IntentStore store;

    @Before
    public void setUp() {
        service = createMock(IntentService.class);
        store = new SimpleIntentStore();
        cleanup = new IntentCleanup();

        service.addListener(cleanup);
        expectLastCall().once();
        replay(service);

        cleanup.cfgService = new ComponentConfigAdapter();
        cleanup.service = service;
        cleanup.store = store;
        cleanup.period = 1000;
        cleanup.retryThreshold = 3;
        cleanup.activate();

        verify(service);
        reset(service);

        assertTrue("store should be empty",
                   Sets.newHashSet(cleanup.store.getIntents()).isEmpty());

        super.setUp();
    }

    @After
    public void tearDown() {
        service.removeListener(cleanup);
        expectLastCall().once();
        replay(service);

        cleanup.deactivate();

        verify(service);
        reset(service);

        super.tearDown();
    }

    /**
     * Trigger resubmit of intent in CORRUPT during periodic poll.
     */
    @Test
    public void corruptPoll() {
        IntentStoreDelegate mockDelegate = new IntentStoreDelegate() {
            @Override
            public void process(IntentData intentData) {
                intentData.setState(CORRUPT);
                store.write(intentData);
            }

            @Override
            public void notify(IntentEvent event) {}
        };
        store.setDelegate(mockDelegate);

        Intent intent = new MockIntent(1L);
        Timestamp version = new SystemClockTimestamp(1L);
        IntentData data = new IntentData(intent, INSTALL_REQ, version);
        store.addPending(data);

        service.submit(intent);
        expectLastCall().once();
        replay(service);

        synchronized (service) {
            cleanup.run();
        }
        verify(service);
        reset(service);
    }

    /**
     * Trigger resubmit of intent in INSTALL_REQ for too long.
     */
    @Test
    public void pendingPoll() {
        IntentStoreDelegate mockDelegate = new IntentStoreDelegate() {
            @Override
            public void process(IntentData intentData) {}

            @Override
            public void notify(IntentEvent event) {
                cleanup.event(event);
            }
        };
        store.setDelegate(mockDelegate);

        Intent intent = new MockIntent(1L);
        Timestamp version = new SystemClockTimestamp(1L);
        IntentData data = new IntentData(intent, INSTALL_REQ, version);
        store.addPending(data);

        service.addPending(data);
        expectLastCall().once();
        replay(service);

        cleanup.run();
        verify(service);
        reset(service);
    }

    /**
     * Trigger resubmit of intent in INSTALLING for too long.
     */
    @Test
    @Ignore("The implementation is dependent on the SimpleStore")
    public void installingPoll() {
        IntentStoreDelegate mockDelegate = new IntentStoreDelegate() {
            @Override
            public void process(IntentData intentData) {
                intentData.setState(INSTALLING);
                store.write(intentData);
            }

            @Override
            public void notify(IntentEvent event) {
                cleanup.event(event);
            }
        };
        store.setDelegate(mockDelegate);

        Intent intent = new MockIntent(1L);
        Timestamp version = new SystemClockTimestamp(1L);
        IntentData data = new IntentData(intent, INSTALL_REQ, version);
        store.addPending(data);

        service.addPending(data);
        expectLastCall().once();
        replay(service);

        cleanup.run();
        verify(service);
        reset(service);
    }

    /**
     * Only submit one of two intents because one is too new.
     */
    @Test
    public void skipPoll() {
        IntentStoreDelegate mockDelegate = new IntentStoreDelegate() {
            @Override
            public void process(IntentData intentData) {
                intentData.setState(CORRUPT);
                store.write(intentData);
            }

            @Override
            public void notify(IntentEvent event) {}
        };
        store.setDelegate(mockDelegate);

        Intent intent = new MockIntent(1L);
        IntentData data = new IntentData(intent, INSTALL_REQ, null);
        store.addPending(data);

        Intent intent2 = new MockIntent(2L);
        Timestamp version = new SystemClockTimestamp(1L);
        data = new IntentData(intent2, INSTALL_REQ, version);
        store.addPending(data);

        service.submit(intent2);
        expectLastCall().once();
        replay(service);

        cleanup.run();
        verify(service);
        reset(service);
    }

    /**
     * Verify resubmit in response to CORRUPT event.
     */
    @Test
    public void corruptEvent() {
        IntentStoreDelegate mockDelegate = new IntentStoreDelegate() {
            @Override
            public void process(IntentData intentData) {
                intentData.setState(CORRUPT);
                store.write(intentData);
            }

            @Override
            public void notify(IntentEvent event) {
                cleanup.event(event);
            }
        };
        store.setDelegate(mockDelegate);


        Intent intent = new MockIntent(1L);
        IntentData data = new IntentData(intent, INSTALL_REQ, null);

        service.submit(intent);
        expectLastCall().once();
        replay(service);

        store.addPending(data);

        verify(service);
        reset(service);
    }

    /**
     * Intent should not be retried because threshold is reached.
     */
    @Test
    public void corruptEventThreshold() {
        IntentStoreDelegate mockDelegate = new IntentStoreDelegate() {
            @Override
            public void process(IntentData intentData) {
                intentData.setState(CORRUPT);
                intentData.setErrorCount(cleanup.retryThreshold);
                store.write(intentData);
            }

            @Override
            public void notify(IntentEvent event) {
                cleanup.event(event);
            }
        };
        store.setDelegate(mockDelegate);


        Intent intent = new MockIntent(1L);
        IntentData data = new IntentData(intent, INSTALL_REQ, null);

        replay(service);

        store.addPending(data);

        verify(service);
        reset(service);
    }
}