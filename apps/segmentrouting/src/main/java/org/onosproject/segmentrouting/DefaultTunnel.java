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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Tunnel class.
 */
public class DefaultTunnel implements Tunnel {

    private static final Logger log = LoggerFactory
            .getLogger(DefaultTunnel.class);

    private final String tunnelId;
    private final List<Integer> labelIds;
    private final SegmentRoutingManager srManager;
    private final DeviceConfiguration config;

    private int groupId;

    /**
     * Creates a Tunnel reference.
     *
     * @param srm SegmentRoutingManager object
     * @param tid  Tunnel ID
     * @param labelIds Label stack of the tunnel
     */
    public DefaultTunnel(SegmentRoutingManager srm, String tid,
                         List<Integer> labelIds) {
        this.srManager = checkNotNull(srm);
        this.tunnelId = checkNotNull(tid);
        this.labelIds = Collections.unmodifiableList(labelIds);
        this.config = srManager.deviceConfiguration;
        this.groupId = -1;
    }

    /**
     * Creates a Tunnel reference.
     *
     * @param tid  Tunnel ID
     * @param labelIds Label stack of the tunnel
     */
    public DefaultTunnel(String tid, List<Integer> labelIds) {
        this.srManager = null;
        this.tunnelId = checkNotNull(tid);
        this.labelIds = Collections.unmodifiableList(labelIds);
        this.config = null;
        this.groupId = -1;
    }

    /**
     * Creates a new DefaultTunnel reference using the tunnel reference.
     *
     * @param tunnel DefaultTunnel reference
     */
    public DefaultTunnel(DefaultTunnel tunnel) {
        this.srManager = tunnel.srManager;
        this.tunnelId = tunnel.tunnelId;
        this.labelIds = tunnel.labelIds;
        this.config = tunnel.config;
        this.groupId = tunnel.groupId;
    }

    @Override
    public String id() {
        return this.tunnelId;
    }

    @Override
    public List<Integer> labelIds() {
        return this.labelIds;
    }

    @Override
    public boolean create() {

        if (labelIds.isEmpty() || labelIds.size() < 3) {
            log.error("More than one router needs to specified to created a tunnel");
            return false;
        }

        groupId = createGroupsForTunnel();
        if (groupId < 0) {
            log.error("Failed to create groups for the tunnel");
            return false;
        }

        return true;
    }

    @Override
    public boolean remove() {

        DeviceId deviceId = config.getDeviceId(labelIds.get(0));
        srManager.removeNextObjective(deviceId, groupId);

        return true;
    }

    @Override
    public int groupId() {
        return this.groupId;
    }

    @Override
    public DeviceId source() {
        return config.getDeviceId(labelIds.get(0));
    }

    private int createGroupsForTunnel() {

        List<Integer> portNumbers;

        int groupId;

        DeviceId deviceId = config.getDeviceId(labelIds.get(0));
        if (deviceId == null) {
            log.warn("No device found for SID {}", labelIds.get(0));
            return -1;
        }
        Set<DeviceId> deviceIds = new HashSet<>();
        int sid = labelIds.get(1);
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

        NeighborSet ns = new NeighborSet(deviceIds, labelIds.get(2));
        groupId = srManager.getNextObjectiveId(deviceId, ns);

        return groupId;
    }

}
