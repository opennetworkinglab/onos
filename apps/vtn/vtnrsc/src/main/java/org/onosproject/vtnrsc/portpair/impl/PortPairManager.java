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
package org.onosproject.vtnrsc.portpair.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.UUID;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.MultiValuedTimestamp;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.onosproject.vtnrsc.DefaultPortPair;
import org.onosproject.vtnrsc.PortPair;
import org.onosproject.vtnrsc.PortPairId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.portpair.PortPairEvent;
import org.onosproject.vtnrsc.portpair.PortPairListener;
import org.onosproject.vtnrsc.portpair.PortPairService;
import org.slf4j.Logger;

/**
 * Provides implementation of the portPairService.
 */
@Component(immediate = true)
@Service
public class PortPairManager extends AbstractListenerManager<PortPairEvent, PortPairListener> implements
        PortPairService {

    private static final String PORT_PAIR_ID_NULL = "PortPair ID cannot be null";
    private static final String PORT_PAIR_NULL = "PortPair cannot be null";
    private static final String LISTENER_NOT_NULL = "Listener cannot be null";
    private static final String EVENT_NOT_NULL = "event cannot be null";

    private final Logger log = getLogger(getClass());

    private EventuallyConsistentMap<PortPairId, PortPair> portPairStore;

    private EventuallyConsistentMapListener<PortPairId, PortPair> portPairListener =
            new InnerPortPairStoreListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Activate
    public void activate() {

        eventDispatcher.addSink(PortPairEvent.class, listenerRegistry);
        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(MultiValuedTimestamp.class)
                .register(PortPair.class, PortPairId.class, UUID.class, DefaultPortPair.class, TenantId.class);

        portPairStore = storageService.<PortPairId, PortPair>eventuallyConsistentMapBuilder()
                .withName("portpairstore")
                .withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();

        portPairStore.addListener(portPairListener);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(PortPairEvent.class);
        portPairStore.destroy();
        log.info("Stopped");
    }

    @Override
    public boolean exists(PortPairId portPairId) {
        checkNotNull(portPairId, PORT_PAIR_ID_NULL);
        return portPairStore.containsKey(portPairId);
    }

    @Override
    public int getPortPairCount() {
        return portPairStore.size();
    }

    @Override
    public Iterable<PortPair> getPortPairs() {
        return Collections.unmodifiableCollection(portPairStore.values());
    }

    @Override
    public PortPair getPortPair(PortPairId portPairId) {
        checkNotNull(portPairId, PORT_PAIR_ID_NULL);
        return portPairStore.get(portPairId);
    }

    @Override
    public boolean createPortPair(PortPair portPair) {
        checkNotNull(portPair, PORT_PAIR_NULL);

        portPairStore.put(portPair.portPairId(), portPair);
        if (!portPairStore.containsKey(portPair.portPairId())) {
            log.debug("The portPair is created failed which identifier was {}", portPair.portPairId().toString());
            return false;
        }
        return true;
    }

    @Override
    public boolean updatePortPair(PortPair portPair) {
        checkNotNull(portPair, PORT_PAIR_NULL);

        if (!portPairStore.containsKey(portPair.portPairId())) {
            log.debug("The portPair is not exist whose identifier was {} ", portPair.portPairId().toString());
            return false;
        }

        portPairStore.put(portPair.portPairId(), portPair);

        if (!portPair.equals(portPairStore.get(portPair.portPairId()))) {
            log.debug("The portPair is updated failed whose identifier was {} ", portPair.portPairId().toString());
            return false;
        }
        return true;
    }

    @Override
    public boolean removePortPair(PortPairId portPairId) {
        checkNotNull(portPairId, PORT_PAIR_NULL);

        portPairStore.remove(portPairId);
        if (portPairStore.containsKey(portPairId)) {
            log.debug("The portPair is removed failed whose identifier was {}", portPairId.toString());
            return false;
        }
        return true;
    }

    private class InnerPortPairStoreListener
            implements
            EventuallyConsistentMapListener<PortPairId, PortPair> {

        @Override
        public void event(EventuallyConsistentMapEvent<PortPairId, PortPair> event) {
            checkNotNull(event, EVENT_NOT_NULL);
            PortPair portPair = event.value();
            if (EventuallyConsistentMapEvent.Type.PUT == event.type()) {
                notifyListeners(new PortPairEvent(
                        PortPairEvent.Type.PORT_PAIR_PUT,
                        portPair));
            }
            if (EventuallyConsistentMapEvent.Type.REMOVE == event.type()) {
                notifyListeners(new PortPairEvent(
                        PortPairEvent.Type.PORT_PAIR_DELETE,
                        portPair));
            }
        }
    }

    /**
     * Notifies specify event to all listeners.
     *
     * @param event port pair event
     */
    private void notifyListeners(PortPairEvent event) {
        checkNotNull(event, EVENT_NOT_NULL);
        post(event);
    }
}
