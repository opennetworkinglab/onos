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

package org.onosproject.incubator.store.virtual.impl;

import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkFlowObjectiveStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentMap;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Distributed flow objective store for virtual network.
 */
@Component(immediate = true, enabled = false)
@Service
public class DistributedVirtualFlowObjectiveStore
        extends SimpleVirtualFlowObjectiveStore
        implements VirtualNetworkFlowObjectiveStore {

    private final Logger log = getLogger(getClass());

    private ConsistentMap<NetworkId, ConcurrentMap<Integer, byte[]>> nextGroupsMap;
    private static final String VNET_FLOW_OBJ_GROUP_MAP_NAME =
            "onos-networkId-flowobjective-groups";
    private static final String VNET_FLOW_OBJ_GROUP_MAP_FRIENDLYNAME =
            "DistributedVirtualFlowObjectiveStore";

    @Override
    protected void initNextGroupsMap() {
        nextGroupsMap = storageService.<NetworkId, ConcurrentMap<Integer, byte[]>>consistentMapBuilder()
                .withName(VNET_FLOW_OBJ_GROUP_MAP_NAME)
                .withSerializer(Serializer.using(
                        new KryoNamespace.Builder()
                                .register(KryoNamespaces.API)
                                .register(NetworkId.class)
                                .build(VNET_FLOW_OBJ_GROUP_MAP_FRIENDLYNAME)))
                .build();

    }

    @Override
    protected ConcurrentMap<Integer, byte[]> getNextGroups(NetworkId networkId) {
        nextGroupsMap.computeIfAbsent(networkId, n -> {
            log.debug("getNextGroups - creating new ConcurrentMap");
            return Maps.newConcurrentMap();
        });

        return nextGroupsMap.get(networkId).value();
    }

    @Override
    protected void updateNextGroupsMap(NetworkId networkId, ConcurrentMap<Integer,
            byte[]> nextGroups) {
        nextGroupsMap.put(networkId, nextGroups);
    }

}
