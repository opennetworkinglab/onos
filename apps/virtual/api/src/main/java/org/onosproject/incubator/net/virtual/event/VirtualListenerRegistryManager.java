/*
 * Copyright 2017-present Open Networking Foundation
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

import com.google.common.collect.Maps;
import org.onosproject.event.Event;
import org.onosproject.event.EventSink;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.incubator.net.virtual.NetworkId;

import java.util.Map;

/**
 * Base implementation of an virtual event sink and a registry capable of tracking
 * listeners and dispatching events to them as part of event sink processing.
 */
public final class VirtualListenerRegistryManager
        implements EventSink<VirtualEvent> {

    private Map<NetworkId, Map<Class<? extends Event>, ListenerRegistry>>
            listenerMapByNetwork = Maps.newConcurrentMap();

    ListenerRegistry lastStart;

    // non-instantiable (except for our Singleton)
    private VirtualListenerRegistryManager() {

    }

    public static VirtualListenerRegistryManager getInstance() {
        return SingletonHelper.INSTANCE;
    }

    public ListenerRegistry getRegistry(NetworkId networkId,
                                        Class<? extends Event> eventClass) {
        Map<Class<? extends Event>, ListenerRegistry> listenerMapByEvent =
                listenerMapByNetwork.get(networkId);

        if (listenerMapByEvent == null) {
            listenerMapByEvent = Maps.newConcurrentMap();
            listenerMapByNetwork.putIfAbsent(networkId, listenerMapByEvent);
        }

        ListenerRegistry listenerRegistry = listenerMapByEvent.get(eventClass);

        if (listenerRegistry == null) {
            listenerRegistry = new ListenerRegistry();
            listenerMapByEvent.putIfAbsent(eventClass, listenerRegistry);
        }

        return listenerRegistry;
    }

    @Override
    public void process(VirtualEvent event) {
        NetworkId networkId = event.networkId();
        Event originalEvent = (Event) event.subject();

        ListenerRegistry listenerRegistry =
                listenerMapByNetwork.get(networkId).get(originalEvent.getClass());
        if (listenerRegistry != null) {
            listenerRegistry.process(originalEvent);
            lastStart = listenerRegistry;
        }
    }

    @Override
    public void onProcessLimit() {
        lastStart.onProcessLimit();
    }

    /**
     * Prevents object instantiation from external.
     */
    private static final class SingletonHelper {
        private static final String ILLEGAL_ACCESS_MSG =
                "Should not instantiate this class.";
        private static final VirtualListenerRegistryManager INSTANCE =
                new VirtualListenerRegistryManager();

        private SingletonHelper() {
            throw new IllegalAccessError(ILLEGAL_ACCESS_MSG);
        }
    }
}
