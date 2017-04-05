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
package org.onosproject.pce.pcestore.api;

import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.pce.pceservice.ExplicitPathInfo;
import org.onosproject.pce.pcestore.PcePathInfo;

import java.util.List;

/**
 * Abstraction of an entity providing pool of available labels to devices, links and tunnels.
 */
public interface PceStore {

    /**
     * Checks whether path info is present in failed path info list.
     *
     * @param failedPathInfo failed path information
     * @return success or failure
     */
    boolean existsFailedPathInfo(PcePathInfo failedPathInfo);

    /**
     * Retrieves the failed path info count.
     *
     * @return failed path info count
     */
    int getFailedPathInfoCount();

    /**
     * Retrieves path info collection from failed path info store.
     *
     * @return collection of failed path info
     */
    Iterable<PcePathInfo> getFailedPathInfos();

    /**
     * Stores path information into failed path info store.
     *
     * @param failedPathInfo failed path information
     */
    void addFailedPathInfo(PcePathInfo failedPathInfo);


    /**
     * Removes path info from failed path info store.
     *
     * @param failedPathInfo failed path information
     * @return success or failure
     */
    boolean removeFailedPathInfo(PcePathInfo failedPathInfo);

    /**
     * Adds explicit path info to the map with corresponding tunnel name.
     *
     * @param tunnelName tunnel name as key
     * @param explicitPathInfo list of explicit path objects
     * @return whether it is added to map
     */
    boolean tunnelNameExplicitPathInfoMap(String tunnelName, List<ExplicitPathInfo> explicitPathInfo);

    /**
     * Gets explicit path info based on tunnel name.
     *
     * @param tunnelName tunnel name as key
     * @return list of explicit path info
     */
    List<ExplicitPathInfo> getTunnelNameExplicitPathInfoMap(String tunnelName);

    //DisjointPath getDisjointPaths(String tunnelName);

    //boolean addDisjointPathInfo(String tunnelName, DisjointPath path);

    /**
     * Stores load balancing tunnels by load balance path name.
     *
     * @param loadBalancingPathName load balancing path name
     * @param tunnelIds list load balancing tunnels
     * @return success or failure
     */
    boolean addLoadBalancingTunnelIdsInfo(String loadBalancingPathName, TunnelId... tunnelIds);

    /**
     * Query load balancing tunnels by load balance path name.
     *
     * @param loadBalancingPathName load balancing path name
     * @return list of load balancing tunnels
     */
    List<TunnelId> getLoadBalancingTunnelIds(String loadBalancingPathName);

    /**
     * Removes load balancing tunnel info.
     *
     * @param loadBalancingPathName load balancing path name
     * @return success or failure
     */
    boolean removeLoadBalancingTunnelIdsInfo(String loadBalancingPathName);

    //boolean removeDisjointPathInfo(String tunnelName);
}
