/*
 * Copyright 2018-present Open Networking Foundation
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
 *
 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */

package org.onosproject.drivers.odtn.impl;

import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.FlowId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Stores a set of Rules for given open config based devices in order to properly report them to the store.
 */
public final class DeviceConnectionCache {
    private static final Logger log =
            LoggerFactory.getLogger(DeviceConnectionCache.class);

    private static final StorageService STORAGE_SERVICE = DefaultServiceDirectory.getService(StorageService.class);

    private static final String MAP_NAME = "onos-odtn-flow-cache";

    private static final KryoNamespace SERIALIZER = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .nextId(KryoNamespaces.BEGIN_USER_CUSTOM_ID)
            .register(DeviceConnection.class).build();

    private EventuallyConsistentMap<DeviceId, Set<DeviceConnection>>
            flowCache;

    private static DeviceConnectionCache cache = null;
    private static final Object CACHE_LOCK = new Object();

    //banning public construction
    private DeviceConnectionCache() {
        flowCache = STORAGE_SERVICE
                .<DeviceId, Set<DeviceConnection>>eventuallyConsistentMapBuilder()
                .withName(MAP_NAME)
                .withSerializer(SERIALIZER)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();
    }

    /**
     * Initializes the cache if not already present.
     * If present returns the existing one.
     *
     * @return single instance of cache
     */
    public static DeviceConnectionCache init() {
        synchronized (CACHE_LOCK) {
            if (cache == null) {
                cache = new DeviceConnectionCache();
            }
        }
        return cache;
    }

    /**
     * Returns the number of rules stored for a given device.
     *
     * @param did the device
     * @return number of flows stored
     */
    public int size(DeviceId did) {
        if (!flowCache.containsKey(did)) {
            return 0;
        }
        return flowCache.get(did).size();
    }

    /**
     * Returns the flow with given Id for the specific device.
     *
     * @param did    device id
     * @param flowId flow id
     * @return the flow rule
     */
    public DeviceConnection get(DeviceId did, FlowId flowId) {
        if (!flowCache.containsKey(did)) {
            return null;
        }
        Set<DeviceConnection> set = flowCache.get(did);
        return set.stream()
                .filter(c -> c.getFlowRule().id() == flowId)
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the flow with given Id for the specific device.
     *
     * @param did          device id
     * @param connectionId the device specific connection id
     * @return the flow rule
     */
    public FlowRule get(DeviceId did, String connectionId) {
        if (!flowCache.containsKey(did)) {
            return null;
        }
        Set<DeviceConnection> set = flowCache.get(did);
        DeviceConnection connection = set.stream()
                .filter(c -> c.getId().equals(connectionId))
                .findFirst()
                .orElse(null);
        return connection != null ? connection.getFlowRule() : null;
    }

    /**
     * Returns all the flows for the specific device.
     *
     * @param did device id
     * @return Set of flow rules
     */
    public Set<FlowRule> get(DeviceId did) {
        if (!flowCache.containsKey(did)) {
            return null;
        }
        return flowCache.get(did).stream()
                .map(DeviceConnection::getFlowRule)
                .collect(Collectors.toSet());
    }

    /**
     * Add to a specific device a flow and a device specific connection id for that flow.
     *
     * @param did          device id
     * @param connectionId the device specific connection identifier
     * @param flowRule     the flow rule
     */
    public void add(DeviceId did, String connectionId, FlowRule flowRule) {
        Set<DeviceConnection> set;
        if (flowCache.containsKey(did)) {
            set = flowCache.get(did);
        } else {
            set = new HashSet<>();
            log.debug("DeviceConnectionCache created for {}", did);
            flowCache.put(did, set);
        }
        set.add(DeviceConnection.of(connectionId, flowRule));
    }

    /**
     * Add a flows for the specific device.
     *
     * @param did      device id
     * @param flowRule the flow rule
     */
    public void remove(DeviceId did, FlowRule flowRule) {
        if (!flowCache.containsKey(did)) {
            return;
        }
        Set<DeviceConnection> set = flowCache.get(did);
        set.removeIf(r2 -> r2.getFlowRule().id() == flowRule.id());
    }

    /**
     * Add a flows for the specific device.
     *
     * @param did          device id
     * @param connectionId the connectionId as identified on the Device
     */
    public void remove(DeviceId did, String connectionId) {
        if (!flowCache.containsKey(did)) {
            return;
        }
        Set<DeviceConnection> set = flowCache.get(did);
        set.removeIf(r2 -> r2.getId().equals(connectionId));
    }
}
