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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.incubator.net.PortStatisticsService;
import org.onosproject.incubator.net.tunnel.TunnelService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Annotated;
import org.onosproject.net.Annotations;
import org.onosproject.net.Device;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.statistic.StatisticService;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.ui.model.topo.UiClusterMember;
import org.onosproject.ui.model.topo.UiDevice;
import org.onosproject.ui.model.topo.UiHost;
import org.onosproject.ui.model.topo.UiLink;
import org.onosproject.ui.model.topo.UiNode;
import org.onosproject.ui.model.topo.UiRegion;
import org.onosproject.ui.model.topo.UiSynthLink;
import org.onosproject.ui.model.topo.UiTopoLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.AnnotationKeys.LATITUDE;
import static org.onosproject.net.AnnotationKeys.LONGITUDE;
import static org.onosproject.ui.model.topo.UiNode.LAYER_DEFAULT;

/**
 * Facility for creating JSON messages to send to the topology view in the
 * Web client.
 */
class Topo2Jsonifier {

    private static final String E_DEF_NOT_LAST =
            "UiNode.LAYER_DEFAULT not last in layer list";
    private static final String E_UNKNOWN_UI_NODE =
            "Unknown subclass of UiNode: ";

    private static final String REGION = "region";
    private static final String DEVICE = "device";
    private static final String HOST = "host";

    private final Logger log = LoggerFactory.getLogger(getClass());

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


    // NOTE: we'll stick this here for now, but maybe there is a better home?
    //       (this is not distributed across the cluster)
    private static Map<String, ObjectNode> metaUi = new ConcurrentHashMap<>();


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

