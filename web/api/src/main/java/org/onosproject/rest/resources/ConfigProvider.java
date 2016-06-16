/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.rest.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.onlab.packet.ChassisId;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.Frequency;
import org.onlab.util.Spectrum;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.GridType;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Link;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.OchPort;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduCltPort;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.OmsPort;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.OchPortDescription;
import org.onosproject.net.device.OduCltPortDescription;
import org.onosproject.net.device.OmsPortDescription;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_ADDED;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED;

/**
 * Provider of devices and links parsed from a JSON configuration structure.
 *
 * @deprecated in 1.5.0 (Falcon)
 */
@Deprecated
class ConfigProvider implements DeviceProvider, LinkProvider, HostProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final ProviderId PID =
            new ProviderId("cfg", "org.onosproject.rest", true);

    private static final String UNKNOWN = "unknown";

    // C-band has 4.4 THz (4,400 GHz) total bandwidth
    private static final Frequency TOTAL = Frequency.ofGHz(4_400);

    private CountDownLatch deviceLatch;

    private final JsonNode cfg;
    private final DeviceService deviceService;

    private final DeviceProviderRegistry deviceProviderRegistry;
    private final LinkProviderRegistry linkProviderRegistry;
    private final HostProviderRegistry hostProviderRegistry;

    private DeviceProviderService deviceProviderService;
    private LinkProviderService linkProviderService;
    private HostProviderService hostProviderService;

    private DeviceListener deviceEventCounter = new DeviceEventCounter();
    private List<ConnectPoint> connectPoints = Lists.newArrayList();
    private Map<ConnectPoint, PortDescription> descriptions = Maps.newHashMap();

    /**
     * Creates a new configuration provider.
     *
     * @param cfg                    JSON configuration
     * @param deviceService          device service
     * @param deviceProviderRegistry device provider registry
     * @param linkProviderRegistry   link provider registry
     * @param hostProviderRegistry   host provider registry
     */
    ConfigProvider(JsonNode cfg,
                   DeviceService deviceService,
                   DeviceProviderRegistry deviceProviderRegistry,
                   LinkProviderRegistry linkProviderRegistry,
                   HostProviderRegistry hostProviderRegistry) {
        this.cfg = checkNotNull(cfg, "Configuration cannot be null");
        this.deviceService = checkNotNull(deviceService, "Device service cannot be null");
        this.deviceProviderRegistry = checkNotNull(deviceProviderRegistry, "Device provider registry cannot be null");
        this.linkProviderRegistry = checkNotNull(linkProviderRegistry, "Link provider registry cannot be null");
        this.hostProviderRegistry = checkNotNull(hostProviderRegistry, "Host provider registry cannot be null");
    }

    /**
     * Parses the given JSON and provides links as configured.
     */
    void parse() {
        try {
            register();
            parseDevices();
            parseLinks();
            parseHosts();
            addMissingPorts();
        } finally {
            unregister();
        }
    }

    private void register() {
        deviceProviderService = deviceProviderRegistry.register(this);
        linkProviderService = linkProviderRegistry.register(this);
        hostProviderService = hostProviderRegistry.register(this);
    }

    private void unregister() {
        deviceProviderRegistry.unregister(this);
        linkProviderRegistry.unregister(this);
        hostProviderRegistry.unregister(this);
    }

    // Parses the given JSON and provides devices.
    private void parseDevices() {
        try {
            JsonNode nodes = cfg.get("devices");
            if (nodes != null) {
                prepareForDeviceEvents(nodes.size());
                for (JsonNode node : nodes) {
                    parseDevice(node);

                    // FIXME: hack to make sure device attributes take
                    // This will be fixed when GossipDeviceStore uses ECM
                    parseDevice(node);
                }
            }
        } finally {
            waitForDeviceEvents();
        }
    }

    // Parses the given node with device data and supplies the device.
    private void parseDevice(JsonNode node) {
        URI uri = URI.create(get(node, "uri"));
        Device.Type type = Device.Type.valueOf(get(node, "type", "SWITCH"));
        String mfr = get(node, "mfr", UNKNOWN);
        String hw = get(node, "hw", UNKNOWN);
        String sw = get(node, "sw", UNKNOWN);
        String serial = get(node, "serial", UNKNOWN);
        ChassisId cid = new ChassisId(get(node, "mac", "000000000000"));
        SparseAnnotations annotations = annotations(node.get("annotations"));

        DeviceDescription desc =
                new DefaultDeviceDescription(uri, type, mfr, hw, sw, serial,
                                             cid, annotations);
        DeviceId deviceId = deviceId(uri);
        deviceProviderService.deviceConnected(deviceId, desc);

        JsonNode ports = node.get("ports");
        if (ports != null) {
            parsePorts(deviceId, ports);
        }
    }

    // Parses the given node with list of device ports.
    private void parsePorts(DeviceId deviceId, JsonNode nodes) {
        List<PortDescription> ports = new ArrayList<>();
        for (JsonNode node : nodes) {
            ports.add(parsePort(deviceId, node));
        }
        deviceProviderService.updatePorts(deviceId, ports);
    }

    // Parses the given node with port information.
    private PortDescription parsePort(DeviceId deviceId, JsonNode node) {
        Port.Type type = Port.Type.valueOf(node.path("type").asText("COPPER"));
        // TL1-based ports have a name
        PortNumber port = null;
        if (node.has("name")) {
            for (Port p : deviceService.getPorts(deviceId)) {
                if (p.number().name().equals(node.get("name").asText())) {
                    port = p.number();
                    break;
                }
            }
        } else {
            port = portNumber(node.path("port").asLong(0));
        }

        if (port == null) {
            log.error("Cannot find port given in node {}", node);
            return null;
        }

        String portName = Strings.emptyToNull(port.name());
        SparseAnnotations annotations  = null;
        if (portName != null) {
            annotations = DefaultAnnotations.builder()
                    .set(AnnotationKeys.PORT_NAME, portName).build();
        }
        switch (type) {
            case COPPER:
                return new DefaultPortDescription(port, node.path("enabled").asBoolean(true),
                                                  type, node.path("speed").asLong(1_000),
                                                  annotations);
            case FIBER:
                // Currently, assume OMS when FIBER. Provide sane defaults.
                annotations = annotations(node.get("annotations"));
                return new OmsPortDescription(port, node.path("enabled").asBoolean(true),
                        Spectrum.CENTER_FREQUENCY, Spectrum.CENTER_FREQUENCY.add(TOTAL),
                                              Frequency.ofGHz(100), annotations);
            case ODUCLT:
                annotations = annotations(node.get("annotations"));
                OduCltPort oduCltPort = (OduCltPort) deviceService.getPort(deviceId, port);
                return new OduCltPortDescription(port, node.path("enabled").asBoolean(true),
                        oduCltPort.signalType(), annotations);
            case OCH:
                annotations = annotations(node.get("annotations"));
                OchPort ochPort = (OchPort) deviceService.getPort(deviceId, port);
                return new OchPortDescription(port, node.path("enabled").asBoolean(true),
                        ochPort.signalType(), ochPort.isTunable(),
                        ochPort.lambda(), annotations);
            case OMS:
                annotations = annotations(node.get("annotations"));
                OmsPort omsPort = (OmsPort) deviceService.getPort(deviceId, port);
                return new OmsPortDescription(port, node.path("enabled").asBoolean(true),
                        omsPort.minFrequency(), omsPort.maxFrequency(), omsPort.grid(), annotations);
            default:
                log.warn("{}: Unsupported Port Type");
        }
        return new DefaultPortDescription(port, node.path("enabled").asBoolean(true),
                                          type, node.path("speed").asLong(1_000),
                                          annotations);
    }

    // Parses the given JSON and provides links as configured.
    private void parseLinks() {
        JsonNode nodes = cfg.get("links");
        if (nodes != null) {
            for (JsonNode node : nodes) {
                parseLink(node, false);
                if (!node.has("halfplex")) {
                    parseLink(node, true);
                }
            }
        }
    }

    // Parses the given node with link data and supplies the link.
    private void parseLink(JsonNode node, boolean reverse) {
        ConnectPoint src = connectPoint(get(node, "src"));
        ConnectPoint dst = connectPoint(get(node, "dst"));
        Link.Type type = Link.Type.valueOf(get(node, "type", "DIRECT"));
        SparseAnnotations annotations = annotations(node.get("annotations"));
        // take annotations to update optical ports with correct attributes.
        updatePorts(src, dst, annotations);
        DefaultLinkDescription desc = reverse ?
                new DefaultLinkDescription(dst, src, type, annotations) :
                new DefaultLinkDescription(src, dst, type, annotations);
        linkProviderService.linkDetected(desc);

        connectPoints.add(src);
        connectPoints.add(dst);
    }

    private void updatePorts(ConnectPoint src, ConnectPoint dst, SparseAnnotations annotations) {
        final String linkType = annotations.value("optical.type");
        if ("cross-connect".equals(linkType)) {
            String value = annotations.value("bandwidth").trim();
            try {
                double bw = Double.parseDouble(value);
                updateOchPort(bw, src, dst);
            } catch (NumberFormatException e) {
                log.warn("Invalid bandwidth ({}), can't configure port(s)", value);
                return;
            }
        } else if ("WDM".equals(linkType)) {
            String value = annotations.value("optical.waves").trim();
            try {
                int numChls = Integer.parseInt(value);
                updateOmsPorts(numChls, src, dst);
            } catch (NumberFormatException e) {
                log.warn("Invalid channel ({}), can't configure port(s)", value);
                return;
            }
        }
    }

    // uses 'bandwidth' annotation to determine the channel spacing.
    private void updateOchPort(double bw, ConnectPoint srcCp, ConnectPoint dstCp) {
        Device src = deviceService.getDevice(srcCp.deviceId());
        Device dst = deviceService.getDevice(dstCp.deviceId());
        // bandwidth in MHz (assuming Hz - linc is not clear if that or Mb).
        Frequency spacing = Frequency.ofMHz(bw);
        // channel bandwidth is smaller than smallest standard channel spacing.
        ChannelSpacing chsp = null;
        if (spacing.compareTo(ChannelSpacing.CHL_6P25GHZ.frequency()) <= 0) {
            chsp = ChannelSpacing.CHL_6P25GHZ;
        }
        for (int i = 1; i < ChannelSpacing.values().length; i++) {
            Frequency val = ChannelSpacing.values()[i].frequency();
            // pick the next highest or equal channel interval.
            if (val.isLessThan(spacing)) {
                chsp = ChannelSpacing.values()[i - 1];
                break;
            }
        }
        if (chsp == null) {
            log.warn("Invalid channel spacing ({}), can't configure port(s)", spacing);
            return;
        }
        OchSignal signal = new OchSignal(GridType.DWDM, chsp, 1, 1);
        if (src.type() == Device.Type.ROADM) {
            PortDescription portDesc = new OchPortDescription(srcCp.port(), true,
                    OduSignalType.ODU4, true, signal);
            descriptions.put(srcCp, portDesc);
            deviceProviderService.portStatusChanged(srcCp.deviceId(), portDesc);
        }
        if (dst.type() == Device.Type.ROADM) {
            PortDescription portDesc = new OchPortDescription(dstCp.port(), true,
                    OduSignalType.ODU4, true, signal);
            descriptions.put(dstCp, portDesc);
            deviceProviderService.portStatusChanged(dstCp.deviceId(), portDesc);
        }
    }

    private void updateOmsPorts(int numChls, ConnectPoint srcCp, ConnectPoint dstCp) {
        // round down to largest slot that allows numChl channels to fit into C band range
        ChannelSpacing chl = null;
        Frequency perChl = TOTAL.floorDivision(numChls);
        for (int i = 0; i < ChannelSpacing.values().length; i++) {
            Frequency val = ChannelSpacing.values()[i].frequency();
            if (val.isLessThan(perChl)) {
                chl = ChannelSpacing.values()[i];
                break;
            }
        }
        if (chl == null) {
            chl = ChannelSpacing.CHL_6P25GHZ;
        }

        // if true, there was less channels than can be tightly packed.
        Frequency grid = chl.frequency();
        // say Linc's 1st slot starts at CENTER and goes up from there.
        Frequency min = Spectrum.CENTER_FREQUENCY.add(grid);
        Frequency max = Spectrum.CENTER_FREQUENCY.add(grid.multiply(numChls));

        PortDescription srcPortDesc = new OmsPortDescription(srcCp.port(), true, min, max, grid);
        PortDescription dstPortDesc = new OmsPortDescription(dstCp.port(), true, min, max, grid);
        descriptions.put(srcCp, srcPortDesc);
        descriptions.put(dstCp, dstPortDesc);
        deviceProviderService.portStatusChanged(srcCp.deviceId(), srcPortDesc);
        deviceProviderService.portStatusChanged(dstCp.deviceId(), dstPortDesc);
    }

    // Parses the given JSON and provides hosts as configured.
    private void parseHosts() {
        try {
            JsonNode nodes = cfg.get("hosts");
            if (nodes != null) {
                for (JsonNode node : nodes) {
                    parseHost(node);

                    // FIXME: hack to make sure host attributes take
                    // This will be fixed when GossipHostStore uses ECM
                    parseHost(node);
                }
            }
        } finally {
            hostProviderRegistry.unregister(this);
        }
    }

    // Parses the given node with host data and supplies the host.
    private void parseHost(JsonNode node) {
        MacAddress mac = MacAddress.valueOf(get(node, "mac"));
        VlanId vlanId = VlanId.vlanId((short) node.get("vlan").asInt(VlanId.UNTAGGED));
        HostId hostId = HostId.hostId(mac, vlanId);
        SparseAnnotations annotations = annotations(node.get("annotations"));
        HostLocation location = new HostLocation(connectPoint(get(node, "location")), 0);

        String[] ipStrings = get(node, "ip", "").split(",");
        Set<IpAddress> ips = new HashSet<>();
        for (String ip : ipStrings) {
            ips.add(IpAddress.valueOf(ip.trim()));
        }

        DefaultHostDescription desc =
                new DefaultHostDescription(mac, vlanId, location, ips, annotations);
        hostProviderService.hostDetected(hostId, desc, false);

        connectPoints.add(location);
    }

    // Adds any missing device ports for configured links and host locations.
    private void addMissingPorts() {
        deviceService.getDevices().forEach(this::addMissingPorts);
    }

    // Adds any missing device ports.
    private void addMissingPorts(Device device) {
        try {
            List<Port> ports = deviceService.getPorts(device.id());
            Set<ConnectPoint> existing = ports.stream()
                    .map(p -> new ConnectPoint(device.id(), p.number()))
                    .collect(Collectors.toSet());
            Set<ConnectPoint> missing = connectPoints.stream()
                    .filter(cp -> cp.deviceId().equals(device.id()))
                    .filter(cp -> !existing.contains(cp))
                    .collect(Collectors.toSet());

            if (!missing.isEmpty()) {
                List<PortDescription> newPorts = Stream.concat(
                        ports.stream().map(this::description),
                        missing.stream().map(this::description)
                ).collect(Collectors.toList());
                deviceProviderService.updatePorts(device.id(), newPorts);
            }
        } catch (IllegalArgumentException e) {
            log.warn("Error pushing ports: {}", e.getMessage());
        }
    }

    // Creates a port description from the specified port.
    private PortDescription description(Port p) {
        switch (p.type()) {
            case OMS:
                OmsPort op = (OmsPort) p;
                return new OmsPortDescription(
                        op.number(), op.isEnabled(), op.minFrequency(), op.maxFrequency(), op.grid());
            case OCH:
                OchPort ochp = (OchPort) p;
                return new OchPortDescription(
                        ochp.number(), ochp.isEnabled(), ochp.signalType(), ochp.isTunable(), ochp.lambda());
            case ODUCLT:
                OduCltPort odup = (OduCltPort) p;
                return new OduCltPortDescription(
                        odup.number(), odup.isEnabled(), odup.signalType());
            default:
                return new DefaultPortDescription(p.number(), p.isEnabled(), p.type(), p.portSpeed());
        }
    }

    // Creates a port description from the specified connection point if none created earlier.
    private PortDescription description(ConnectPoint cp) {
        PortDescription saved = descriptions.get(cp);
        if (saved != null) {
            return saved;
        }
        Port p = deviceService.getPort(cp.deviceId(), cp.port());
        if (p == null) {
            return new DefaultPortDescription(cp.port(), true);
        }
        return description(p);
    }

    // Produces set of annotations from the given JSON node.
    private SparseAnnotations annotations(JsonNode node) {
        if (node == null) {
            return DefaultAnnotations.EMPTY;
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
        String portName = text.substring(i + 1);
        DeviceId deviceId = deviceId(text.substring(0, i));

        for (Port port : deviceService.getPorts(deviceId)) {
            PortNumber pn = port.number();
            if (pn.name().equals(portName)) {
                return new ConnectPoint(deviceId, pn);
            }
        }

        long portNum;
        try {
            portNum = Long.parseLong(portName);
        } catch (NumberFormatException e) {
            portNum = 0;
        }

        return new ConnectPoint(deviceId, portNumber(portNum, portName));
    }

    // Returns string form of the named property in the given JSON object.
    private String get(JsonNode node, String name) {
        return node.path(name).asText();
    }

    // Returns string form of the named property in the given JSON object.
    private String get(JsonNode node, String name, String defaultValue) {
        return node.path(name).asText(defaultValue);
    }

    @Override
    public void roleChanged(DeviceId device, MastershipRole newRole) {
        deviceProviderService.receivedRoleReply(device, newRole, newRole);
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {
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
        return true;
    }

    @Override
    public void enablePort(DeviceId deviceId, PortNumber portNumber) {
        //TODO
    }

    @Override
    public void disablePort(DeviceId deviceId, PortNumber portNumber) {
        //TODO
    }

    /**
     * Prepares to count device added/available/removed events.
     *
     * @param count number of events to count
     */
    protected void prepareForDeviceEvents(int count) {
        deviceLatch = new CountDownLatch(count);
        deviceService.addListener(deviceEventCounter);
    }

    /**
     * Waits for all expected device added/available/removed events.
     */
    protected void waitForDeviceEvents() {
        try {
            deviceLatch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Device events did not arrive in time");
        }
        deviceService.removeListener(deviceEventCounter);
    }

    // Counts down number of device added/available/removed events.
    private class DeviceEventCounter implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            DeviceEvent.Type type = event.type();
            if (type == DEVICE_ADDED || type == DEVICE_AVAILABILITY_CHANGED) {
                deviceLatch.countDown();
            }
        }
    }

}
