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
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.incubator.net.PortStatisticsService;
import org.onosproject.incubator.net.tunnel.TunnelService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Annotated;
import org.onosproject.net.Annotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.region.Region;
import org.onosproject.net.statistic.StatisticService;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.ui.JsonUtils;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiPreferencesService;
import org.onosproject.ui.UiTopoMap;
import org.onosproject.ui.UiTopoMapFactory;
import org.onosproject.ui.impl.topo.model.UiModelEvent;
import org.onosproject.ui.model.topo.UiClusterMember;
import org.onosproject.ui.model.topo.UiDevice;
import org.onosproject.ui.model.topo.UiElement;
import org.onosproject.ui.model.topo.UiHost;
import org.onosproject.ui.model.topo.UiLink;
import org.onosproject.ui.model.topo.UiNode;
import org.onosproject.ui.model.topo.UiRegion;
import org.onosproject.ui.model.topo.UiSynthLink;
import org.onosproject.ui.model.topo.UiTopoLayout;
import org.onosproject.ui.model.topo.UiTopoLayoutId;
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
import static org.onosproject.net.AnnotationKeys.GRID_X;
import static org.onosproject.net.AnnotationKeys.GRID_Y;
import static org.onosproject.net.AnnotationKeys.LATITUDE;
import static org.onosproject.net.AnnotationKeys.LONGITUDE;
import static org.onosproject.ui.model.topo.UiNode.LAYER_DEFAULT;

/**
 * Facility for creating JSON messages to send to the topology view in the
 * Web client.
 */
public class Topo2Jsonifier {

    private static final String E_DEF_NOT_LAST =
            "UiNode.LAYER_DEFAULT not last in layer list";
    private static final String E_UNKNOWN_UI_NODE =
            "Unknown subclass of UiNode: ";

    private static final String REGION = "region";
    private static final String DEVICE = "device";
    private static final String HOST = "host";
    private static final String TYPE = "type";
    private static final String SUBJECT = "subject";
    private static final String DATA = "data";
    private static final String MEMO = "memo";

    private static final String GEO = "geo";
    private static final String GRID = "grid";
    private static final String PKEY_TOPO_ZOOM = "topo2_zoom";
    private static final String ZOOM_SCALE = "zoomScale";
    private static final String ZOOM_PAN_X = "zoomPanX";
    private static final String ZOOM_PAN_Y = "zoomPanY";
    private static final String DEFAULT_SCALE = "1.0";
    private static final String DEFAULT_PAN = "0.0";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ObjectMapper mapper = new ObjectMapper();

    // preferences are stored per user name...
    private final String userName;

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
    private UiExtensionService uiextService;
    private UiPreferencesService prefService;


    // NOTE: we'll stick this here for now, but maybe there is a better home?
    //       (this is not distributed across the cluster)
    private static Map<String, ObjectNode> metaUi = new ConcurrentHashMap<>();


    /**
     * Creates an instance with a reference to the services directory, so that
     * additional information about network elements may be looked up on
     * on the fly.
     *
     * @param directory service directory
     * @param userName  logged in user name
     */
    public Topo2Jsonifier(ServiceDirectory directory, String userName) {
        this.directory = checkNotNull(directory, "Directory cannot be null");
        this.userName = checkNotNull(userName, "User name cannot be null");

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
        uiextService = directory.get(UiExtensionService.class);
        prefService = directory.get(UiPreferencesService.class);
    }

    // for unit testing
    Topo2Jsonifier() {
        userName = "(unit-test)";
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
        int switchCount = mastershipService.getDevicesOf(member.id()).size();
        return objectNode()
                .put("id", member.id().toString())
                .put("ip", member.ip().toString())
                .put("online", member.isOnline())
                .put("ready", member.isReady())
                .put("uiAttached", isUiAttached)
                .put("switches", switchCount);
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
        addBgRef(result, layout);
        return result;
    }

