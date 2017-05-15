/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.segmentrouting;

import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.link.LinkService;
import org.onosproject.segmentrouting.config.DeviceConfiguration;
import org.onosproject.segmentrouting.grouphandler.DefaultGroupHandler;
import org.onosproject.segmentrouting.grouphandler.NeighborSet;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Tunnel Handler.
 */
public class TunnelHandler {
    protected final Logger log = getLogger(getClass());

    private final DeviceConfiguration config;
    private final EventuallyConsistentMap<String, Tunnel> tunnelStore;
    private Map<DeviceId, DefaultGroupHandler> groupHandlerMap;
    private LinkService linkService;

    /**
     * Result of tunnel creation or removal.
     */
    public enum Result {
        /**
         * Success.
         */
        SUCCESS,

        /**
         * More than one router needs to specified to created a tunnel.
         */
        WRONG_PATH,

        /**
         * The same tunnel exists already.
         */
        TUNNEL_EXISTS,

        /**
         * The same tunnel ID exists already.
         */
        ID_EXISTS,

        /**
         * Tunnel not found.
         */
        TUNNEL_NOT_FOUND,

        /**
         * Cannot remove the tunnel used by a policy.
         */
        TUNNEL_IN_USE,

        /**
         * Failed to create/remove groups for the tunnel.
         */
        INTERNAL_ERROR
    }

    /**
     * Constructs tunnel handler.
     *
     * @param linkService link service
     * @param deviceConfiguration device configuration
     * @param groupHandlerMap group handler map
     * @param tunnelStore tunnel store
     */
    public TunnelHandler(LinkService linkService,
                         DeviceConfiguration deviceConfiguration,
                         Map<DeviceId, DefaultGroupHandler> groupHandlerMap,
                         EventuallyConsistentMap<String, Tunnel> tunnelStore) {
        this.linkService = linkService;
        this.config = deviceConfiguration;
        this.groupHandlerMap = groupHandlerMap;
        this.tunnelStore = tunnelStore;
    }

    /**
     * Creates a tunnel.
     *
     * @param tunnel tunnel reference to create a tunnel
     * @return WRONG_PATH if the tunnel path is wrong, ID_EXISTS if the tunnel ID
     * exists already, TUNNEL_EXISTS if the same tunnel exists, INTERNAL_ERROR
     * if the tunnel creation failed internally, SUCCESS if the tunnel is created
     * successfully
     */
    public Result createTunnel(Tunnel tunnel) {

        if (tunnel.labelIds().isEmpty() || tunnel.labelIds().size() < 3) {
            log.error("More than one router needs to specified to created a tunnel");
            return Result.WRONG_PATH;
        }

        if (tunnelStore.containsKey(tunnel.id())) {
            log.warn("The same tunnel ID exists already");
            return Result.ID_EXISTS;
        }

        if (tunnelStore.containsValue(tunnel)) {
            log.warn("The same tunnel exists already");
            return Result.TUNNEL_EXISTS;
        }

        int groupId = createGroupsForTunnel(tunnel);
        if (groupId < 0) {
            log.error("Failed to create groups for the tunnel");
            return Result.INTERNAL_ERROR;
        }

        tunnel.setGroupId(groupId);
        tunnelStore.put(tunnel.id(), tunnel);

        return Result.SUCCESS;
    }

    /**
     * Removes the tunnel with the tunnel ID given.
     *
     * @param tunnelInfo tunnel information to delete tunnels
     * @return TUNNEL_NOT_FOUND if the tunnel to remove does not exists,
     * INTERNAL_ERROR if the tunnel creation failed internally, SUCCESS
     * if the tunnel is created successfully.
     */
    public Result removeTunnel(Tunnel tunnelInfo) {

        Tunnel tunnel = tunnelStore.get(tunnelInfo.id());
        if (tunnel != null) {
            DeviceId deviceId = config.getDeviceId(tunnel.labelIds().get(0));
            if (tunnel.isAllowedToRemoveGroup()) {
                if (groupHandlerMap.get(deviceId).removeGroup(tunnel.groupId())) {
                    tunnelStore.remove(tunnel.id());
                } else {
                    log.error("Failed to remove the tunnel {}", tunnelInfo.id());
                    return Result.INTERNAL_ERROR;
                }
            } else {
                log.debug("The group is not removed because it is being used.");
                tunnelStore.remove(tunnel.id());
            }
        } else {
            log.error("No tunnel found for tunnel ID {}", tunnelInfo.id());
            return Result.TUNNEL_NOT_FOUND;
        }

        return Result.SUCCESS;
    }

    /**
     * Returns the tunnel with the tunnel ID given.
     *
     * @param tid Tunnel ID
     * @return Tunnel reference
     */
    public Tunnel getTunnel(String tid) {
        return tunnelStore.get(tid);
    }

    /**
     * Returns all tunnels.
     *
     * @return list of Tunnels
     */
    public List<Tunnel> getTunnels() {
        List<Tunnel> tunnels = new ArrayList<>();
        tunnelStore.values().forEach(tunnel -> tunnels.add(
                new DefaultTunnel((DefaultTunnel) tunnel)));

        return tunnels;
    }

    private int createGroupsForTunnel(Tunnel tunnel) {

        Set<Integer> portNumbers;
        final int groupError = -1;

        DeviceId deviceId = config.getDeviceId(tunnel.labelIds().get(0));
        if (deviceId == null) {
            log.warn("No device found for SID {}", tunnel.labelIds().get(0));
            return groupError;
        } else if (groupHandlerMap.get(deviceId) == null) {
            log.warn("group handler not found for {}", deviceId);
            return groupError;
        }
        Set<DeviceId> deviceIds = new HashSet<>();
        int sid = tunnel.labelIds().get(1);
        if (config.isAdjacencySid(deviceId, sid)) {
            portNumbers = config.getPortsForAdjacencySid(deviceId, sid);
            for (Link link: linkService.getDeviceEgressLinks(deviceId)) {
                for (Integer port: portNumbers) {
                    if (link.src().port().toLong() == port) {
                        deviceIds.add(link.dst().deviceId());
                    }
                }
            }
        } else {
            deviceIds.add(config.getDeviceId(sid));
        }
        // For these NeighborSet isMpls is meaningless.
        NeighborSet ns = new NeighborSet(deviceIds, false,
                                         tunnel.labelIds().get(2),
                                         DeviceId.NONE);

        // If the tunnel reuses any existing groups, then tunnel handler
        // should not remove the group.
        if (groupHandlerMap.get(deviceId).hasNextObjectiveId(ns)) {
            tunnel.allowToRemoveGroup(false);
        } else {
            tunnel.allowToRemoveGroup(true);
        }

        return groupHandlerMap.get(deviceId).getNextObjectiveId(ns, null, true);
    }

}
