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

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.pce.pceservice.ExplicitPathInfo;
import org.onosproject.pce.pceservice.LspType;
import org.onosproject.pce.pceservice.constraint.CapabilityConstraint;
import org.onosproject.pce.pceservice.constraint.CostConstraint;
import org.onosproject.pce.pceservice.constraint.PceBandwidthConstraint;
import org.onosproject.pce.pceservice.constraint.SharedBandwidthConstraint;
import org.onosproject.pce.pcestore.api.PceStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

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

    //Mapping tunnel name with Disjoint paths
    private ConsistentMap<String, List<TunnelId>> tunnelNameDisjoinTunnelIdInfo;

    // List of Failed path info
    private DistributedSet<PcePathInfo> failedPathSet;

    // Maintains tunnel name mapped to explicit path info
    private ConsistentMap<String, List<ExplicitPathInfo>> tunnelNameExplicitPathInfoMap;

    private static final Serializer SERIALIZER = Serializer
            .using(new KryoNamespace.Builder().register(KryoNamespaces.API)
                    .register(PcePathInfo.class)
                    .register(ExplicitPathInfo.class)
                    .register(ExplicitPathInfo.Type.class)
                    .register(CostConstraint.class)
                    .register(CostConstraint.Type.class)
                    .register(PceBandwidthConstraint.class)
                    .register(SharedBandwidthConstraint.class)
                    .register(CapabilityConstraint.class)
                    .register(CapabilityConstraint.CapabilityType.class)
                    .register(LspType.class)
                    .build());

    @Activate
    protected void activate() {

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

        tunnelNameDisjoinTunnelIdInfo = storageService.<String, List<TunnelId>>consistentMapBuilder()
                .withName("onos-pce-disjointTunnelIds")
                .withSerializer(Serializer.using(
                        new KryoNamespace.Builder()
                                .register(KryoNamespaces.API)
                                .register(TunnelId.class)
                                .build()))
                .build();

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public boolean existsFailedPathInfo(PcePathInfo failedPathInfo) {
        checkNotNull(failedPathInfo, PATH_INFO_NULL);
        return failedPathSet.contains(failedPathInfo);
    }


    @Override
    public int getFailedPathInfoCount() {
        return failedPathSet.size();
    }

    @Override
    public Iterable<PcePathInfo> getFailedPathInfos() {
       return ImmutableSet.copyOf(failedPathSet);
    }



    @Override
    public void addFailedPathInfo(PcePathInfo failedPathInfo) {
        checkNotNull(failedPathInfo, PATH_INFO_NULL);
        failedPathSet.add(failedPathInfo);
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
        if (tunnelNameExplicitPathInfoMap.get(tunnelName) != null) {
            return tunnelNameExplicitPathInfoMap.get(tunnelName).value();
        }
        return null;
    }

/*    @Override
    public DisjointPath getDisjointPaths(String tunnelName) {
        if (tunnelNameDisjointPathInfo.get(tunnelName) != null) {
            return tunnelNameDisjointPathInfo.get(tunnelName).value();
        }
        return null;
    }

    @Override
    public boolean addDisjointPathInfo(String tunnelName, DisjointPath path) {
        checkNotNull(tunnelName);
        checkNotNull(path);
        return tunnelNameDisjointPathInfo.put(tunnelName, path) != null ? true : false;
    }*/

    @Override
    public boolean addLoadBalancingTunnelIdsInfo(String tunnelName, TunnelId... tunnelIds) {
        checkNotNull(tunnelName);
        checkNotNull(tunnelIds);
        return tunnelNameDisjoinTunnelIdInfo.put(tunnelName, Arrays.asList(tunnelIds)) != null ? true : false;
    }

    @Override
    public List<TunnelId> getLoadBalancingTunnelIds(String tunnelName) {
        if (tunnelNameDisjoinTunnelIdInfo.get(tunnelName) != null) {
            return tunnelNameDisjoinTunnelIdInfo.get(tunnelName).value();
        }
        return null;
    }

    @Override
    public boolean removeLoadBalancingTunnelIdsInfo(String tunnelName) {
        if (tunnelNameDisjoinTunnelIdInfo.remove(tunnelName) == null) {
            log.error("Failed to remove entry {} for this tunnelName in DisjointTunnelIdsInfoMap" + tunnelName);
            return false;
        }
        return true;
    }

 /*   @Override
    public boolean removeDisjointPathInfo(String tunnelName) {
        if (tunnelNameDisjointPathInfo.remove(tunnelName) == null) {
            log.error("Failed to remove entry {} for this tunnelName in DisjointPathInfoMap", tunnelName);
            return false;
        }
        return true;
    }*/
}