    private void addBgRef(ObjectNode result, UiTopoLayout layout) {
        String mapId = layout.geomap();
        String sprId = layout.sprites();

        if (mapId != null) {
            result.put("bgType", GEO).put("bgId", mapId);
            addMapParameters(result, mapId);
        } else if (sprId != null) {
            result.put("bgType", GRID).put("bgId", sprId);
        }
        addZoomPan(result, layout.id());
    }

    private void addZoomPan(ObjectNode result, UiTopoLayoutId layoutId) {
        // need to look up topo_zoom settings from preferences service.

        // NOTE:
        // UiPreferencesService API only allows us to retrieve ALL prefs for
        // the given user. It would be better if we could call something like:
        //
        //   ObjectNode value = prefService.getPreference(userName, prefKey);
        //
        // to get back a single value.

        Map<String, ObjectNode> userPrefs = prefService.getPreferences(userName);
        ObjectNode zoomPrefs = userPrefs.get(PKEY_TOPO_ZOOM);

        if (zoomPrefs == null) {
            // no zoom prefs structure yet.. so initialize..
            ObjectNode zoomForLayout = objectNode()
                    .put(ZOOM_SCALE, DEFAULT_SCALE)
                    .put(ZOOM_PAN_X, DEFAULT_PAN)
                    .put(ZOOM_PAN_Y, DEFAULT_PAN);

            zoomPrefs = objectNode();
            zoomPrefs.set(layoutId.id(), zoomForLayout);

            prefService.setPreference(userName, PKEY_TOPO_ZOOM, zoomPrefs);
        }
        ObjectNode zoomData = (ObjectNode) zoomPrefs.get(layoutId.id());

        result.put("bgZoomScale", zoomData.get(ZOOM_SCALE).asText());
        result.put("bgZoomPanX", zoomData.get(ZOOM_PAN_X).asText());
        result.put("bgZoomPanY", zoomData.get(ZOOM_PAN_Y).asText());
    }

