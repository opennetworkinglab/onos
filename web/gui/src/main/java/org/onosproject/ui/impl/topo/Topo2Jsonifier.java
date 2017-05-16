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
import com.google.common.base.Strings;
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
import org.onosproject.ui.model.topo.UiClusterMember;
import org.onosproject.ui.model.topo.UiDevice;
import org.onosproject.ui.model.topo.UiElement;
import org.onosproject.ui.model.topo.UiHost;
import org.onosproject.ui.model.topo.UiLink;
import org.onosproject.ui.model.topo.UiLinkId;
import org.onosproject.ui.model.topo.UiModelEvent;
import org.onosproject.ui.model.topo.UiNode;
import org.onosproject.ui.model.topo.UiRegion;
import org.onosproject.ui.model.topo.UiSynthLink;
import org.onosproject.ui.model.topo.UiTopoLayout;
import org.onosproject.ui.topo.LayoutLocation;
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
import static org.onosproject.ui.topo.LayoutLocation.fromCompactListString;

/**
 * Facility for creating JSON messages to send to the topology view in the
 * Web client.
 */
public class Topo2Jsonifier {

    private static final String E_DEF_NOT_LAST =
            "UiNode.LAYER_DEFAULT not last in layer list";
    private static final String E_UNKNOWN_UI_NODE =
            "Unknown subclass of UiNode: ";

    private static final String CONTEXT_KEY_DELIM = "_";
    private static final String NO_CONTEXT = "";
    private static final String ZOOM_KEY = "layoutZoom";

    private static final String REGION = "region";
    private static final String DEVICE = "device";
    private static final String HOST = "host";
    private static final String TYPE = "type";
    private static final String SUBJECT = "subject";
    private static final String DATA = "data";
    private static final String MEMO = "memo";

    private static final String GEO = "geo";
    private static final String GRID = "grid";
    private static final String PEER_LOCATIONS = "peerLocations";
    private static final String LOCATION = "location";
    private static final String LOC_TYPE = "locType";
    private static final String LAT_OR_Y = "latOrY";
    private static final String LONG_OR_X = "longOrX";

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

