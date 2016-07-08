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

package org.onosproject.ui.impl.topo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.incubator.net.PortStatisticsService;
import org.onosproject.incubator.net.tunnel.TunnelService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.region.Region;
import org.onosproject.net.statistic.StatisticService;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.ui.model.topo.UiClusterMember;
import org.onosproject.ui.model.topo.UiDevice;
import org.onosproject.ui.model.topo.UiHost;
import org.onosproject.ui.model.topo.UiLink;
import org.onosproject.ui.model.topo.UiRegion;
import org.onosproject.ui.model.topo.UiTopoLayout;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Facility for creating JSON messages to send to the topology view in the
 * Web client.
 */
class Topo2Jsonifier {

    private final ObjectMapper mapper = new ObjectMapper();

    private ServiceDirectory directory;
    private ClusterService clusterService;
    private DeviceService deviceService;
    private LinkService linkService;
    private HostService hostService;
    private MastershipService mastershipService;
    private IntentService intentService;
    private FlowRuleService flowService;
    private StatisticService flowStatsService;
    private PortStatisticsService portStatsService;
    private TopologyService topologyService;
    private TunnelService tunnelService;


    /**
     * Creates an instance with a reference to the services directory, so that
     * additional information about network elements may be looked up on
     * on the fly.
     *
     * @param directory service directory
     */
    Topo2Jsonifier(ServiceDirectory directory) {
        this.directory = checkNotNull(directory, "Directory cannot be null");

        clusterService = directory.get(ClusterService.class);
        deviceService = directory.get(DeviceService.class);
        linkService = directory.get(LinkService.class);
        hostService = directory.get(HostService.class);
        mastershipService = directory.get(MastershipService.class);
        intentService = directory.get(IntentService.class);
        flowService = directory.get(FlowRuleService.class);
        flowStatsService = directory.get(StatisticService.class);
        portStatsService = directory.get(PortStatisticsService.class);
        topologyService = directory.get(TopologyService.class);
        tunnelService = directory.get(TunnelService.class);

    }

    private ObjectNode objectNode() {
        return mapper.createObjectNode();
    }

    private ArrayNode arrayNode() {
        return mapper.createArrayNode();
    }

    private String nullIsEmpty(Object o) {
        return o == null ? "" : o.toString();
    }


    /**
     * Returns a JSON representation of the cluster members (ONOS instances).
     *
     * @param instances the instance model objects
     * @return a JSON representation of the data
     */
    ObjectNode instances(List<UiClusterMember> instances) {
        NodeId local = clusterService.getLocalNode().id();
        ObjectNode payload = objectNode();

        ArrayNode members = arrayNode();
        payload.set("members", members);
        for (UiClusterMember member : instances) {
            members.add(json(member, member.id().equals(local)));
        }

        return payload;
    }

    private ObjectNode json(UiClusterMember member, boolean isUiAttached) {
        return objectNode()
                .put("id", member.id().toString())
                .put("ip", member.ip().toString())
                .put("online", member.isOnline())
                .put("ready", member.isReady())
                .put("uiAttached", isUiAttached)
                .put("switches", member.deviceCount());
    }

    /**
     * Returns a JSON representation of the layout to use for displaying in
     * the topology view.
     *
     * @param layout the layout to transform
     * @return a JSON representation of the data
     */
    ObjectNode layout(UiTopoLayout layout) {
        return objectNode()
                .put("id", layout.id().toString())
                .put("parent", nullIsEmpty(layout.parent()))
                .put("region", nullIsEmpty(layout.regionId()))
                .put("regionName", regionName(layout.region()));
    }

    private String regionName(Region region) {
        return region == null ? "" : region.name();
    }

    /**
     * Returns a JSON representation of the region to display in the topology
     * view.
     *
     * @param region the region to transform to JSON
     * @return a JSON representation of the data
     */
    ObjectNode region(UiRegion region) {
        ObjectNode payload = objectNode();

        if (region == null) {
            payload.put("note", "no-region");
            return payload;
        }

        payload.put("id", region.id().toString());

        ArrayNode layerOrder = arrayNode();
        payload.set("layerOrder", layerOrder);
        region.layerOrder().forEach(layerOrder::add);

        ArrayNode devices = arrayNode();
        payload.set("devices", devices);
        for (UiDevice device : region.devices()) {
            devices.add(json(device));
        }

        ArrayNode hosts = arrayNode();
        payload.set("hosts", hosts);
        for (UiHost host : region.hosts()) {
            hosts.add(json(host));
        }

        ArrayNode links = arrayNode();
        payload.set("links", links);
        for (UiLink link : region.links()) {
            links.add(json(link));
        }

        return payload;
    }

    private ObjectNode json(UiDevice device) {
        ObjectNode node = objectNode()
                .put("id", device.id().toString())
                .put("type", device.type())
                .put("online", device.isOnline())
                .put("master", device.master().toString())
                .put("layer", device.layer());

        // TODO: complete device details
//        addLabels(node, device);
//        addProps(node, device);
//        addGeoLocation(node, device);
//        addMetaUi(node, device);

        return node;
    }

    private void addLabels(ObjectNode node, UiDevice device) {

    }

    private ObjectNode json(UiHost host) {
        return objectNode()
                .put("id", host.id().toString())
                .put("layer", host.layer());
        // TODO: complete host details
    }


    private ObjectNode json(UiLink link) {
        return objectNode()
                .put("id", link.id().toString());
        // TODO: complete link details
    }


}
