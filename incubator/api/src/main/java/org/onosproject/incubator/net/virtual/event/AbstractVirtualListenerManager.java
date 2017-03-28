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

import org.onlab.osgi.ServiceDirectory;
import org.onosproject.event.Event;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.event.EventListener;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.event.ListenerService;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.incubator.net.virtual.VnetService;

/**
 * Basis for virtual event components which need to export listener mechanism.
 */
public abstract class AbstractVirtualListenerManager
        <E extends Event, L extends EventListener<E>>
        implements ListenerService<E, L>, VnetService {

    protected final NetworkId networkId;
    protected final VirtualNetworkService manager;
    protected final ServiceDirectory serviceDirectory;

    protected EventDeliveryService eventDispatcher;

    private ListenerRegistry<E, L> listenerRegistry;

    private VirtualListenerRegistryManager listenerManager =
            VirtualListenerRegistryManager.getInstance();

    public AbstractVirtualListenerManager(VirtualNetworkService manager,
                                          NetworkId networkId,
                                          Class<? extends Event> eventClass) {
        this.manager = manager;
        this.networkId = networkId;
        this.serviceDirectory = manager.getServiceDirectory();

        //Set default event delivery service by default
        this.eventDispatcher = serviceDirectory.get(EventDeliveryService.class);

        //Initialize and reference to the listener registry
        this.listenerRegistry = listenerManager.getRegistry(networkId, eventClass);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addListener(L listener) {
        listenerRegistry.addListener(listener);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void removeListener(L listener) {
        listenerRegistry.removeListener(listener);
    }

    /**
     * Safely posts the specified event to the local event dispatcher.
     * If there is no event dispatcher or if the event is null, this method
     * is a noop.
     *
     * @param event event to be posted; may be null
     */
    protected void post(E event) {
        if (event != null && eventDispatcher != null) {
            VirtualEvent<E> vEvent =
                    new VirtualEvent<E>(networkId, VirtualEvent.Type.POSTED, event);
            eventDispatcher.post(vEvent);
        }
    }

    @Override
    public NetworkId networkId() {
        return this.networkId;
    }
}
