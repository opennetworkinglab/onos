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
package org.onosproject.vtnrsc.portchain.impl;

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
import org.onosproject.net.DeviceId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.MultiValuedTimestamp;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.onosproject.vtnrsc.DefaultPortChain;
import org.onosproject.vtnrsc.FiveTuple;
import org.onosproject.vtnrsc.FlowClassifierId;
import org.onosproject.vtnrsc.LoadBalanceId;
import org.onosproject.vtnrsc.PortChain;
import org.onosproject.vtnrsc.PortChainId;
import org.onosproject.vtnrsc.PortPairGroupId;
import org.onosproject.vtnrsc.PortPairId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.portchain.PortChainEvent;
import org.onosproject.vtnrsc.portchain.PortChainListener;
import org.onosproject.vtnrsc.portchain.PortChainService;
import org.slf4j.Logger;

/**
 * Provides implementation of the portChainService.
 */
@Component(immediate = true)
@Service
public class PortChainManager extends AbstractListenerManager<PortChainEvent, PortChainListener> implements
        PortChainService {

    private static final String PORT_CHAIN_ID_NULL = "PortChain ID cannot be null";
    private static final String PORT_CHAIN_NULL = "PortChain cannot be null";
    private static final String EVENT_NOT_NULL = "event cannot be null";

    private final Logger log = getLogger(getClass());
    private EventuallyConsistentMap<PortChainId, PortChain> portChainStore;

    private EventuallyConsistentMapListener<PortChainId, PortChain> portChainListener =
            new InnerPortChainStoreListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Activate
    public void activate() {

        eventDispatcher.addSink(PortChainEvent.class, listenerRegistry);

        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(MultiValuedTimestamp.class)
                .register(PortChain.class, PortChainId.class, UUID.class, PortPairGroupId.class,
                          FlowClassifierId.class, FiveTuple.class, LoadBalanceId.class, DeviceId.class,
                          DefaultPortChain.class, PortPairId.class, TenantId.class);

        portChainStore = storageService
                .<PortChainId, PortChain>eventuallyConsistentMapBuilder()
                .withName("portchainstore").withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp()).build();

        portChainStore.addListener(portChainListener);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(PortChainEvent.class);
        portChainStore.removeListener(portChainListener);
        portChainStore.destroy();
        log.info("Stopped");
    }

    @Override
    public boolean exists(PortChainId portChainId) {
        checkNotNull(portChainId, PORT_CHAIN_ID_NULL);
        return portChainStore.containsKey(portChainId);
    }

    @Override
    public int getPortChainCount() {
        return portChainStore.size();
    }

    @Override
    public Iterable<PortChain> getPortChains() {
        return Collections.unmodifiableCollection(portChainStore.values());
    }

    @Override
    public PortChain getPortChain(PortChainId portChainId) {
        checkNotNull(portChainId, PORT_CHAIN_ID_NULL);
        return portChainStore.get(portChainId);
    }

    @Override
    public boolean createPortChain(PortChain portChain) {
        checkNotNull(portChain, PORT_CHAIN_NULL);

        portChainStore.put(portChain.portChainId(), portChain);
        if (!portChainStore.containsKey(portChain.portChainId())) {
            log.error("The portChain created is failed which identifier was {}", portChain.portChainId()
                      .toString());
            return false;
        }
        return true;
    }

    @Override
    public boolean updatePortChain(PortChain portChain) {
        checkNotNull(portChain, PORT_CHAIN_NULL);
        PortChain oldPortChain = null;
        if (!portChainStore.containsKey(portChain.portChainId())) {
            log.warn("The portChain is not exist whose identifier was {} ",
                     portChain.portChainId().toString());
            return false;
        } else {
            oldPortChain = portChainStore.get(portChain.portChainId());
        }
        PortChain newPortChain = DefaultPortChain.create(portChain, oldPortChain);
        portChainStore.put(newPortChain.portChainId(), newPortChain);

        if (!newPortChain.equals(portChainStore.get(newPortChain.portChainId()))) {
            log.debug("The portChain is updated failed whose identifier was {} ",
                      newPortChain.portChainId().toString());
            return false;
        }
        return true;
    }

    @Override
    public boolean removePortChain(PortChainId portChainId) {
        checkNotNull(portChainId, PORT_CHAIN_NULL);

        portChainStore.remove(portChainId);
        if (portChainStore.containsKey(portChainId)) {
            log.debug("The portChain is removed failed whose identifier was {}",
                      portChainId.toString());
            return false;
        }
        return true;
    }

    private class InnerPortChainStoreListener
            implements
            EventuallyConsistentMapListener<PortChainId, PortChain> {

        @Override
        public void event(EventuallyConsistentMapEvent<PortChainId, PortChain> event) {
            checkNotNull(event, EVENT_NOT_NULL);
            PortChain portChain = event.value();
            if (EventuallyConsistentMapEvent.Type.PUT == event.type()) {
                notifyListeners(new PortChainEvent(
                        PortChainEvent.Type.PORT_CHAIN_PUT,
                        portChain));
            }
            if (EventuallyConsistentMapEvent.Type.REMOVE == event.type()) {
                notifyListeners(new PortChainEvent(
                        PortChainEvent.Type.PORT_CHAIN_DELETE,
                        portChain));
            }
        }
    }

    /**
     * Notifies specify event to all listeners.
     *
     * @param event port chain event
     */
    private void notifyListeners(PortChainEvent event) {
        checkNotNull(event, EVENT_NOT_NULL);
        post(event);
    }
}
