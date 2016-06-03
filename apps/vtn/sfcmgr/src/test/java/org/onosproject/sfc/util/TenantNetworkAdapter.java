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
package org.onosproject.sfc.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.onosproject.vtnrsc.TenantNetwork;
import org.onosproject.vtnrsc.TenantNetworkId;
import org.onosproject.vtnrsc.tenantnetwork.TenantNetworkService;

import com.google.common.collect.ImmutableList;

/**
 * Provides implementation of the VtnRsc service.
 */
public class TenantNetworkAdapter implements TenantNetworkService {

    private final ConcurrentMap<TenantNetworkId, TenantNetwork> tenantNetworkStore = new ConcurrentHashMap<>();

    @Override
    public boolean exists(TenantNetworkId networkId) {
        return tenantNetworkStore.containsKey(networkId);
    }

    @Override
    public int getNetworkCount() {
        return tenantNetworkStore.size();
    }

    @Override
    public Iterable<TenantNetwork> getNetworks() {
        return ImmutableList.copyOf(tenantNetworkStore.values());
    }

    @Override
    public TenantNetwork getNetwork(TenantNetworkId networkId) {
        return tenantNetworkStore.get(networkId);
    }

    @Override
    public boolean createNetworks(Iterable<TenantNetwork> networks) {
        for (TenantNetwork network : networks) {
            tenantNetworkStore.put(network.id(), network);
            if (!tenantNetworkStore.containsKey(network.id())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean updateNetworks(Iterable<TenantNetwork> networks) {
        return false;
    }

    @Override
    public boolean removeNetworks(Iterable<TenantNetworkId> networksIds) {
        return false;
    }

}
