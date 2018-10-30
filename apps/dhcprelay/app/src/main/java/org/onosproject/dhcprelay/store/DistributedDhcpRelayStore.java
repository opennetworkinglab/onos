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

package org.onosproject.dhcprelay.store;

import org.onlab.packet.DHCP;
import org.onlab.packet.DHCP6;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.HostId;
import org.onosproject.store.StoreDelegate;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Distributed DHCP relay store.
 */
@Component(immediate = true, service = DhcpRelayStore.class)
public class DistributedDhcpRelayStore implements DhcpRelayStore {
    private static final KryoNamespace APP_KRYO = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(DhcpRecord.class)
            .register(DHCP.MsgType.class)
            .register(DHCP6.MsgType.class)
            .register(DhcpRelayCounters.class)
            .build();

    private Logger log = getLogger(getClass());
    private StoreDelegate<DhcpRelayStoreEvent> delegate;
    private EventuallyConsistentMap<HostId, DhcpRecord> dhcpRecords;
    private EventuallyConsistentMapListener<HostId, DhcpRecord> listener;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Activate
    protected void activated() {
        dhcpRecords = storageService.<HostId, DhcpRecord>eventuallyConsistentMapBuilder()
                .withName("DHCP-Relay-Records")
                .withTimestampProvider((hostId, record) -> {
                    if (record != null) {
                        return new WallClockTimestamp(record.lastSeen());
                    } else {
                        return new WallClockTimestamp();
                    }
                })
                .withSerializer(APP_KRYO)
                .build();
        listener = new InternalMapListener();
        dhcpRecords.addListener(listener);
    }

    @Deactivate
    protected void deactivated() {
        dhcpRecords.removeListener(listener);
        dhcpRecords.destroy().join();
    }

    @Override
    public void setDelegate(StoreDelegate<DhcpRelayStoreEvent> delegate) {
        checkNotNull(delegate, "Delegate can't be null");
        this.delegate = delegate;
    }

    @Override
    public void unsetDelegate(StoreDelegate<DhcpRelayStoreEvent> delegate) {
        this.delegate = null;
    }

    @Override
    public boolean hasDelegate() {
        return delegate != null;
    }

    @Override
    public void updateDhcpRecord(HostId hostId, DhcpRecord dhcpRecord) {
        checkNotNull(hostId, "Host id can't be null");
        checkNotNull(dhcpRecord, "DHCP record can't be null");
        dhcpRecords.put(hostId, dhcpRecord);
    }

    @Override
    public Optional<DhcpRecord> getDhcpRecord(HostId hostId) {
        checkNotNull(hostId, "Host id can't be null");
        return Optional.ofNullable(dhcpRecords.get(hostId));
    }

    @Override
    public Collection<DhcpRecord> getDhcpRecords() {
        return dhcpRecords.values();
    }

    @Override
    public Optional<DhcpRecord> removeDhcpRecord(HostId hostId) {
        checkNotNull(hostId, "Host id can't be null");
        return Optional.ofNullable(dhcpRecords.remove(hostId));
    }

    /**
     * Internal map listener for DHCP records map.
     */
    private class InternalMapListener implements EventuallyConsistentMapListener<HostId, DhcpRecord> {
        @Override
        public void event(EventuallyConsistentMapEvent<HostId, DhcpRecord> event) {
            DhcpRelayStoreEvent.Type eventType;
            switch (event.type()) {
                case PUT:
                    eventType = DhcpRelayStoreEvent.Type.UPDATED;
                    break;
                case REMOVE:
                    eventType = DhcpRelayStoreEvent.Type.REMOVED;
                    break;
                default:
                    log.warn("Unknown event type {}", event.type());
                    return;
            }
            if (delegate != null) {
                delegate.notify(new DhcpRelayStoreEvent(eventType, event.value()));
            }
        }
    }
}
