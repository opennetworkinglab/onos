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

package org.onosproject.incubator.store.virtual.impl;

import com.google.common.collect.Maps;
import org.onosproject.event.Event;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualStore;
import org.onosproject.store.StoreDelegate;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;

/**
 * Base implementation of a virtual store.
 */
public class AbstractVirtualStore<E extends Event, D extends StoreDelegate<E>>
        implements VirtualStore<E, D> {

    protected Map<NetworkId, D> delegateMap = Maps.newConcurrentMap();

    @Override
    public void setDelegate(NetworkId networkId, D delegate) {
        checkState(delegateMap.get(networkId) == null
                           || delegateMap.get(networkId) == delegate,
                   "Store delegate already set");

        delegateMap.putIfAbsent(networkId, delegate);
    }

    @Override
    public void unsetDelegate(NetworkId networkId, D delegate) {
        if (delegateMap.get(networkId) == delegate) {
            delegateMap.remove(networkId, delegate);
        }
    }

    @Override
    public boolean hasDelegate(NetworkId networkId) {
        return delegateMap.get(networkId) != null;
    }

    /**
     * Notifies the delegate with the specified event.
     *
     * @param networkId a virtual network identifier
     * @param event event to delegate
     */
    protected void notifyDelegate(NetworkId networkId, E event) {
        if (delegateMap.get(networkId) != null) {
            delegateMap.get(networkId).notify(event);
        }
    }

    /**
     * Notifies the delegate with the specified list of events.
     *
     * @param networkId a virtual network identifier
     * @param events list of events to delegate
     */
    protected void notifyDelegate(NetworkId networkId, List<E> events) {
        for (E event: events) {
            notifyDelegate(networkId, event);
        }
    }
}
