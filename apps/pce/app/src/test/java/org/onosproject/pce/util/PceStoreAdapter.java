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

import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.net.resource.ResourceConsumer;
import org.onosproject.pce.pceservice.ExplicitPathInfo;
import org.onosproject.pce.pcestore.PcePathInfo;
import org.onosproject.pce.pcestore.api.PceStore;

/**
 * Provides test implementation of PceStore.
 */
public class PceStoreAdapter implements PceStore {

    // Mapping tunnel with device local info with tunnel consumer id
    private ConcurrentMap<TunnelId, ResourceConsumer> tunnelInfoMap = new ConcurrentHashMap<>();

    // Set of Path info
    private Set<PcePathInfo> failedPathInfoSet = new HashSet<>();

    // Locally maintain with tunnel name as key and corresponding list of explicit path object
    private Map<String, List<ExplicitPathInfo>> tunnelNameExplicitPathInfoMap = new HashMap<>();

    @Override
    public boolean existsTunnelInfo(TunnelId tunnelId) {
        return tunnelInfoMap.containsKey(tunnelId);
    }

    @Override
    public boolean existsFailedPathInfo(PcePathInfo pathInfo) {
        return failedPathInfoSet.contains(pathInfo);
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
    public Map<TunnelId, ResourceConsumer> getTunnelInfos() {
       return tunnelInfoMap.entrySet().stream()
                 .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()));
    }

    @Override
    public Iterable<PcePathInfo> getFailedPathInfos() {
       return ImmutableSet.copyOf(failedPathInfoSet);
    }

    @Override
    public ResourceConsumer getTunnelInfo(TunnelId tunnelId) {
        return tunnelInfoMap.get(tunnelId);
    }

    @Override
    public void addTunnelInfo(TunnelId tunnelId, ResourceConsumer tunnelConsumerId) {
        tunnelInfoMap.put(tunnelId, tunnelConsumerId);
    }

    @Override
    public void addFailedPathInfo(PcePathInfo pathInfo) {
        failedPathInfoSet.add(pathInfo);
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
    public boolean tunnelNameExplicitPathInfoMap(String tunnelName, List<ExplicitPathInfo> explicitPathInfo) {
        tunnelNameExplicitPathInfoMap.put(tunnelName, explicitPathInfo);
        return false;
    }

    @Override
    public List<ExplicitPathInfo> getTunnelNameExplicitPathInfoMap(String tunnelName) {
        return tunnelNameExplicitPathInfoMap.get(tunnelName);
    }
}