    private void addMapParameters(ObjectNode result, String mapId) {

        // TODO: This ought to be written more efficiently.

        // ALSO: Should retrieving a UiTopoMap by ID be something that
        //       the UiExtensionService provides, along with other
        //       useful lookups?
        //
        //       Or should it remain very basic / general?
        //
        //       return uiextService.getTopoMap(String mapId);

        final UiTopoMap[] map = {null};

        uiextService.getExtensions().forEach(ext -> {
            UiTopoMapFactory factory = ext.topoMapFactory();

            // TODO: use .stream().filter(...) here
            if (map[0] == null && factory != null) {
                List<UiTopoMap> topoMaps = factory.geoMaps();

                topoMaps.forEach(m -> {
                    if (map[0] == null && m.id().equals(mapId)) {
                        map[0] = m;
                    }
                });
            }
        });

        UiTopoMap m = map[0];
        if (m != null) {
            result.put("bgDesc", m.description())
                    .put("bgFilePath", m.filePath())
                    .put("bgDefaultScale", m.scale());
        } else {
            result.put("bgWarn", "no map registered with id: " + mapId);
        }
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
        payload.set("subregions", jsonSubRegions(subRegions));
        payload.set("links", jsonLinks(links));

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

    /**
     * Creates a JSON representation of a UI element.
     *
     * @param element the source element
     * @return a JSON representation of that element
     */
    public ObjectNode jsonUiElement(UiElement element) {
        if (element instanceof UiNode) {
            return json((UiNode) element);
        }
        if (element instanceof UiLink) {
            return json((UiLink) element);
        }

        // TODO: UiClusterMember

        // Unrecognized UiElement class
        return objectNode()
                .put("warning", "unknown UiElement... cannot encode")
                .put("javaclass", element.getClass().toString());
    }

    /**
     * Creates a JSON representation of a UI model event.
     *
     * @param modelEvent the source model event
     * @return a JSON representation of that event
     */
    public ObjectNode jsonEvent(UiModelEvent modelEvent) {
        ObjectNode payload = objectNode();
        payload.put(TYPE, enumToString(modelEvent.type()));
        payload.put(SUBJECT, modelEvent.subject().idAsString());
        payload.set(DATA, modelEvent.data());
        payload.put(MEMO, modelEvent.memo());
        return payload;
    }

    // TODO: Investigate why we can't do this inline
    private String enumToString(Enum<?> e) {
        return e.toString();
    }

    // Returns the name of the master node for the specified device id.
    private String master(DeviceId deviceId) {
        NodeId master = mastershipService.getMasterFor(deviceId);
        return master != null ? master.toString() : "";
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
                .put("master", master(device.id()))
                .put("layer", device.layer());

        Device d = device.backingDevice();

        addProps(node, d);
        addGeoGridLocation(node, d);
        addMetaUi(node, device.idAsString());

        return node;
    }

    private void addProps(ObjectNode node, Annotated a) {
        Annotations annot = a.annotations();
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

    private void addGeoGridLocation(ObjectNode node, Annotated a) {
        List<String> lngLat = getAnnotValues(a, LONGITUDE, LATITUDE);
        List<String> gridYX = getAnnotValues(a, GRID_Y, GRID_X);

        if (lngLat != null) {
            attachLocation(node, "geo", "lng", "lat", lngLat);
        } else if (gridYX != null) {
            attachLocation(node, "grid", "gridY", "gridX", gridYX);
        }
    }

    private void attachLocation(ObjectNode node, String locType,
                                String keyA, String keyB, List<String> values) {
        try {
            double valA = Double.parseDouble(values.get(0));
            double valB = Double.parseDouble(values.get(1));
            ObjectNode loc = objectNode()
                    .put("type", locType)
                    .put(keyA, valA)
                    .put(keyB, valB);
            node.set("location", loc);

        } catch (NumberFormatException e) {
            log.warn("Invalid {} data: long/Y={}, lat/X={}",
                    locType, values.get(0), values.get(1));
        }
    }

    private void addIps(ObjectNode node, Host h) {
        Set<IpAddress> ips = h.ipAddresses();

        ArrayNode a = arrayNode();
        for (IpAddress ip : ips) {
            a.add(ip.toString());
        }

        node.set("ips", a);
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
        ObjectNode node = objectNode()
                .put("id", host.idAsString())
                .put("nodeType", HOST)
                .put("layer", host.layer());
        // TODO: complete host details
        Host h = host.backingHost();

        addIps(node, h);
        addProps(node, h);
        addGeoGridLocation(node, h);
        addMetaUi(node, host.idAsString());

        return node;
    }

    private ObjectNode json(UiSynthLink sLink) {
        return json(sLink.link());
    }

    private ObjectNode json(UiLink link) {
        ObjectNode data = objectNode()
                .put("id", link.idAsString())
                .put("epA", link.endPointA())
                .put("epB", link.endPointB())
                .put("type", link.type());
        String pA = link.endPortA();
        String pB = link.endPortB();
        if (pA != null) {
            data.put("portA", pA);
        }
        if (pB != null) {
            data.put("portB", pB);
        }
        return data;
    }


    private ObjectNode jsonClosedRegion(UiRegion region) {
        ObjectNode node = objectNode()
                .put("id", region.idAsString())
                .put("name", region.name())
                .put("nodeType", REGION)
                .put("nDevs", region.deviceCount())
                .put("nHosts", region.hostCount());

        Region r = region.backingRegion();
        addGeoGridLocation(node, r);
        addProps(node, r);

        addMetaUi(node, region.idAsString());
        return node;
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

    /**
     * Stores the memento for an element.
     * This method assumes the payload has an id String, memento ObjectNode
     *
     * @param payload event payload
     */
    void updateMeta(ObjectNode payload) {

        String id = JsonUtils.string(payload, "id");
        metaUi.put(id, JsonUtils.node(payload, "memento"));

        log.debug("Storing metadata for {}", id);
    }
}
