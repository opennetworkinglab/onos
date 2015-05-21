/*
 * Copyright 2015 Open Networking Laboratory
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
import org.onosproject.segmentrouting.grouphandler.NeighborSet;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Tunnel Handler.
 */
public class TunnelHandler {
    protected final Logger log = getLogger(getClass());

    private final SegmentRoutingManager srManager;
    private final DeviceConfiguration config;
    private final EventuallyConsistentMap<String, Tunnel> tunnelStore;

    public TunnelHandler(SegmentRoutingManager srm,
                         EventuallyConsistentMap<String, Tunnel> tunnelStore) {
        this.srManager = checkNotNull(srm);
        this.config = srm.deviceConfiguration;
        this.tunnelStore = tunnelStore;
    }

    /**
     * Creates a tunnel.
     *
     * @param tunnel tunnel reference to create a tunnel
     * @return true if creation succeeded
     */
    public boolean createTunnel(Tunnel tunnel) {

        if (tunnel.labelIds().isEmpty() || tunnel.labelIds().size() < 3) {
            log.error("More than one router needs to specified to created a tunnel");
            return false;
        }

        if (tunnelStore.containsKey(tunnel.id())) {
            log.warn("The same tunnel ID exists already");
            return false;
        }

        if (tunnelStore.containsValue(tunnel)) {
            log.warn("The same tunnel exists already");
            return false;
        }

        int groupId = createGroupsForTunnel(tunnel);
        if (groupId < 0) {
            log.error("Failed to create groups for the tunnel");
            return false;
        }

        tunnel.setGroupId(groupId);
        tunnelStore.put(tunnel.id(), tunnel);

        return true;
    }

    /**
     * Removes the tunnel with the tunnel ID given.
     *
     * @param tunnelInfo tunnel information to delete tunnels
     */
    public void removeTunnel(Tunnel tunnelInfo) {

        Tunnel tunnel = tunnelStore.get(tunnelInfo.id());
        if (tunnel != null) {
            DeviceId deviceId = config.getDeviceId(tunnel.labelIds().get(0));
            if (tunnel.isAllowedToRemoveGroup()) {
                if (srManager.removeNextObjective(deviceId, tunnel.groupId())) {
                    tunnelStore.remove(tunnel.id());
                } else {
                    log.error("Failed to remove the tunnel {}", tunnelInfo.id());
                }
            } else {
                log.debug("The group is not removed because it is being used.");
                tunnelStore.remove(tunnel.id());
            }
        } else {
            log.warn("No tunnel found for tunnel ID {}", tunnelInfo.id());
        }
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

        List<Integer> portNumbers;

        int groupId;

        DeviceId deviceId = config.getDeviceId(tunnel.labelIds().get(0));
        if (deviceId == null) {
            log.warn("No device found for SID {}", tunnel.labelIds().get(0));
            return -1;
        }
        Set<DeviceId> deviceIds = new HashSet<>();
        int sid = tunnel.labelIds().get(1);
        if (config.isAdjacencySid(deviceId, sid)) {
            portNumbers = config.getPortsForAdjacencySid(deviceId, sid);
            for (Link link: srManager.linkService.getDeviceEgressLinks(deviceId)) {
                for (Integer port: portNumbers) {
                    if (link.src().port().toLong() == port) {
                        deviceIds.add(link.dst().deviceId());
                    }
                }
            }
        } else {
            deviceIds.add(config.getDeviceId(sid));
        }

        NeighborSet ns = new NeighborSet(deviceIds, tunnel.labelIds().get(2));

        // If the tunnel reuses any existing groups, then tunnel handler
        // should not remove the group.
        if (srManager.hasNextObjectiveId(deviceId, ns)) {
            tunnel.allowToRemoveGroup(false);
        } else {
            tunnel.allowToRemoveGroup(true);
        }
        groupId = srManager.getNextObjectiveId(deviceId, ns);

        return groupId;
    }

}
