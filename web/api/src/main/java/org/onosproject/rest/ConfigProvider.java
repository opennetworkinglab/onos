/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.rest;

import com.fasterxml.jackson.databind.JsonNode;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Link;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.Port;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.host.DefaultHostDescription;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.host.HostProviderRegistry;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.provider.ProviderId;
import org.onlab.packet.ChassisId;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Provider of devices and links parsed from a JSON configuration structure.
 */
class ConfigProvider implements DeviceProvider, LinkProvider, HostProvider {

    private static final ProviderId PID =
            new ProviderId("cfg", "org.onosproject.rest", true);

    private final JsonNode cfg;
    private final DeviceProviderRegistry deviceProviderRegistry;
    private final LinkProviderRegistry linkProviderRegistry;
    private final HostProviderRegistry hostProviderRegistry;

    /**
     * Creates a new configuration provider.
     *
     * @param cfg                    JSON configuration
     * @param deviceProviderRegistry device provider registry
     * @param linkProviderRegistry   link provider registry
     * @param hostProviderRegistry   host provider registry
     */
    ConfigProvider(JsonNode cfg,
                   DeviceProviderRegistry deviceProviderRegistry,
                   LinkProviderRegistry linkProviderRegistry,
                   HostProviderRegistry hostProviderRegistry) {
        this.cfg = checkNotNull(cfg, "Configuration cannot be null");
        this.deviceProviderRegistry = checkNotNull(deviceProviderRegistry, "Device provider registry cannot be null");
        this.linkProviderRegistry = checkNotNull(linkProviderRegistry, "Link provider registry cannot be null");
        this.hostProviderRegistry = checkNotNull(hostProviderRegistry, "Host provider registry cannot be null");
    }

    /**
     * Parses the given JSON and provides links as configured.
     */
    void parse() {
        parseDevices();
        parseLinks();
        parseHosts();
    }

    // Parses the given JSON and provides devices.
    private void parseDevices() {
        try {
            DeviceProviderService dps = deviceProviderRegistry.register(this);
            JsonNode nodes = cfg.get("devices");
            if (nodes != null) {
                for (JsonNode node : nodes) {
                    parseDevice(dps, node);
                }
            }
        } finally {
            deviceProviderRegistry.unregister(this);
        }
    }

    // Parses the given node with device data and supplies the device.
    private void parseDevice(DeviceProviderService dps, JsonNode node) {
        URI uri = URI.create(get(node, "uri"));
        Device.Type type = Device.Type.valueOf(get(node, "type"));
        String mfr = get(node, "mfr");
        String hw = get(node, "hw");
        String sw = get(node, "sw");
        String serial = get(node, "serial");
        ChassisId cid = new ChassisId(get(node, "mac"));
        SparseAnnotations annotations = annotations(node.get("annotations"));

        DeviceDescription desc =
                new DefaultDeviceDescription(uri, type, mfr, hw, sw, serial,
                                             cid, annotations);
        DeviceId deviceId = deviceId(uri);
        dps.deviceConnected(deviceId, desc);

        JsonNode ports = node.get("ports");
        if (ports != null) {
            parsePorts(dps, deviceId, ports);
        }
    }

    // Parses the given node with list of device ports.
    private void parsePorts(DeviceProviderService dps, DeviceId deviceId, JsonNode nodes) {
        List<PortDescription> ports = new ArrayList<>();
        for (JsonNode node : nodes) {
            ports.add(parsePort(node));
        }
        dps.updatePorts(deviceId, ports);
    }

    // Parses the given node with port information.
    private PortDescription parsePort(JsonNode node) {
        Port.Type type = Port.Type.valueOf(node.path("type").asText("COPPER"));
        return new DefaultPortDescription(portNumber(node.path("port").asLong(0)),
                                          node.path("enabled").asBoolean(true),
                                          type, node.path("speed").asLong(1_000));
    }

    // Parses the given JSON and provides links as configured.
    private void parseLinks() {
        try {
            LinkProviderService lps = linkProviderRegistry.register(this);
            JsonNode nodes = cfg.get("links");
            if (nodes != null) {
                for (JsonNode node : nodes) {
                    parseLink(lps, node, false);
                    if (!node.has("halfplex")) {
                        parseLink(lps, node, true);
                    }
                }
            }
        } finally {
            linkProviderRegistry.unregister(this);
        }
    }

    // Parses the given node with link data and supplies the link.
    private void parseLink(LinkProviderService lps, JsonNode node, boolean reverse) {
        ConnectPoint src = connectPoint(get(node, "src"));
        ConnectPoint dst = connectPoint(get(node, "dst"));
        Link.Type type = Link.Type.valueOf(get(node, "type"));
        SparseAnnotations annotations = annotations(node.get("annotations"));

        DefaultLinkDescription desc = reverse ?
                new DefaultLinkDescription(dst, src, type, annotations) :
                new DefaultLinkDescription(src, dst, type, annotations);
        lps.linkDetected(desc);
    }

    // Parses the given JSON and provides hosts as configured.
    private void parseHosts() {
        try {
            HostProviderService hps = hostProviderRegistry.register(this);
            JsonNode nodes = cfg.get("hosts");
            if (nodes != null) {
                for (JsonNode node : nodes) {
                    parseHost(hps, node);
                }
            }
        } finally {
            hostProviderRegistry.unregister(this);
        }
    }

    // Parses the given node with host data and supplies the host.
    private void parseHost(HostProviderService hps, JsonNode node) {
        MacAddress mac = MacAddress.valueOf(get(node, "mac"));
        VlanId vlanId = VlanId.vlanId(node.get("vlan").shortValue());
        HostId hostId = HostId.hostId(mac, vlanId);
        SparseAnnotations annotations = annotations(node.get("annotations"));
        HostLocation location = new HostLocation(connectPoint(get(node, "location")), 0);

        String[] ipStrings = get(node, "ip").split(",");
        Set<IpAddress> ips = new HashSet<>();
        for (String ip : ipStrings) {
            ips.add(IpAddress.valueOf(ip.trim()));
        }

        DefaultHostDescription desc =
                new DefaultHostDescription(mac, vlanId, location, ips, annotations);
        hps.hostDetected(hostId, desc);
    }

    // Produces set of annotations from the given JSON node.
    private SparseAnnotations annotations(JsonNode node) {
        if (node == null) {
            return null;
        }

        DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
        Iterator<String> it = node.fieldNames();
        while (it.hasNext()) {
            String k = it.next();
            builder.set(k, node.get(k).asText());
        }
        return builder.build();
    }

    // Produces a connection point from the specified uri/port text.
    private ConnectPoint connectPoint(String text) {
        int i = text.lastIndexOf("/");
        return new ConnectPoint(deviceId(text.substring(0, i)),
                                portNumber(text.substring(i + 1)));
    }

    // Returns string form of the named property in the given JSON object.
    private String get(JsonNode node, String name) {
        return node.path(name).asText();
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {
    }

    @Override
    public void roleChanged(DeviceId device, MastershipRole newRole) {
    }

    @Override
    public void triggerProbe(Host host) {
    }

    @Override
    public ProviderId id() {
        return PID;
    }

    @Override
    public boolean isReachable(DeviceId device) {
        return false;
    }
}
