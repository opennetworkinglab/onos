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
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.pce.pceservice.ExplicitPathInfo;
import org.onosproject.pce.pcestore.PcePathInfo;
import org.onosproject.pce.pcestore.api.PceStore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides test implementation of PceStore.
 */
public class PceStoreAdapter implements PceStore {

    // Mapping tunnel with device local info with tunnel consumer id

    // Set of Path info
    private Set<PcePathInfo> failedPathInfoSet = new HashSet<>();

    // Locally maintain with tunnel name as key and corresponding list of explicit path object
    private Map<String, List<ExplicitPathInfo>> tunnelNameExplicitPathInfoMap = new HashMap<>();

    //Mapping tunnel name with Disjoint paths
    private Map<String, List<TunnelId>> loadBalancingPathNameTunnelIdInfo = new HashMap<>();

    @Override
    public boolean existsFailedPathInfo(PcePathInfo pathInfo) {
        return failedPathInfoSet.contains(pathInfo);
    }

    @Override
    public int getFailedPathInfoCount() {
        return failedPathInfoSet.size();
    }

    @Override
    public Iterable<PcePathInfo> getFailedPathInfos() {
       return ImmutableSet.copyOf(failedPathInfoSet);
    }

    @Override
    public void addFailedPathInfo(PcePathInfo pathInfo) {
        failedPathInfoSet.add(pathInfo);
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

/*    @Override
    public DisjointPath getDisjointPaths(String tunnelName) {
        if (tunnelNameDisjointPathInfo.get(tunnelName) != null) {
            return tunnelNameDisjointPathInfo.get(tunnelName);
        }
        return null;
    }

    @Override
    public boolean addDisjointPathInfo(String tunnelName, DisjointPath path) {
        return tunnelNameDisjointPathInfo.put(tunnelName, path) != null ? true : false;
    }*/

    @Override
    public boolean addLoadBalancingTunnelIdsInfo(String loadBalancingPathName, TunnelId... tunnelIds) {
        return loadBalancingPathNameTunnelIdInfo.put(loadBalancingPathName,
                Arrays.asList(tunnelIds)) != null ? true : false;
    }

    @Override
    public List<TunnelId> getLoadBalancingTunnelIds(String loadBalancingPathName) {
        if (loadBalancingPathNameTunnelIdInfo.get(loadBalancingPathName) != null) {
            return loadBalancingPathNameTunnelIdInfo.get(loadBalancingPathName);
        }
        return null;
    }

    @Override
    public boolean removeLoadBalancingTunnelIdsInfo(String loadBalancingPathName) {
        if (loadBalancingPathNameTunnelIdInfo.remove(loadBalancingPathName) == null) {
            return false;
        }
        return true;
    }

/*    @Override
    public boolean removeDisjointPathInfo(String tunnelName) {
        if (tunnelNameDisjointPathInfo.remove(tunnelName) == null) {
            return false;
        }
        return true;
    }*/
}
