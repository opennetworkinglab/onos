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

import java.util.HashMap;
import java.util.HashSet;
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
import org.onosproject.incubator.net.resource.label.LabelResource;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.resource.ResourceConsumer;
import org.onosproject.pce.pceservice.constraint.CapabilityConstraint;
import org.onosproject.pce.pceservice.constraint.CostConstraint;
import org.onosproject.pce.pceservice.TunnelConsumerId;
import org.onosproject.pce.pceservice.LspType;
import org.onosproject.pce.pcestore.api.LspLocalLabelInfo;
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

    private static final String DEVICE_ID_NULL = "Device ID cannot be null";
    private static final String DEVICE_LABEL_STORE_INFO_NULL = "Device Label Store cannot be null";
    private static final String LABEL_RESOURCE_ID_NULL = "Label Resource Id cannot be null";
    private static final String LABEL_RESOURCE_LIST_NULL = "Label Resource List cannot be null";
    private static final String LABEL_RESOURCE_NULL = "Label Resource cannot be null";
    private static final String LINK_NULL = "LINK cannot be null";
    private static final String LSP_LOCAL_LABEL_INFO_NULL = "LSP Local Label Info cannot be null";
    private static final String PATH_INFO_NULL = "Path Info cannot be null";
    private static final String PCECC_TUNNEL_INFO_NULL = "PCECC Tunnel Info cannot be null";
    private static final String TUNNEL_ID_NULL = "Tunnel Id cannot be null";
    private static final String TUNNEL_CONSUMER_ID_NULL = "Tunnel consumer Id cannot be null";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    // Mapping device with global node label
    private ConsistentMap<DeviceId, LabelResourceId> globalNodeLabelMap;

    // Mapping link with adjacency label
    private ConsistentMap<Link, LabelResourceId> adjLabelMap;

    // Mapping tunnel with device local info with tunnel consumer id
    private ConsistentMap<TunnelId, PceccTunnelInfo> tunnelInfoMap;

    // List of Failed path info
    private DistributedSet<PcePathInfo> failedPathSet;

    // Locally maintain LSRID to device id mapping for better performance.
    private Map<String, DeviceId> lsrIdDeviceIdMap = new HashMap<>();

    // List of PCC LSR ids whose BGP device information was not available to perform
    // label db sync.
    private HashSet<DeviceId> pendinglabelDbSyncPccMap = new HashSet();

    @Activate
    protected void activate() {
        globalNodeLabelMap = storageService.<DeviceId, LabelResourceId>consistentMapBuilder()
                .withName("onos-pce-globalnodelabelmap")
                .withSerializer(Serializer.using(
                        new KryoNamespace.Builder()
                                .register(KryoNamespaces.API)
                                .register(LabelResourceId.class)
                                .build()))
                .build();

        adjLabelMap = storageService.<Link, LabelResourceId>consistentMapBuilder()
                .withName("onos-pce-adjlabelmap")
                .withSerializer(Serializer.using(
                        new KryoNamespace.Builder()
                                .register(KryoNamespaces.API)
                                .register(Link.class,
                                          LabelResource.class,
                                          LabelResourceId.class)
                                .build()))
                .build();

        tunnelInfoMap = storageService.<TunnelId, PceccTunnelInfo>consistentMapBuilder()
                .withName("onos-pce-tunnelinfomap")
                .withSerializer(Serializer.using(
                        new KryoNamespace.Builder()
                                .register(KryoNamespaces.API)
                                .register(TunnelId.class,
                                          PceccTunnelInfo.class,
                                          DefaultLspLocalLabelInfo.class,
                                          TunnelConsumerId.class,
                                          LabelResourceId.class)
                                .build()))
                .build();

        failedPathSet = storageService.<PcePathInfo>setBuilder()
                .withName("failed-path-info")
                .withSerializer(Serializer.using(
                        new KryoNamespace.Builder()
                                .register(KryoNamespaces.API)
                                .register(PcePathInfo.class,
                                          CostConstraint.class,
                                          CostConstraint.Type.class,
                                          BandwidthConstraint.class,
                                          CapabilityConstraint.class,
                                          CapabilityConstraint.CapabilityType.class,
                                          LspType.class)
                                .build()))

                .build()
                .asDistributedSet();

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public boolean existsGlobalNodeLabel(DeviceId id) {
        checkNotNull(id, DEVICE_ID_NULL);
        return globalNodeLabelMap.containsKey(id);
    }

    @Override
    public boolean existsAdjLabel(Link link) {
        checkNotNull(link, LINK_NULL);
        return adjLabelMap.containsKey(link);
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
    public int getGlobalNodeLabelCount() {
        return globalNodeLabelMap.size();
    }

    @Override
    public int getAdjLabelCount() {
        return adjLabelMap.size();
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
    public Map<DeviceId, LabelResourceId> getGlobalNodeLabels() {
       return globalNodeLabelMap.entrySet().stream()
                 .collect(Collectors.toMap(Map.Entry::getKey, e -> (LabelResourceId) e.getValue().value()));
    }

    @Override
    public Map<Link, LabelResourceId> getAdjLabels() {
       return adjLabelMap.entrySet().stream()
                 .collect(Collectors.toMap(Map.Entry::getKey, e -> (LabelResourceId) e.getValue().value()));
    }

    @Override
    public Map<TunnelId, PceccTunnelInfo> getTunnelInfos() {
       return tunnelInfoMap.entrySet().stream()
                 .collect(Collectors.toMap(Map.Entry::getKey, e -> (PceccTunnelInfo) e.getValue().value()));
    }

    @Override
    public Iterable<PcePathInfo> getFailedPathInfos() {
       return ImmutableSet.copyOf(failedPathSet);
    }

    @Override
    public LabelResourceId getGlobalNodeLabel(DeviceId id) {
        checkNotNull(id, DEVICE_ID_NULL);
        return globalNodeLabelMap.get(id) == null ? null : globalNodeLabelMap.get(id).value();
    }

    @Override
    public LabelResourceId getAdjLabel(Link link) {
        checkNotNull(link, LINK_NULL);
        return adjLabelMap.get(link) == null ? null : adjLabelMap.get(link).value();
    }

    @Override
    public PceccTunnelInfo getTunnelInfo(TunnelId tunnelId) {
        checkNotNull(tunnelId, TUNNEL_ID_NULL);
        return tunnelInfoMap.get(tunnelId) == null ? null : tunnelInfoMap.get(tunnelId).value();
    }

    @Override
    public void addGlobalNodeLabel(DeviceId deviceId, LabelResourceId labelId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        checkNotNull(labelId, LABEL_RESOURCE_ID_NULL);

        globalNodeLabelMap.put(deviceId, labelId);
    }

    @Override
    public void addAdjLabel(Link link, LabelResourceId labelId) {
        checkNotNull(link, LINK_NULL);
        checkNotNull(labelId, LABEL_RESOURCE_ID_NULL);

        adjLabelMap.put(link, labelId);
    }

    @Override
    public void addTunnelInfo(TunnelId tunnelId, PceccTunnelInfo pceccTunnelInfo) {
        checkNotNull(tunnelId, TUNNEL_ID_NULL);
        checkNotNull(pceccTunnelInfo, PCECC_TUNNEL_INFO_NULL);

        tunnelInfoMap.put(tunnelId, pceccTunnelInfo);
    }

    @Override
    public void addFailedPathInfo(PcePathInfo failedPathInfo) {
        checkNotNull(failedPathInfo, PATH_INFO_NULL);
        failedPathSet.add(failedPathInfo);
    }

    @Override
    public boolean updateTunnelInfo(TunnelId tunnelId, List<LspLocalLabelInfo> lspLocalLabelInfoList) {
        checkNotNull(tunnelId, TUNNEL_ID_NULL);
        checkNotNull(lspLocalLabelInfoList, LSP_LOCAL_LABEL_INFO_NULL);

        if (!tunnelInfoMap.containsKey((tunnelId))) {
            log.debug("Tunnel info does not exist whose tunnel id is {}.", tunnelId.toString());
            return false;
        }

        PceccTunnelInfo tunnelInfo = tunnelInfoMap.get(tunnelId).value();
        tunnelInfo.lspLocalLabelInfoList(lspLocalLabelInfoList);
        tunnelInfoMap.put(tunnelId, tunnelInfo);

        return true;
    }

    @Override
    public boolean updateTunnelInfo(TunnelId tunnelId, ResourceConsumer tunnelConsumerId) {
        checkNotNull(tunnelId, TUNNEL_ID_NULL);
        checkNotNull(tunnelConsumerId, TUNNEL_CONSUMER_ID_NULL);

        if (!tunnelInfoMap.containsKey((tunnelId))) {
            log.debug("Tunnel info does not exist whose tunnel id is {}.", tunnelId.toString());
            return false;
        }

        PceccTunnelInfo tunnelInfo = tunnelInfoMap.get(tunnelId).value();
        tunnelInfo.tunnelConsumerId(tunnelConsumerId);
        tunnelInfoMap.put(tunnelId, tunnelInfo);

        return true;
    }

    @Override
    public boolean removeGlobalNodeLabel(DeviceId id) {
        checkNotNull(id, DEVICE_ID_NULL);

        if (globalNodeLabelMap.remove(id) == null) {
            log.error("SR-TE node label deletion for device {} has failed.", id.toString());
            return false;
        }
        return true;
    }

    @Override
    public boolean removeAdjLabel(Link link) {
        checkNotNull(link, LINK_NULL);

        if (adjLabelMap.remove(link) == null) {
            log.error("Adjacency label deletion for link {} hash failed.", link.toString());
            return false;
        }
        return true;
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
    public boolean addLsrIdDevice(String lsrId, DeviceId deviceId) {
        checkNotNull(lsrId);
        checkNotNull(deviceId);

        lsrIdDeviceIdMap.put(lsrId, deviceId);
        return true;
    }

    @Override
    public boolean removeLsrIdDevice(String lsrId) {
        checkNotNull(lsrId);

        lsrIdDeviceIdMap.remove(lsrId);
        return true;
    }

    @Override
    public DeviceId getLsrIdDevice(String lsrId) {
        checkNotNull(lsrId);

        return lsrIdDeviceIdMap.get(lsrId);

    }

    @Override
    public boolean addPccLsr(DeviceId lsrId) {
        checkNotNull(lsrId);
        pendinglabelDbSyncPccMap.add(lsrId);
        return true;
    }

    @Override
    public boolean removePccLsr(DeviceId lsrId) {
        checkNotNull(lsrId);
        pendinglabelDbSyncPccMap.remove(lsrId);
        return true;
    }

    @Override
    public boolean hasPccLsr(DeviceId lsrId) {
        checkNotNull(lsrId);
        return pendinglabelDbSyncPccMap.contains(lsrId);

    }
}
