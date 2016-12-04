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

package org.onosproject.tetunnel.impl;

import com.google.common.collect.ImmutableList;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.LogicalClockService;
import org.onosproject.store.service.StorageService;
import org.onosproject.tetopology.management.api.TeTopologyKey;
import org.onosproject.tetunnel.api.lsp.TeLsp;
import org.onosproject.tetunnel.api.lsp.TeLspKey;
import org.onosproject.tetunnel.api.tunnel.TeTunnel;
import org.onosproject.tetunnel.api.TeTunnelStore;
import org.onosproject.tetunnel.api.tunnel.TeTunnelKey;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages the TE tunnel attributes using an eventually consistent map.
 */
@Component(immediate = true)
@Service
public class DistributedTeTunnelStore implements TeTunnelStore {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LogicalClockService clockService;

    private EventuallyConsistentMap<TeTunnelKey, TeTunnel> teTunnels;
    private EventuallyConsistentMap<TeTunnelKey, TunnelId> tunnelIds;
    private EventuallyConsistentMap<TeLspKey, TeLsp> lsps;

    @Activate
    public void activate() {
        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .nextId(KryoNamespaces.BEGIN_USER_CUSTOM_ID)
                .register(TeTunnel.class);

        teTunnels = storageService.<TeTunnelKey, TeTunnel>eventuallyConsistentMapBuilder()
                .withName("TeTunnelStore")
                .withSerializer(serializer)
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();

        tunnelIds = storageService.<TeTunnelKey, TunnelId>eventuallyConsistentMapBuilder()
                .withName("TeTunnelIdStore")
                .withSerializer(serializer)
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();

        lsps = storageService.<TeLspKey, TeLsp>eventuallyConsistentMapBuilder()
                .withName("TeLspStore")
                .withSerializer(serializer)
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        teTunnels.destroy();
        tunnelIds.destroy();
        lsps.destroy();

        log.info("Stopped");
    }

    @Override
    public boolean addTeTunnel(TeTunnel teTunnel) {
        if (teTunnel == null) {
            log.warn("teTunnel is null");
            return false;
        }
        if (teTunnels.containsKey(teTunnel.teTunnelKey())) {
            log.warn("teTunnel already exist");
            return false;
        }
        teTunnels.put(teTunnel.teTunnelKey(), teTunnel);
        return true;
    }

    @Override
    public void setTunnelId(TeTunnelKey teTunnelKey, TunnelId tunnelId) {
        tunnelIds.put(teTunnelKey, tunnelId);
    }

    @Override
    public TunnelId getTunnelId(TeTunnelKey teTunnelKey) {
        return tunnelIds.get(teTunnelKey);
    }

    @Override
    public void updateTeTunnel(TeTunnel teTunnel) {
        if (teTunnel == null) {
            log.warn("TeTunnel is null");
            return;
        }
        teTunnels.put(teTunnel.teTunnelKey(), teTunnel);
    }

    @Override
    public void removeTeTunnel(TeTunnelKey teTunnelKey) {
        teTunnels.remove(teTunnelKey);
        tunnelIds.remove(teTunnelKey);
    }

    @Override
    public TeTunnel getTeTunnel(TeTunnelKey teTunnelKey) {
        return teTunnels.get(teTunnelKey);
    }

    @Override
    public TeTunnel getTeTunnel(TunnelId tunnelId) {
        if (tunnelIds.containsValue(tunnelId)) {
            for (TeTunnel teTunnel : teTunnels.values()) {
                if (tunnelIds.get(teTunnel.teTunnelKey()).equals(tunnelId)) {
                    return teTunnel;
                }
            }
        }
        return null;
    }

    @Override
    public Collection<TeTunnel> getTeTunnels() {
        return ImmutableList.copyOf(teTunnels.values());
    }

    @Override
    public Collection<TeTunnel> getTeTunnels(TeTunnel.Type type) {
        return ImmutableList.copyOf(teTunnels.values()
                .stream()
                .filter(teTunnel -> teTunnel.type().equals(type))
                .collect(Collectors.toList()));
    }

    @Override
    public Collection<TeTunnel> getTeTunnels(TeTopologyKey teTopologyKey) {
        return ImmutableList.copyOf(teTunnels.values()
                .stream()
                .filter(teTunnel -> teTunnel.teTunnelKey()
                        .teTopologyKey()
                        .equals(teTopologyKey))
                .collect(Collectors.toList()));
    }

    @Override
    public boolean addTeLsp(TeLsp lsp) {
        if (lsp == null) {
            log.warn("TeLsp is null");
            return false;
        }
        if (lsps.containsKey(lsp.teLspKey())) {
            log.error("TeLsp exist {}", lsp.teLspKey());
            return false;
        }
        lsps.put(lsp.teLspKey(), lsp);
        return true;
    }

    @Override
    public void updateTeLsp(TeLsp lsp) {
        if (lsp == null) {
            log.warn("TeLsp is null");
            return;
        }
        lsps.put(lsp.teLspKey(), lsp);
    }

    @Override
    public void removeTeLsp(TeLspKey key) {
        lsps.remove(key);
    }

    @Override
    public TeLsp getTeLsp(TeLspKey key) {
        return lsps.get(key);
    }

    @Override
    public Collection<TeLsp> getTeLsps() {
        return ImmutableList.copyOf(lsps.values());
    }
}
