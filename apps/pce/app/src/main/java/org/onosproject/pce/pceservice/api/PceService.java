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
package org.onosproject.pce.pceservice.api;

import java.util.List;

import org.onosproject.net.DeviceId;
import org.onosproject.net.intent.Constraint;
import org.onosproject.pce.pceservice.ExplicitPathInfo;
import org.onosproject.pce.pceservice.LspType;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelId;

/**
 * Service to compute path based on constraints, release path,
 * update path with new constraints and query existing tunnels.
 */
public interface PceService {

    /**
     * Creates new path based on constraints and LSP type.
     *
     * @param src source device
     * @param dst destination device
     * @param tunnelName name of the tunnel
     * @param constraints list of constraints to be applied on path
     * @param lspType type of path to be setup
     * @return false on failure and true on successful path creation
     */
    boolean setupPath(DeviceId src, DeviceId dst, String tunnelName, List<Constraint> constraints, LspType lspType);

    /**
     * Creates new path based on constraints and LSP type.
     *
     * @param src source device
     * @param dst destination device
     * @param tunnelName name of the tunnel
     * @param constraints list of constraints to be applied on path
     * @param lspType type of path to be setup
     * @param explicitPathInfo list of explicit path info
     * @return false on failure and true on successful path creation
     */
    boolean setupPath(DeviceId src, DeviceId dst, String tunnelName, List<Constraint> constraints, LspType lspType,
                      List<ExplicitPathInfo> explicitPathInfo);

    /**
     * Updates an existing path.
     *
     * @param tunnelId tunnel identifier
     * @param constraints list of constraints to be applied on path
     * @return false on failure and true on successful path update
     */
    boolean updatePath(TunnelId tunnelId, List<Constraint> constraints);

    /**
     * Releases an existing path.
     *
     * @param tunnelId tunnel identifier
     * @return false on failure and true on successful path removal
     */
    boolean releasePath(TunnelId tunnelId);

    /**
     * Queries all paths.
     *
     * @return iterable of existing tunnels
     */
    Iterable<Tunnel> queryAllPath();

    /**
     * Queries particular path based on tunnel identifier.
     *
     * @param tunnelId tunnel identifier
     * @return tunnel if path exists, otherwise null
     */
    Tunnel queryPath(TunnelId tunnelId);

    /**
     * Returns list of explicit path info.
     *
     * @param tunnelName tunnel name
     * @return list of explicit path info
     */
    List<ExplicitPathInfo> explicitPathInfoList(String tunnelName);
}