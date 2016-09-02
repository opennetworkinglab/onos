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
package org.onosproject.pce.pcestore;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;

import org.onlab.util.KryoNamespace;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.net.resource.ResourceConsumer;
import org.onosproject.pce.pceservice.ExplicitPathInfo;
import org.onosproject.pce.pceservice.constraint.CapabilityConstraint;
import org.onosproject.pce.pceservice.constraint.CostConstraint;
import org.onosproject.pce.pceservice.TunnelConsumerId;
import org.onosproject.pce.pceservice.LspType;
import org.onosproject.pce.pceservice.constraint.SharedBandwidthConstraint;
import org.onosproject.pce.pcestore.api.PceStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the pool of available labels to devices, links and tunnels.
 */
@Component(immediate = true)
@Service
public class DistributedPceStore implements PceStore {
    private static final String PATH_INFO_NULL = "Path Info cannot be null";
    private static final String PCECC_TUNNEL_INFO_NULL = "PCECC Tunnel Info cannot be null";
    private static final String TUNNEL_ID_NULL = "Tunnel Id cannot be null";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    // Mapping tunnel with device local info with tunnel consumer id
    private ConsistentMap<TunnelId, ResourceConsumer> tunnelInfoMap;

    // List of Failed path info
    private DistributedSet<PcePathInfo> failedPathSet;

    // Maintains tunnel name mapped to explicit path info
    private ConsistentMap<String, List<ExplicitPathInfo>> tunnelNameExplicitPathInfoMap;

    private static final Serializer SERIALIZER = Serializer
            .using(new KryoNamespace.Builder().register(KryoNamespaces.API)
                    .register(PcePathInfo.class)
                    .register(CostConstraint.class)
                    .register(CostConstraint.Type.class)
                    .register(BandwidthConstraint.class)
                    .register(SharedBandwidthConstraint.class)
                    .register(CapabilityConstraint.class)
                    .register(CapabilityConstraint.CapabilityType.class)
                    .register(LspType.class)
                    .build());

    @Activate
    protected void activate() {
        tunnelInfoMap = storageService.<TunnelId, ResourceConsumer>consistentMapBuilder()
                .withName("onos-pce-tunnelinfomap")
                .withSerializer(Serializer.using(
                        new KryoNamespace.Builder()
                                .register(KryoNamespaces.API)
                                .register(TunnelId.class,
                                          TunnelConsumerId.class)
                                .build()))
                .build();

        failedPathSet = storageService.<PcePathInfo>setBuilder()
                .withName("failed-path-info")
                .withSerializer(SERIALIZER)
                .build()
                .asDistributedSet();

        tunnelNameExplicitPathInfoMap = storageService.<String, List<ExplicitPathInfo>>consistentMapBuilder()
                .withName("onos-pce-explicitpathinfo")
                .withSerializer(Serializer.using(
                        new KryoNamespace.Builder()
                                .register(KryoNamespaces.API)
                                .register(ExplicitPathInfo.class)
                                .register(ExplicitPathInfo.Type.class)
                                .build()))
                .build();

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public boolean existsTunnelInfo(TunnelId tunnelId) {
        checkNotNull(tunnelId, TUNNEL_ID_NULL);
        return tunnelInfoMap.containsKey(tunnelId);
    }

    @Override
    public boolean existsFailedPathInfo(PcePathInfo failedPathInfo) {
        checkNotNull(failedPathInfo, PATH_INFO_NULL);
        return failedPathSet.contains(failedPathInfo);
    }

    @Override
    public int getTunnelInfoCount() {
        return tunnelInfoMap.size();
    }

    @Override
    public int getFailedPathInfoCount() {
        return failedPathSet.size();
    }

    @Override
    public Map<TunnelId, ResourceConsumer> getTunnelInfos() {
       return tunnelInfoMap.entrySet().stream()
                 .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().value()));
    }

    @Override
    public Iterable<PcePathInfo> getFailedPathInfos() {
       return ImmutableSet.copyOf(failedPathSet);
    }

    @Override
    public ResourceConsumer getTunnelInfo(TunnelId tunnelId) {
        checkNotNull(tunnelId, TUNNEL_ID_NULL);
        return tunnelInfoMap.get(tunnelId) == null ? null : tunnelInfoMap.get(tunnelId).value();
    }

    @Override
    public void addTunnelInfo(TunnelId tunnelId, ResourceConsumer tunnelConsumerId) {
        checkNotNull(tunnelId, TUNNEL_ID_NULL);
        checkNotNull(tunnelConsumerId, PCECC_TUNNEL_INFO_NULL);

        tunnelInfoMap.put(tunnelId, tunnelConsumerId);
    }

    @Override
    public void addFailedPathInfo(PcePathInfo failedPathInfo) {
        checkNotNull(failedPathInfo, PATH_INFO_NULL);
        failedPathSet.add(failedPathInfo);
    }

    @Override
    public boolean removeTunnelInfo(TunnelId tunnelId) {
        checkNotNull(tunnelId, TUNNEL_ID_NULL);

        if (tunnelInfoMap.remove(tunnelId) == null) {
            log.error("Tunnel info deletion for tunnel id {} has failed.", tunnelId.toString());
            return false;
        }
        return true;
    }

    @Override
    public boolean removeFailedPathInfo(PcePathInfo failedPathInfo) {
        checkNotNull(failedPathInfo, PATH_INFO_NULL);

        if (!failedPathSet.remove(failedPathInfo)) {
            log.error("Failed path info {} deletion has failed.", failedPathInfo.toString());
            return false;
        }
        return true;
    }

    @Override
    public boolean tunnelNameExplicitPathInfoMap(String tunnelName, List<ExplicitPathInfo> explicitPathInfo) {
        checkNotNull(tunnelName);
        checkNotNull(explicitPathInfo);
        return tunnelNameExplicitPathInfoMap.put(tunnelName, explicitPathInfo) != null ? true : false;
    }

    @Override
    public List<ExplicitPathInfo> getTunnelNameExplicitPathInfoMap(String tunnelName) {
        checkNotNull(tunnelName);
        return tunnelNameExplicitPathInfoMap.get(tunnelName).value();
    }

}