    // for unit testing
    Topo2Jsonifier() {
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
     * the topology view. The identifiers and names of regions from the
     * current to the root is included, so that the bread-crumb widget can
     * be rendered.
     *
     * @param layout the layout to transform
     * @param crumbs list of layouts in bread-crumb order
     * @return a JSON representation of the data
     */
    ObjectNode layout(UiTopoLayout layout, List<UiTopoLayout> crumbs) {
        ObjectNode result = objectNode()
                .put("id", layout.id().toString())
                .put("parent", nullIsEmpty(layout.parent()))
                .put("region", nullIsEmpty(layout.regionId()))
                .put("regionName", UiRegion.safeName(layout.region()));
        addCrumbs(result, crumbs);
        return result;
    }

    private void addCrumbs(ObjectNode result, List<UiTopoLayout> crumbs) {
        ArrayNode trail = arrayNode();
        crumbs.forEach(c -> {
            ObjectNode n = objectNode()
                    .put("id", c.regionId().toString())
                    .put("name", UiRegion.safeName(c.region()));
            trail.add(n);
        });
        result.set("crumbs", trail);
    }

    /**
     * Returns a JSON representation of the region to display in the topology
     * view.
     *
     * @param region     the region to transform to JSON
     * @param subRegions the subregions within this region
     * @param links      the links within this region
     * @return a JSON representation of the data
     */
    ObjectNode region(UiRegion region, Set<UiRegion> subRegions,
                      List<UiSynthLink> links) {
        ObjectNode payload = objectNode();
        if (region == null) {
            payload.put("note", "no-region");
            return payload;
        }
        payload.put("id", region.idAsString());
        if (subRegions != null) {
            payload.set("subregions", jsonSubRegions(subRegions));
        }

        if (links != null) {
            payload.set("links", jsonLinks(links));
        }

        List<String> layerTags = region.layerOrder();
        List<Set<UiNode>> splitDevices = splitByLayer(layerTags, region.devices());
        List<Set<UiNode>> splitHosts = splitByLayer(layerTags, region.hosts());

        payload.set("devices", jsonGrouped(splitDevices));
        payload.set("hosts", jsonGrouped(splitHosts));
        payload.set("layerOrder", jsonStrings(layerTags));

        return payload;
    }

    private ArrayNode jsonSubRegions(Set<UiRegion> subregions) {
        ArrayNode kids = arrayNode();
        subregions.forEach(s -> kids.add(jsonClosedRegion(s)));
        return kids;
    }

    private JsonNode jsonLinks(List<UiSynthLink> links) {
        ArrayNode synthLinks = arrayNode();
        links.forEach(l -> synthLinks.add(json(l)));
        return synthLinks;
    }

    private ArrayNode jsonStrings(List<String> strings) {
        ArrayNode array = arrayNode();
        strings.forEach(array::add);
        return array;
    }

    private ArrayNode jsonGrouped(List<Set<UiNode>> groupedNodes) {
        ArrayNode result = arrayNode();
        groupedNodes.forEach(g -> {
            ArrayNode subset = arrayNode();
            g.forEach(n -> subset.add(json(n)));
            result.add(subset);
        });
        return result;
    }


    private ObjectNode json(UiNode node) {
        if (node instanceof UiRegion) {
            return jsonClosedRegion((UiRegion) node);
        }
        if (node instanceof UiDevice) {
            return json((UiDevice) node);
        }
        if (node instanceof UiHost) {
            return json((UiHost) node);
        }
        throw new IllegalStateException(E_UNKNOWN_UI_NODE + node.getClass());
    }

    private ObjectNode json(UiDevice device) {
        ObjectNode node = objectNode()
                .put("id", device.idAsString())
                .put("nodeType", DEVICE)
                .put("type", device.type())
                .put("online", deviceService.isAvailable(device.id()))
                .put("master", nullIsEmpty(device.master()))
                .put("layer", device.layer());

        Device d = device.backingDevice();

        addProps(node, d);
        addGeoLocation(node, d);
        addMetaUi(node, device.idAsString());

        return node;
    }

    private void addProps(ObjectNode node, Device dev) {
        Annotations annot = dev.annotations();
        ObjectNode props = objectNode();
        if (annot != null) {
            annot.keys().forEach(k -> props.put(k, annot.value(k)));
        }
        node.set("props", props);
    }

    private void addMetaUi(ObjectNode node, String metaInstanceId) {
        ObjectNode meta = metaUi.get(metaInstanceId);
        if (meta != null) {
            node.set("metaUi", meta);
        }
    }

    private void addGeoLocation(ObjectNode node, Annotated a) {
        List<String> lngLat = getAnnotValues(a, LONGITUDE, LATITUDE);
        if (lngLat != null) {
            try {
                double lng = Double.parseDouble(lngLat.get(0));
                double lat = Double.parseDouble(lngLat.get(1));
                ObjectNode loc = objectNode()
                        .put("type", "lnglat")
                        .put("lng", lng)
                        .put("lat", lat);
                node.set("location", loc);

            } catch (NumberFormatException e) {
                log.warn("Invalid geo data: longitude={}, latitude={}",
                        lngLat.get(0), lngLat.get(1));
            }
        } else {
            log.debug("No geo lng/lat for {}", a);
        }
    }

    // return list of string values from annotated instance, for given keys
    // return null if any keys are not present
    List<String> getAnnotValues(Annotated a, String... annotKeys) {
        List<String> result = new ArrayList<>(annotKeys.length);
        for (String k : annotKeys) {
            String v = a.annotations().value(k);
            if (v == null) {
                return null;
            }
            result.add(v);
        }
        return result;
    }

    // derive JSON object from annotations
    private ObjectNode props(Annotations annotations) {
        ObjectNode p = objectNode();
        if (annotations != null) {
            annotations.keys().forEach(k -> p.put(k, annotations.value(k)));
        }
        return p;
    }

    private ObjectNode json(UiHost host) {
        return objectNode()
                .put("id", host.idAsString())
                .put("nodeType", HOST)
                .put("layer", host.layer());
        // TODO: complete host details
    }

    private ObjectNode json(UiSynthLink sLink) {
        UiLink uLink = sLink.link();
        ObjectNode data = objectNode()
                .put("id", uLink.idAsString())
                .put("epA", uLink.endPointA())
                .put("epB", uLink.endPointB())
                .put("type", uLink.type());
        String pA = uLink.endPortA();
        String pB = uLink.endPortB();
        if (pA != null) {
            data.put("portA", pA);
        }
        if (pB != null) {
            data.put("portB", pB);
        }
        return data;
    }


    private ObjectNode jsonClosedRegion(UiRegion region) {
        return objectNode()
                .put("id", region.idAsString())
                .put("name", region.name())
                .put("nodeType", REGION)
                .put("nDevs", region.deviceCount());
        // TODO: complete closed-region details
    }

    /**
     * Returns a JSON array representation of a set of regions/devices. Note
     * that the information is sufficient for showing regions as nodes.
     *
     * @param nodes the nodes
     * @return a JSON representation of the nodes
     */
    public ArrayNode closedNodes(Set<UiNode> nodes) {
        ArrayNode array = arrayNode();
        for (UiNode node : nodes) {
            if (node instanceof UiRegion) {
                array.add(jsonClosedRegion((UiRegion) node));
            } else if (node instanceof UiDevice) {
                array.add(json((UiDevice) node));
            } else {
                log.warn("Unexpected node instance: {}", node.getClass());
            }
        }
        return array;
    }

    /**
     * Returns a JSON array representation of a list of regions. Note that the
     * information about each region is limited to what needs to be used to
     * show the regions as nodes on the view.
     *
     * @param regions the regions
     * @return a JSON representation of the minimal region information
     */
    public ArrayNode closedRegions(Set<UiRegion> regions) {
        ArrayNode array = arrayNode();
        for (UiRegion r : regions) {
            array.add(jsonClosedRegion(r));
        }
        return array;
    }

    /**
     * Returns a JSON array representation of a list of devices.
     *
     * @param devices the devices
     * @return a JSON representation of the devices
     */
    public ArrayNode devices(Set<UiDevice> devices) {
        ArrayNode array = arrayNode();
        for (UiDevice device : devices) {
            array.add(json(device));
        }
        return array;
    }

    /**
     * Returns a JSON array representation of a list of hosts.
     *
     * @param hosts the hosts
     * @return a JSON representation of the hosts
     */
    public ArrayNode hosts(Set<UiHost> hosts) {
        ArrayNode array = arrayNode();
        for (UiHost host : hosts) {
            array.add(json(host));
        }
        return array;
    }

    // package-private for unit testing
    List<Set<UiNode>> splitByLayer(List<String> layerTags,
                                   Set<? extends UiNode> nodes) {
        final int nLayers = layerTags.size();
        if (!layerTags.get(nLayers - 1).equals(LAYER_DEFAULT)) {
            throw new IllegalArgumentException(E_DEF_NOT_LAST);
        }

        List<Set<UiNode>> splitList = new ArrayList<>(layerTags.size());
        Map<String, Set<UiNode>> byLayer = new HashMap<>(layerTags.size());

        for (String tag : layerTags) {
            Set<UiNode> set = new HashSet<>();
            byLayer.put(tag, set);
            splitList.add(set);
        }

        for (UiNode n : nodes) {
            String which = n.layer();
            if (!layerTags.contains(which)) {
                which = LAYER_DEFAULT;
            }
            byLayer.get(which).add(n);
        }

        return splitList;
    }
}
