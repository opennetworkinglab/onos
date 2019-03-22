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

import org.onlab.packet.IpPrefix;
import org.onlab.util.KryoNamespace;
import org.onosproject.routing.fpm.api.FpmPrefixStoreEvent;
import org.onosproject.routing.fpm.api.FpmRecord;
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
 * Persistent Fpm Prefix Store with Listener.
 */
@Component(
    immediate = true,
    service = DhcpFpmPrefixStore.class,
    property = {
        "_fpm_type=DHCP"
    }
)
public class DistributedFpmPrefixStore implements DhcpFpmPrefixStore {

    private static final KryoNamespace APP_KRYO = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(FpmRecord.class)
            .register(FpmRecord.Type.class)
            .build();

    private Logger log = getLogger(getClass());
    private StoreDelegate<FpmPrefixStoreEvent> delegate;
    private EventuallyConsistentMap<IpPrefix, FpmRecord> dhcpFpmRecords;
    private EventuallyConsistentMapListener<IpPrefix, FpmRecord> listener;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Activate
    protected void activated() {
        dhcpFpmRecords = storageService.<IpPrefix, FpmRecord>eventuallyConsistentMapBuilder()
                .withName("DHCP-FPM-Records")
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .withSerializer(APP_KRYO)
                .withPersistence()
                .build();
        listener = new InternalMapListener();
        dhcpFpmRecords.addListener(listener);
    }

    @Deactivate
    protected void deactivated() {
        dhcpFpmRecords.removeListener(listener);
        dhcpFpmRecords.destroy().join();
    }

    @Override
    public void setDelegate(StoreDelegate<FpmPrefixStoreEvent> delegate) {
        checkNotNull(delegate, "Delegate can't be null");
        this.delegate = delegate;
    }

    @Override
    public void unsetDelegate(StoreDelegate<FpmPrefixStoreEvent> delegate) {
        this.delegate = null;
    }

    @Override
    public boolean hasDelegate() {
        return delegate != null;
    }

    @Override
    public Optional<FpmRecord> getFpmRecord(IpPrefix prefix) {
        checkNotNull(prefix, "Prefix can't be null");
        return Optional.ofNullable(dhcpFpmRecords.get(prefix));
    }

    @Override
    public Collection<FpmRecord> getFpmRecords() {
        return dhcpFpmRecords.values();
    }

    /**
     * Add a dhcp fpm entry.
     *
     * @param prefix the route prefix in the advertisement
     * @param fpmRecord the route for fpm
     **/
    @Override
    public void addFpmRecord(IpPrefix prefix, FpmRecord fpmRecord) {
        checkNotNull(prefix, "Prefix can't be null");
        checkNotNull(fpmRecord, "Fpm record can't be null");
        dhcpFpmRecords.put(prefix, fpmRecord);
    }

    /**
     * Remove a dhcp fpm entry.
     *
     * @param prefix the route prefix in the advertisement
     * @return none
     **/
    @Override
    public Optional<FpmRecord> removeFpmRecord(IpPrefix prefix) {
        checkNotNull(prefix, "Prefix can't be null");
        return Optional.ofNullable(dhcpFpmRecords.remove(prefix));
    }

    /**
     * Internal map listener for Fpm records map.
     */
    private class InternalMapListener implements EventuallyConsistentMapListener<IpPrefix, FpmRecord> {
        @Override
        public void event(EventuallyConsistentMapEvent<IpPrefix, FpmRecord> event) {
            FpmPrefixStoreEvent.Type eventType;
            switch (event.type()) {
                case PUT:
                    eventType = FpmPrefixStoreEvent.Type.ADD;
                    break;
                case REMOVE:
                    eventType = FpmPrefixStoreEvent.Type.REMOVE;
                    break;
                default:
                    log.warn("Unknown event type {}", event.type());
                    return;
            }
            if (delegate != null) {
                delegate.notify(new FpmPrefixStoreEvent(eventType, event.value()));
            }
        }
    }
}
