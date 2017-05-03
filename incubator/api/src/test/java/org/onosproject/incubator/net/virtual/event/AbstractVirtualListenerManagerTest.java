/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.incubator.net.virtual.event;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.event.AbstractEvent;
import org.onosproject.event.Event;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.event.EventListener;
import org.onosproject.event.EventSink;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.incubator.net.virtual.VirtualNetworkServiceAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Test of the virtual event dispatcher mechanism.
 */
public class AbstractVirtualListenerManagerTest {

    private TestEventDispatcher dispatcher = new TestEventDispatcher();
    private VirtualListenerRegistryManager listenerRegistryManager =
            VirtualListenerRegistryManager.getInstance();

    private PrickleManager prickleManager;
    private PrickleListener prickleListener;

    private GooManager gooManager;
    private GooListener gooListener;

    private BarManager barManager;
    private BarListener barListener;

    @Before
    public void setUp() {
        VirtualNetworkService manager = new TestVirtualNetworkManager();

        dispatcher.addSink(VirtualEvent.class, listenerRegistryManager);

        prickleListener = new PrickleListener();
        prickleManager = new PrickleManager(manager, NetworkId.networkId(1));
        prickleManager.addListener(prickleListener);

        gooListener = new GooListener();
        gooManager = new GooManager(manager, NetworkId.networkId(1));
        gooManager.addListener(gooListener);

        barListener = new BarListener();
        barManager = new BarManager(manager, NetworkId.networkId(2));
        barManager.addListener(barListener);
    }

    @After
    public void tearDown() {
        dispatcher.removeSink(VirtualEvent.class);

        prickleListener.events.clear();
        gooListener.events.clear();
        barListener.events.clear();

        prickleListener.latch = null;
        gooListener.latch = null;
        barListener.latch = null;
    }

    @Test
    public void postPrickle() throws InterruptedException {
        prickleListener.latch = new CountDownLatch(1);
        prickleManager.post(new Prickle("prickle"));
        prickleListener.latch.await(100, TimeUnit.MILLISECONDS);

        validate(prickleListener, "prickle");
        validate(gooListener);
        validate(barListener);
    }

    @Test
    public void postGoo() throws InterruptedException {
        gooListener.latch = new CountDownLatch(1);
        gooManager.post(new Goo("goo"));
        gooListener.latch.await(100, TimeUnit.MILLISECONDS);

        validate(prickleListener);
        validate(gooListener, "goo");
        validate(barListener);
    }

    @Test
    public void postBar() throws InterruptedException {
        barListener.latch = new CountDownLatch(1);
        barManager.post(new Bar("bar"));
        barListener.latch.await(100, TimeUnit.MILLISECONDS);

        validate(prickleListener);
        validate(gooListener);
        validate(barListener, "bar");
    }

    @Test
    public void postEventWithNoListener() throws Exception {
        dispatcher.post(new Thing("boom"));

        validate(prickleListener);
        validate(gooListener);
        validate(barListener);
    }

    private void validate(TestListener listener, String... strings) {
        int i = 0;
        assertEquals("incorrect event count", strings.length, listener.events.size());
        for (String string : strings) {
            Event event = (Event) listener.events.get(i++);
            assertEquals("incorrect event", string, event.subject());
        }
    }

    private enum Type { FOO }

    private static class Thing extends AbstractEvent<Type, String> {
        protected Thing(String subject) {
            super(Type.FOO, subject);
        }
    }

    private static final class Prickle extends Thing {
        private Prickle(String subject) {
            super(subject);
        }
    }

    private static final class Goo extends Thing {
        private Goo(String subject) {
            super(subject);
        }
    }

    private static final class Bar extends Thing {
        private Bar(String subject) {
            super(subject);
        }
    }

    private class TestListener<E extends Event> implements EventListener<E> {
        List<E> events = new ArrayList<>();
        CountDownLatch latch;

        @Override
        public void event(E event) {
            events.add(event);
            latch.countDown();
        }
    }

    private class PrickleListener extends TestListener<Prickle> {
    }

    private class GooListener extends TestListener<Goo> {
    }

    private class BarListener extends TestListener<Bar> {
    }

    private class PrickleManager extends AbstractVirtualListenerManager<Prickle, PrickleListener> {
        public PrickleManager(VirtualNetworkService service, NetworkId networkId) {
            super(service, networkId, Prickle.class);
        }
    }

    private class GooManager extends AbstractVirtualListenerManager<Goo, GooListener> {
        public GooManager(VirtualNetworkService service, NetworkId networkId) {
            super(service, networkId, Goo.class);
        }
    }

    private class BarManager extends AbstractVirtualListenerManager<Bar, BarListener> {
        public BarManager(VirtualNetworkService service, NetworkId networkId) {
            super(service, networkId, Bar.class);
        }
    }


    private class TestEventDispatcher implements EventDeliveryService {
        private EventSink sink;

        @Override
        public <E extends Event> void addSink(Class<E> eventClass, EventSink<E> sink) {
            this.sink = sink;
        }

        @Override
        public <E extends Event> void removeSink(Class<E> eventClass) {
            this.sink = null;
        }

        @Override
        public <E extends Event> EventSink<E> getSink(Class<E> eventClass) {
            return null;
        }

        @Override
        public Set<Class<? extends Event>> getSinks() {
            return null;
        }

        @Override
        public void setDispatchTimeLimit(long millis) {

        }

        @Override
        public long getDispatchTimeLimit() {
            return 0;
        }

        @Override
        public void post(Event event) {
            if (event instanceof VirtualEvent) {
                sink.process(event);
            }
        }
    }

    private class TestVirtualNetworkManager extends VirtualNetworkServiceAdapter {
        TestServiceDirectory serviceDirectory = new TestServiceDirectory();

        public TestVirtualNetworkManager() {
            serviceDirectory.add(EventDeliveryService.class, dispatcher);
        }

        @Override
        public ServiceDirectory getServiceDirectory() {
            return serviceDirectory;
        }
    }

    private  class TestServiceDirectory implements ServiceDirectory {

        private ClassToInstanceMap<Object> services = MutableClassToInstanceMap.create();

        @Override
        public <T> T get(Class<T> serviceClass) {
            return services.getInstance(serviceClass);
        }

        /**
         * Adds a new service to the directory.
         *
         * @param serviceClass service class
         * @param service service instance
         * @return self
         */
        public TestServiceDirectory add(Class serviceClass, Object service) {
            services.putInstance(serviceClass, service);
            return this;
        }

    }
}