        attachZoomData(result, layout);
    }

    private void attachZoomData(ObjectNode result, UiTopoLayout layout) {

        ObjectNode zoomData = objectNode();

        // first, set configured scale and offset
        addCfgZoomData(zoomData, layout);

        // next, retrieve user-set zoom data, if we have it
        String rid = layout.regionId().toString();
        ObjectNode userZoom = metaUi.get(contextKey(rid, ZOOM_KEY));
        if (userZoom != null) {
            zoomData.set("usr", userZoom);
        }
        result.set("bgZoom", zoomData);
    }

    private void addCfgZoomData(ObjectNode data, UiTopoLayout layout) {
        ObjectNode zoom = objectNode();
        zoom.put("scale", layout.scale());
        zoom.put("offsetX", layout.offsetX());
        zoom.put("offsetY", layout.offsetY());
        data.set("cfg", zoom);
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

        String ridStr = region.idAsString();

        payload.put("id", ridStr);
        payload.set("subregions", jsonSubRegions(ridStr, subRegions));
        payload.set("links", jsonLinks(links));

        List<String> layerTags = region.layerOrder();
        List<Set<UiNode>> splitDevices = splitByLayer(layerTags, region.devices());
        List<Set<UiNode>> splitHosts = splitByLayer(layerTags, region.hosts());

        payload.set("devices", jsonGrouped(ridStr, splitDevices));
        payload.set("hosts", jsonGrouped(ridStr, splitHosts));
        payload.set("layerOrder", jsonStrings(layerTags));

        if (!region.isRoot()) {
            addPeerLocations(payload, region.backingRegion());
        }

        return payload;
    }

    private ArrayNode jsonSubRegions(String ridStr, Set<UiRegion> subregions) {
        ArrayNode kids = arrayNode();
        subregions.forEach(s -> kids.add(jsonClosedRegion(ridStr, s)));
        return kids;
    }

    protected JsonNode jsonLinks(List<UiSynthLink> links) {
        return collateSynthLinks(links);
    }

    private ArrayNode jsonStrings(List<String> strings) {
        ArrayNode array = arrayNode();
        strings.forEach(array::add);
        return array;
    }

    private ArrayNode jsonGrouped(String ridStr, List<Set<UiNode>> groupedNodes) {
        ArrayNode result = arrayNode();
        groupedNodes.forEach(g -> {
            ArrayNode subset = arrayNode();
            g.forEach(n -> subset.add(json(ridStr, n)));
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
            return json(NO_CONTEXT, (UiNode) element);
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

    private ObjectNode json(String ridStr, UiNode node) {
        if (node instanceof UiRegion) {
            return jsonClosedRegion(ridStr, (UiRegion) node);
        }
        if (node instanceof UiDevice) {
            return json(ridStr, (UiDevice) node);
        }
        if (node instanceof UiHost) {
            return json(ridStr, (UiHost) node);
        }
        throw new IllegalStateException(E_UNKNOWN_UI_NODE + node.getClass());
    }

    private ObjectNode json(String ridStr, UiDevice device) {
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
        addMetaUi(node, ridStr, device.idAsString());

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

    private void addMetaUi(ObjectNode node, String ridStr, String metaInstanceId) {
        String key = contextKey(ridStr, metaInstanceId);
        ObjectNode meta = metaUi.get(key);
        if (meta != null) {
            node.set("metaUi", meta);
        }
    }

    private void addGeoGridLocation(ObjectNode node, Annotated a) {
        List<String> latLongData = getAnnotValues(a, LATITUDE, LONGITUDE);
        List<String> gridYXdata = getAnnotValues(a, GRID_Y, GRID_X);

        if (latLongData != null) {
            attachLocation(node, GEO, latLongData);
        } else if (gridYXdata != null) {
            attachLocation(node, GRID, gridYXdata);
        }
    }

    private void attachLocation(ObjectNode node, String locType,
                                List<String> values) {
        try {
            double latOrY = Double.parseDouble(values.get(0));
            double longOrX = Double.parseDouble(values.get(1));
            ObjectNode loc = objectNode()
                    .put(LOC_TYPE, locType)
                    .put(LAT_OR_Y, latOrY)
                    .put(LONG_OR_X, longOrX);
            node.set(LOCATION, loc);

        } catch (NumberFormatException e) {
            log.warn("Invalid {} data: lat/Y={}, long/X={}",
                     locType, values.get(0), values.get(1));
        }
    }

    private void addPeerLocations(ObjectNode node, Region r) {
        String compact = r.annotations().value(PEER_LOCATIONS);
        if (!Strings.isNullOrEmpty(compact)) {
            List<LayoutLocation> locs = fromCompactListString(compact);

            ObjectNode o = objectNode();
            for (LayoutLocation ll : locs) {
                ObjectNode lnode = objectNode()
                        .put(LOC_TYPE, ll.locType().toString())
                        .put(LAT_OR_Y, ll.latOrY())
                        .put(LONG_OR_X, ll.longOrX());
                o.set(ll.id(), lnode);
            }

            node.set(PEER_LOCATIONS, o);
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

    private ObjectNode json(String ridStr, UiHost host) {
        ObjectNode node = objectNode()
                .put("id", host.idAsString())
                .put("nodeType", HOST)
                .put("layer", host.layer());
        // TODO: complete host details
        Host h = host.backingHost();

        // h will be null, for example, after a HOST_REMOVED event
        if (h != null) {
            addIps(node, h);
            addProps(node, h);
            addGeoGridLocation(node, h);
        }
        addMetaUi(node, ridStr, host.idAsString());

        return node;
    }

    private ArrayNode collateSynthLinks(List<UiSynthLink> links) {
        Map<UiLinkId, Set<UiSynthLink>> collation = new HashMap<>();

        // first, group together the synthlinks into sets per ID...
        for (UiSynthLink sl : links) {
            UiLinkId id = sl.link().id();
            Set<UiSynthLink> rollup =
                    collation.computeIfAbsent(id, k -> new HashSet<>());
            rollup.add(sl);
        }

        // now add json nodes per set, and return the array of them
        ArrayNode array = arrayNode();
        for (UiLinkId id : collation.keySet()) {
            array.add(json(collation.get(id)));
        }
        return array;
    }

    private ObjectNode json(Set<UiSynthLink> memberSet) {
        ArrayNode rollup = arrayNode();
        ObjectNode node = null;

        boolean first = true;
        for (UiSynthLink member : memberSet) {
            UiLink link = member.link();
            if (first) {
                node = json(link);
                first = false;
            }
            rollup.add(json(member.original()));
        }
        if (node != null) {
            node.set("rollup", rollup);
        }
        return node;
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


    private ObjectNode jsonClosedRegion(String ridStr, UiRegion region) {
        ObjectNode node = objectNode()
                .put("id", region.idAsString())
                .put("name", region.name())
                .put("nodeType", REGION)
                .put("nDevs", region.deviceCount())
                .put("nHosts", region.hostCount());
        // TODO: device and host counts should take into account any nested
        //       subregions. i.e. should be the sum of all devices/hosts in
        //       all descendant subregions.

        Region r = region.backingRegion();
        if (r != null) {
            // add data injected via network configuration script
            addGeoGridLocation(node, r);
            addProps(node, r);
        }

        // this may contain location data, as dragged by user
        // (which should take precedence, over configured data)
        addMetaUi(node, ridStr, region.idAsString());
        return node;
    }

    /**
     * Returns a JSON array representation of a set of regions/devices. Note
     * that the information is sufficient for showing regions as nodes.
     * THe region ID string defines the context (which region) the node is
     * being displayed in.
     *
     * @param ridStr region-id string
     * @param nodes  the nodes
     * @return a JSON representation of the nodes
     */
    public ArrayNode closedNodes(String ridStr, Set<UiNode> nodes) {
        ArrayNode array = arrayNode();
        for (UiNode node : nodes) {
            if (node instanceof UiRegion) {
                array.add(jsonClosedRegion(ridStr, (UiRegion) node));
            } else if (node instanceof UiDevice) {
                array.add(json(ridStr, (UiDevice) node));
            } else {
                log.warn("Unexpected node instance: {}", node.getClass());
            }
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


    private String contextKey(String context, String key) {
        return context + CONTEXT_KEY_DELIM + key;
    }

    /**
     * Stores the memento for an element.
     * This method assumes the payload has an id String, memento ObjectNode.
     * The region-id string is used as a context within which to store the
     * memento.
     *
     * @param ridStr  region ID string
     * @param payload event payload
     */
    void updateMeta(String ridStr, ObjectNode payload) {

        String id = JsonUtils.string(payload, "id");
        String key = contextKey(ridStr, id);
        metaUi.put(key, JsonUtils.node(payload, "memento"));

        log.debug("Storing metadata for {}", key);
    }
}
