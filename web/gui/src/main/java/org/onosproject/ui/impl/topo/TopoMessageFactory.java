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
 *
 */

package org.onosproject.ui.impl.topo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Annotated;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Annotations;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultEdgeLink;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.EdgeLink;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.ui.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onosproject.cluster.ControllerNode.State.ACTIVE;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Facility for generating messages in {@link ObjectNode} form from
 * ONOS model events.
 */
// package private
class TopoMessageFactory {

    private static final ProviderId PROVIDER_ID =
            new ProviderId("core", "org.onosproject.core", true);
    private static final String COMPACT = "%s/%s-%s/%s";
    private static final PortNumber PORT_ZERO = portNumber(0);

    private static final Map<Enum<?>, String> LOOKUP = new HashMap<>();

    static {
        LOOKUP.put(ClusterEvent.Type.INSTANCE_ADDED, "addInstance");
        LOOKUP.put(ClusterEvent.Type.INSTANCE_REMOVED, "removeInstance");
        LOOKUP.put(DeviceEvent.Type.DEVICE_ADDED, "addDevice");
        LOOKUP.put(DeviceEvent.Type.DEVICE_UPDATED, "updateDevice");
        LOOKUP.put(DeviceEvent.Type.DEVICE_REMOVED, "removeDevice");
        LOOKUP.put(LinkEvent.Type.LINK_ADDED, "addLink");
        LOOKUP.put(LinkEvent.Type.LINK_UPDATED, "updateLink");
        LOOKUP.put(LinkEvent.Type.LINK_REMOVED, "removeLink");
        LOOKUP.put(HostEvent.Type.HOST_ADDED, "addHost");
        LOOKUP.put(HostEvent.Type.HOST_UPDATED, "updateHost");
        LOOKUP.put(HostEvent.Type.HOST_REMOVED, "removeHost");
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Logger log = LoggerFactory.getLogger(getClass());

    private MetaDb metaDb;

    private ClusterService clusterService;
    private DeviceService deviceService;
    private LinkService linkService;
    private HostService hostService;
    private MastershipService mastershipService;


    // ===================================================================
    // Private helper methods

    private ObjectNode objectNode() {
        return MAPPER.createObjectNode();
    }

    private ArrayNode arrayNode() {
        return MAPPER.createArrayNode();
    }

    private String toLc(Object o) {
        return o.toString().toLowerCase();
    }

    // Event type to message type lookup (with fallback).
    private String messageTypeLookup(Enum<?> type, String fallback) {
        String msgType = LOOKUP.get(type);
        return msgType == null ? fallback : msgType;
    }

    // Returns the name of the master node for the specified device ID.
    private String master(DeviceId deviceId) {
        NodeId master = mastershipService.getMasterFor(deviceId);
        return master != null ? master.toString() : "";
    }

    // Produces JSON structure from annotations.
    private ObjectNode props(Annotations annotations) {
        ObjectNode props = objectNode();
        if (annotations != null) {
            for (String key : annotations.keys()) {
                props.put(key, annotations.value(key));
            }
        }
        return props;
    }

    // Adds a geo location JSON to the specified payload object.
    private void addGeoLocation(Annotated annotated, ObjectNode payload) {
        Annotations annot = annotated.annotations();
        if (annot == null) {
            return;
        }

        String slat = annot.value(AnnotationKeys.LATITUDE);
        String slng = annot.value(AnnotationKeys.LONGITUDE);
        try {
            if (!isNullOrEmpty(slat) && !isNullOrEmpty(slng)) {
                double lat = Double.parseDouble(slat);
                double lng = Double.parseDouble(slng);
                ObjectNode loc = objectNode()
                        .put("type", "latlng")
                        .put("lat", lat)
                        .put("lng", lng);
                payload.set("location", loc);
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid geo data latitude={}; longitude={}", slat, slng);
        }
    }

    // Produces compact string representation of a link.
    private String compactLinkString(Link link) {
        return String.format(COMPACT, link.src().elementId(), link.src().port(),
                             link.dst().elementId(), link.dst().port());
    }

    // Generates an edge link from the specified host location.
    private EdgeLink edgeLink(Host host, boolean isIngress) {
        ConnectPoint cp = new ConnectPoint(host.id(), PORT_ZERO);
        return new DefaultEdgeLink(PROVIDER_ID, cp, host.location(), isIngress);
    }

    // Encodes the specified host location into a JSON object.
    private ObjectNode hostConnect(HostLocation loc) {
        return objectNode()
                .put("device", loc.deviceId().toString())
                .put("port", loc.port().toLong());
    }

    // Returns the first IP address from the specified set.
    private String firstIp(Set<IpAddress> addresses) {
        Iterator<IpAddress> it = addresses.iterator();
        return it.hasNext() ? it.next().toString() : "unknown";
    }

    // Returns a JSON array of the specified strings.
    private ArrayNode labels(String... labels) {
        ArrayNode array = arrayNode();
        for (String label : labels) {
            array.add(label);
        }
        return array;
    }

    // ===================================================================
    // API for generating messages

    /**
     * Injects service references so that the message compilation methods
     * can do required lookups when needed.
     *
     * @param meta meta DB
     * @param cs cluster service
     * @param ds device service
     * @param ls link service
     * @param hs host service
     * @param ms mastership service
     */
    public void injectServices(MetaDb meta, ClusterService cs, DeviceService ds,
                               LinkService ls, HostService hs,
                               MastershipService ms) {
        metaDb = meta;
        clusterService = cs;
        deviceService = ds;
        linkService = ls;
        hostService = hs;
        mastershipService = ms;
    }

    /**
     * Transforms a cluster event into an object-node-based message.
     *
     * @param ev cluster event
     * @return marshaled event message
     */
    public ObjectNode instanceMessage(ClusterEvent ev) {
        ControllerNode node = ev.subject();
        NodeId nid = node.id();
        String id = nid.toString();
        String ip = node.ip().toString();
        int switchCount = mastershipService.getDevicesOf(nid).size();

        ObjectNode payload = objectNode()
                .put("id", id)
                .put("ip", ip)
                .put("online", clusterService.getState(nid) == ACTIVE)
                .put("uiAttached", node.equals(clusterService.getLocalNode()))
                .put("switches", switchCount);

        ArrayNode labels = arrayNode().add(id).add(ip);

        payload.set("labels", labels);
        metaDb.addMetaUi(id, payload);

        String msgType = messageTypeLookup(ev.type(), "addInstance");
        return JsonUtils.envelope(msgType, payload);
    }

    /**
     * Transforms a device event into an object-node-based message.
     *
     * @param ev device event
     * @return marshaled event message
     */
    public ObjectNode deviceMessage(DeviceEvent ev) {
        Device device = ev.subject();
        DeviceId did = device.id();
        String id = did.toString();

        ObjectNode payload = objectNode()
                .put("id", id)
                .put("type", toLc(device.type()))
                .put("online", deviceService.isAvailable(did))
                .put("master", master(did));

        Annotations annot = device.annotations();
        String name = annot.value(AnnotationKeys.NAME);
        String friendly = isNullOrEmpty(name) ? id : name;
        payload.set("labels", labels("", friendly, id));
        payload.set("props", props(annot));

        addGeoLocation(device, payload);
        metaDb.addMetaUi(id, payload);

        String msgType = messageTypeLookup(ev.type(), "updateDevice");
        return JsonUtils.envelope(msgType, payload);
    }

    /**
     * Transforms a link event into an object-node-based message.
     *
     * @param ev link event
     * @return marshaled event message
     */
    public ObjectNode linkMessage(LinkEvent ev) {
        Link link = ev.subject();
        ObjectNode payload = objectNode()
                .put("id", compactLinkString(link))
                .put("type", toLc(link.type()))
                .put("online", link.state() == Link.State.ACTIVE)
                .put("linkWidth", 1.2)
                .put("src", link.src().deviceId().toString())
                .put("srcPort", link.src().port().toString())
                .put("dst", link.dst().deviceId().toString())
                .put("dstPort", link.dst().port().toString());

        String msgType = messageTypeLookup(ev.type(), "updateLink");
        return JsonUtils.envelope(msgType, payload);
    }

    /**
     * Transforms a host event into an object-node-based message.
     *
     * @param ev host event
     * @return marshaled event message
     */
    public ObjectNode hostMessage(HostEvent ev) {
        Host host = ev.subject();
        HostId hid = host.id();
        String id = hid.toString();
        Annotations annot = host.annotations();

        String hostType = annot.value(AnnotationKeys.TYPE);

        ObjectNode payload = objectNode()
                .put("id", id)
                .put("type", isNullOrEmpty(hostType) ? "endstation" : hostType)
                .put("ingress", compactLinkString(edgeLink(host, true)))
                .put("egress", compactLinkString(edgeLink(host, false)));

        // TODO: make cp an array of connect point objects (multi-homed)
        payload.set("cp", hostConnect(host.location()));
        String ipStr = firstIp(host.ipAddresses());
        String macStr = host.mac().toString();
        payload.set("labels", labels(ipStr, macStr));
        payload.set("props", props(annot));
        addGeoLocation(host, payload);
        metaDb.addMetaUi(id, payload);

        String mstType = messageTypeLookup(ev.type(), "updateHost");
        return JsonUtils.envelope(mstType, payload);
    }
}
