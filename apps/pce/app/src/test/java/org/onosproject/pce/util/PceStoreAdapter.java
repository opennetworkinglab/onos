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
package org.onosproject.pce.util;

import com.google.common.collect.ImmutableSet;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.resource.ResourceConsumer;
import org.onosproject.pce.pcestore.PceccTunnelInfo;
import org.onosproject.pce.pcestore.PcePathInfo;
import org.onosproject.pce.pcestore.api.LspLocalLabelInfo;
import org.onosproject.pce.pcestore.api.PceStore;

/**
 * Provides test implementation of PceStore.
 */
public class PceStoreAdapter implements PceStore {

    // Mapping device with global node label
    private ConcurrentMap<DeviceId, LabelResourceId> globalNodeLabelMap = new ConcurrentHashMap<>();

    // Mapping link with adjacency label
    private ConcurrentMap<Link, LabelResourceId> adjLabelMap = new ConcurrentHashMap<>();

    // Mapping tunnel with device local info with tunnel consumer id
    private ConcurrentMap<TunnelId, PceccTunnelInfo> tunnelInfoMap = new ConcurrentHashMap<>();

    // Set of Path info
    private Set<PcePathInfo> failedPathInfoSet = new HashSet<>();

    // Locally maintain LSRID to device id mapping for better performance.
    private Map<String, DeviceId> lsrIdDeviceIdMap = new HashMap<>();

    @Override
    public boolean existsGlobalNodeLabel(DeviceId id) {
        return globalNodeLabelMap.containsKey(id);
    }

    @Override
    public boolean existsAdjLabel(Link link) {
        return adjLabelMap.containsKey(link);
    }

    @Override
    public boolean existsTunnelInfo(TunnelId tunnelId) {
        return tunnelInfoMap.containsKey(tunnelId);
    }

    @Override
    public boolean existsFailedPathInfo(PcePathInfo pathInfo) {
        return failedPathInfoSet.contains(pathInfo);
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
        return failedPathInfoSet.size();
    }

    @Override
    public Map<DeviceId, LabelResourceId> getGlobalNodeLabels() {
       return globalNodeLabelMap.entrySet().stream()
                 .collect(Collectors.toMap(Map.Entry::getKey, e -> (LabelResourceId) e.getValue()));
    }

    @Override
    public Map<Link, LabelResourceId> getAdjLabels() {
       return adjLabelMap.entrySet().stream()
                 .collect(Collectors.toMap(Map.Entry::getKey, e -> (LabelResourceId) e.getValue()));
    }

    @Override
    public Map<TunnelId, PceccTunnelInfo> getTunnelInfos() {
       return tunnelInfoMap.entrySet().stream()
                 .collect(Collectors.toMap(Map.Entry::getKey, e -> (PceccTunnelInfo) e.getValue()));
    }

    @Override
    public Iterable<PcePathInfo> getFailedPathInfos() {
       return ImmutableSet.copyOf(failedPathInfoSet);
    }

    @Override
    public LabelResourceId getGlobalNodeLabel(DeviceId id) {
        return globalNodeLabelMap.get(id);
    }

    @Override
    public LabelResourceId getAdjLabel(Link link) {
        return adjLabelMap.get(link);
    }

    @Override
    public PceccTunnelInfo getTunnelInfo(TunnelId tunnelId) {
        return tunnelInfoMap.get(tunnelId);
    }

    @Override
    public void addGlobalNodeLabel(DeviceId deviceId, LabelResourceId labelId) {
        globalNodeLabelMap.put(deviceId, labelId);
    }

    @Override
    public void addAdjLabel(Link link, LabelResourceId labelId) {
        adjLabelMap.put(link, labelId);
    }

    @Override
    public void addTunnelInfo(TunnelId tunnelId, PceccTunnelInfo pceccTunnelInfo) {
        tunnelInfoMap.put(tunnelId, pceccTunnelInfo);
    }

    @Override
    public void addFailedPathInfo(PcePathInfo pathInfo) {
        failedPathInfoSet.add(pathInfo);
    }

    @Override
    public boolean updateTunnelInfo(TunnelId tunnelId, List<LspLocalLabelInfo> lspLocalLabelInfoList) {
        if (!tunnelInfoMap.containsKey((tunnelId))) {
            return false;
        }

        PceccTunnelInfo labelStoreInfo = tunnelInfoMap.get(tunnelId);
        labelStoreInfo.lspLocalLabelInfoList(lspLocalLabelInfoList);
        tunnelInfoMap.put(tunnelId, labelStoreInfo);
        return true;
    }

    @Override
    public boolean updateTunnelInfo(TunnelId tunnelId, ResourceConsumer tunnelConsumerId) {
        if (!tunnelInfoMap.containsKey((tunnelId))) {
            return false;
        }

        PceccTunnelInfo tunnelInfo = tunnelInfoMap.get(tunnelId);
        tunnelInfo.tunnelConsumerId(tunnelConsumerId);
        tunnelInfoMap.put(tunnelId, tunnelInfo);
        return true;
    }

    @Override
    public boolean removeGlobalNodeLabel(DeviceId id) {
        globalNodeLabelMap.remove(id);
        if (globalNodeLabelMap.containsKey(id)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean removeAdjLabel(Link link) {
        adjLabelMap.remove(link);
        if (adjLabelMap.containsKey(link)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean removeTunnelInfo(TunnelId tunnelId) {
        tunnelInfoMap.remove(tunnelId);
        if (tunnelInfoMap.containsKey(tunnelId)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean removeFailedPathInfo(PcePathInfo pathInfo) {
        if (failedPathInfoSet.remove(pathInfo)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean addLsrIdDevice(String lsrId, DeviceId deviceId) {
        lsrIdDeviceIdMap.put(lsrId, deviceId);
        return true;
    }

    @Override
    public boolean removeLsrIdDevice(String lsrId) {
        lsrIdDeviceIdMap.remove(lsrId);
        return true;
    }

    @Override
    public DeviceId getLsrIdDevice(String lsrId) {
        return lsrIdDeviceIdMap.get(lsrId);
    }

    @Override
    public boolean addPccLsr(DeviceId lsrId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removePccLsr(DeviceId lsrId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasPccLsr(DeviceId lsrId) {
        // TODO Auto-generated method stub
        return false;
    }
}
