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
package org.onosproject.pcelabelstore;

import static com.google.common.base.Preconditions.checkNotNull;

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
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.pcelabelstore.api.LspLocalLabelInfo;
import org.onosproject.pcelabelstore.api.PceLabelStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the pool of available labels to devices, links and tunnels.
 */
@Component(immediate = true)
@Service
public class DistributedPceLabelStore implements PceLabelStore {

    private static final String DEVICE_ID_NULL = "Device ID cannot be null";
    private static final String LABEL_RESOURCE_ID_NULL = "Label Resource Id cannot be null";
    private static final String LINK_NULL = "LINK cannot be null";
    private static final String PCECC_TUNNEL_INFO_NULL = "PCECC Tunnel Info cannot be null";
    private static final String TUNNEL_ID_NULL = "Tunnel Id cannot be null";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    // Mapping device with global node label
    private ConsistentMap<DeviceId, LabelResourceId> globalNodeLabelMap;

    // Mapping link with adjacency label
    private ConsistentMap<Link, LabelResourceId> adjLabelMap;

    // Mapping tunnel id with local labels.
    private ConsistentMap<TunnelId, List<LspLocalLabelInfo>> tunnelLabelInfoMap;

    // Locally maintain LSRID to device id mapping for better performance.
    private Map<String, DeviceId> lsrIdDeviceIdMap = new HashMap<>();

    // List of PCC LSR ids whose BGP device information was not available to perform
    // label db sync.
    private HashSet<DeviceId> pendinglabelDbSyncPccMap = new HashSet<>();

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

        tunnelLabelInfoMap = storageService.<TunnelId, List<LspLocalLabelInfo>>consistentMapBuilder()
                .withName("onos-pce-tunnellabelinfomap")
                .withSerializer(Serializer.using(
                        new KryoNamespace.Builder()
                                .register(KryoNamespaces.API)
                                .register(TunnelId.class,
                                          DefaultLspLocalLabelInfo.class,
                                          LabelResourceId.class,
                                          DeviceId.class)
                                .build()))
                .build();

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
        return tunnelLabelInfoMap.containsKey(tunnelId);
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
        return tunnelLabelInfoMap.size();
    }

    @Override
    public Map<DeviceId, LabelResourceId> getGlobalNodeLabels() {
       return globalNodeLabelMap.entrySet().stream()
                 .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().value()));
    }

    @Override
    public Map<Link, LabelResourceId> getAdjLabels() {
       return adjLabelMap.entrySet().stream()
                 .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().value()));
    }

    @Override
    public Map<TunnelId, List<LspLocalLabelInfo>> getTunnelInfos() {
       return tunnelLabelInfoMap.entrySet().stream()
                 .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().value()));
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
    public List<LspLocalLabelInfo> getTunnelInfo(TunnelId tunnelId) {
        checkNotNull(tunnelId, TUNNEL_ID_NULL);
        return tunnelLabelInfoMap.get(tunnelId) == null ? null : tunnelLabelInfoMap.get(tunnelId).value();
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
    public void addTunnelInfo(TunnelId tunnelId, List<LspLocalLabelInfo> lspLocalLabelInfoList) {
        checkNotNull(tunnelId, TUNNEL_ID_NULL);
        checkNotNull(lspLocalLabelInfoList, PCECC_TUNNEL_INFO_NULL);

        tunnelLabelInfoMap.put(tunnelId, lspLocalLabelInfoList);
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

        if (tunnelLabelInfoMap.remove(tunnelId) == null) {
            log.error("Tunnel info deletion for tunnel id {} has failed.", tunnelId.toString());
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
