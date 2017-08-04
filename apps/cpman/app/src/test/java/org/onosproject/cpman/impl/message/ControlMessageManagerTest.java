/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.cpman.impl.message;

import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.cpman.ControlMessage;
import org.onosproject.cpman.DefaultControlMessage;
import org.onosproject.cpman.message.ControlMessageEvent;
import org.onosproject.cpman.message.ControlMessageListener;
import org.onosproject.cpman.message.ControlMessageProvider;
import org.onosproject.cpman.message.ControlMessageProviderRegistry;
import org.onosproject.cpman.message.ControlMessageProviderService;
import org.onosproject.cpman.message.ControlMessageService;
import org.onosproject.event.DefaultEventSinkRegistry;
import org.onosproject.event.Event;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.event.EventSink;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.NetTestTools.injectEventDispatcher;

/**
 * Unit test for control message manager.
 */
public class ControlMessageManagerTest {

    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final DeviceId DID = deviceId("of:foo");

    private ControlMessageManager manager;
    private ControlMessageService service;
    private ControlMessageProviderRegistry registry;
    private ControlMessageProviderService providerService;
    protected TestProvider provider;
    protected TestListener listener = new TestListener();

    /**
     * Initializes all variables for unit test.
     */
    @Before
    public void setUp() {
        manager = new ControlMessageManager();
        service = manager;
        registry = manager;
        manager.store = new DefaultControlMessageStore();
        injectEventDispatcher(manager, new TestEventDispatcher());
        manager.activate();

        service.addListener(listener);

        provider = new TestProvider();
        providerService = registry.register(provider);
        assertTrue("provider should be registered",
                registry.getProviders().contains(provider.id()));
    }

    /**
     * Tear down the control message manager.
     */
    @After
    public void tearDown() {
        registry.unregister(provider);
        assertFalse("provider should not be registered",
                registry.getProviders().contains(provider.id()));
        service.removeListener(listener);
        manager.deactivate();
    }

    /**
     * Tests the updateStatsInfo method.
     */
    @Test
    public void updateStatsInfo() {
        Set<ControlMessage> cms = Sets.newHashSet();
        ControlMessage.Type in =  ControlMessage.Type.INBOUND_PACKET;
        ControlMessage.Type out =  ControlMessage.Type.OUTBOUND_PACKET;
        ControlMessage.Type mod =  ControlMessage.Type.FLOW_MOD_PACKET;
        ControlMessage.Type rmv =  ControlMessage.Type.FLOW_REMOVED_PACKET;
        ControlMessage.Type req =  ControlMessage.Type.REQUEST_PACKET;
        ControlMessage.Type rep =  ControlMessage.Type.REPLY_PACKET;

        cms.add(new DefaultControlMessage(in, DID, 0, 0, 0, 0));
        cms.add(new DefaultControlMessage(out, DID, 0, 0, 0, 0));
        cms.add(new DefaultControlMessage(mod, DID, 0, 0, 0, 0));
        cms.add(new DefaultControlMessage(rmv, DID, 0, 0, 0, 0));
        cms.add(new DefaultControlMessage(req, DID, 0, 0, 0, 0));
        cms.add(new DefaultControlMessage(rep, DID, 0, 0, 0, 0));

        providerService.updateStatsInfo(DID, cms);
        validateEvents(ControlMessageEvent.Type.STATS_UPDATE);
        cms.clear();
    }

    /**
     * Validates whether the manager receives the right events.
     *
     * @param types a set of types of control message event
     */
    protected void validateEvents(Enum... types) {
        int i = 0;
        assertEquals("wrong events received", types.length, listener.events.size());
        for (Event event : listener.events) {
            assertEquals("incorrect event type", types[i], event.type());
            i++;
        }
        listener.events.clear();
    }

    /**
     * A mock of control message provider.
     */
    private class TestProvider extends AbstractProvider implements ControlMessageProvider {

        public TestProvider() {
            super(PID);
        }
    }

    /**
     * A mock of control message listener.
     */
    private static class TestListener implements ControlMessageListener {
        final List<ControlMessageEvent> events = new ArrayList<>();

        @Override
        public void event(ControlMessageEvent event) {
            events.add(event);
        }
    }

    /**
     * A mock of event dispatcher.
     */
    private class TestEventDispatcher extends DefaultEventSinkRegistry
            implements EventDeliveryService {
        @Override
        @SuppressWarnings("unchecked")
        public synchronized void post(Event event) {
            EventSink sink = getSink(event.getClass());
            checkState(sink != null, "No sink for event %s", event);
            sink.process(event);
        }

        @Override
        public void setDispatchTimeLimit(long millis) {
        }

        @Override
        public long getDispatchTimeLimit() {
            return 0;
        }
    }
}
