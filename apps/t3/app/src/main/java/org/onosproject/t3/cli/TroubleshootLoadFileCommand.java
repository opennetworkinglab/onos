/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.t3.cli;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.PlaceholderCompleter;
import org.onosproject.cluster.NodeId;
import org.onosproject.mcast.api.McastRoute;
import org.onosproject.mcast.api.McastRouteData;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.basics.InterfaceConfig;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.group.Group;
import org.onosproject.routeservice.ResolvedRoute;
import org.onosproject.routeservice.Route;
import org.onosproject.segmentrouting.config.SegmentRoutingDeviceConfig;
import org.onosproject.t3.api.DeviceNib;
import org.onosproject.t3.api.DriverNib;
import org.onosproject.t3.api.EdgePortNib;
import org.onosproject.t3.api.FlowNib;
import org.onosproject.t3.api.GroupNib;
import org.onosproject.t3.api.HostNib;
import org.onosproject.t3.api.LinkNib;
import org.onosproject.t3.api.MastershipNib;
import org.onosproject.t3.api.MulticastRouteNib;
import org.onosproject.t3.api.NetworkConfigNib;
import org.onosproject.t3.api.NibProfile;
import org.onosproject.t3.api.RouteNib;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * T3 CLI command to load the NIB with dump files that represent snapshots of the network states.
 */
@Service
@Command(scope = "onos", name = "t3-load-file",
        description = "Load the NIB with onos-diagnostics dump files")
public class TroubleshootLoadFileCommand
        extends AbstractShellCommand implements NibLoader {

    private static final Logger log = getLogger(TroubleshootLoadFileCommand.class);

    @Argument(index = 0, name = "rootDir", description = "Specify the location of the directory " +
            "where the dump files of a given instance have been extracted (e.g. /tmp/onos-diags/127.0.0.1)",
            required = true, multiValued = false)
    @Completion(PlaceholderCompleter.class)
    String rootDir;

    @Override
    protected void doExecute() {

        if (!rootDir.endsWith("/")) {
            rootDir = rootDir + "/";
        }
        print("Load target files in: %s", rootDir);

        try {
            // names of files to read are defined in the onos-diagnostics script
            loadFlowNib();
            loadGroupNib();
            loadLinkNib();
            loadHostNib();
            loadDeviceNib();
            loadDriverNib();
            loadMastershipNib();
            loadEdgePortNib();
            loadRouteNib();
            loadNetworkConfigNib();
            loadMulticastRouteNib();

        } catch (IOException e) {
            print("Error in creating NIB: %s", e.getMessage());
            log.error("Nib creation error", e);
            return;
        }

        // ensured no errors in file loading. so make them available officially
        Lists.newArrayList(FlowNib.getInstance(), GroupNib.getInstance(), LinkNib.getInstance(),
                HostNib.getInstance(), DeviceNib.getInstance(), DriverNib.getInstance(),
                MastershipNib.getInstance(), EdgePortNib.getInstance(), RouteNib.getInstance(),
                NetworkConfigNib.getInstance(), MulticastRouteNib.getInstance())
                .forEach(nib -> {
                    // specify creation time and source which the NIB is filled with
                    nib.setProfile(new NibProfile(System.currentTimeMillis(), NibProfile.SourceType.FILE));
                    NibProfile profile = nib.getProfile();
                    print(String.format(
                            nib.getClass().getSimpleName() + " created %s from %s",
                            profile.date(), profile.sourceType()));
                });
    }

    @Override
    public void loadFlowNib() throws IOException {
        InputStream stream = new FileInputStream(new File(rootDir + "flows.json"));
        JsonNode jsonTree = mapper().readTree(stream);
        Set<FlowEntry> flows = new HashSet<>();

        List<ObjectNode> flowNodeList = new ArrayList<>();
        jsonTree.forEach(jsonNode -> {
            ArrayNode flowArrayNode = (ArrayNode) jsonNode.get("flows");
            Lists.newArrayList(flowArrayNode.iterator())
                    .forEach(flowNode -> flowNodeList.add((ObjectNode) flowNode));
        });

        // TODO: future plan for the new APIs of the flow rule service that returns raw flows or normalized flows
        flowNodeList.forEach(flowNode -> {
            FlowEntry flow;
            try {
                flow = codec(FlowEntry.class).decode(flowNode, this);
            } catch (IllegalArgumentException e) {
                log.warn("T3 in offline mode ignores reading extension fields of this flow to avoid decoding error");
                ObjectNode extensionRemoved = removeExtension(flowNode);
                flow = codec(FlowEntry.class).decode(extensionRemoved, this);
            }
            flows.add(flow);
        });

        FlowNib flowNib = FlowNib.getInstance();
        flowNib.setFlows(flows);

        stream.close();
    }

    /**
     * Remove JSON nodes for extension instructions of a flow.
     * This effectively allows T3 in offline mode to ignore extension fields of flows to avoid "device not found" error.
     * See decodeExtension() in {@link org.onosproject.codec.impl.DecodeInstructionCodecHelper}.
     *
     * @param flowNode  the json node representing a flow
     * @return          json node with removed extensions
     */
    private ObjectNode removeExtension(ObjectNode flowNode) {

        // TODO: decoding extension instructions of offline (dumped) flows is not supported by T3 for now
        ArrayNode extensionRemoved = mapper().createArrayNode();
        ArrayNode instructionArrayNode = (ArrayNode) flowNode.get("treatment").get("instructions");
        instructionArrayNode.forEach(instrNode -> {
            String instrType = instrNode.get("type").asText();
            if (!instrType.equals(Instruction.Type.EXTENSION.name())) {
                extensionRemoved.add(instrNode);
            }
        });
        ((ObjectNode) flowNode.get("treatment")).replace("instructions", extensionRemoved);

        return flowNode;
    }

    @Override
    public void loadGroupNib() throws IOException {
        InputStream stream = new FileInputStream(new File(rootDir + "groups.json"));
        JsonNode jsonTree = mapper().readTree(stream);
        Set<Group> groups = new HashSet<>();

        // note: the parsing structure depends on GroupsListCommand
        groups.addAll(codec(Group.class).decode((ArrayNode) jsonTree, this));

        GroupNib groupNib = GroupNib.getInstance();
        groupNib.setGroups(groups);

        stream.close();
    }

    @Override
    public void loadLinkNib() throws IOException {
        InputStream stream = new FileInputStream(new File(rootDir + "links.json"));
        JsonNode jsonTree = mapper().readTree(stream);
        Set<Link> links = new HashSet<>();

        // note: the parsing structure depends on LinksListCommand
        links.addAll(codec(Link.class).decode((ArrayNode) jsonTree, this));

        LinkNib linkNib = LinkNib.getInstance();
        linkNib.setLinks(links);

        stream.close();
    }

    @Override
    public void loadHostNib() throws IOException {
        InputStream stream = new FileInputStream(new File(rootDir + "hosts.json"));
        JsonNode jsonTree = mapper().readTree(stream);
        Set<Host> hosts = new HashSet<>();

        // note: the parsing structure depends on HostsListCommand
        hosts.addAll(codec(Host.class).decode((ArrayNode) jsonTree, this));

        HostNib hostNib = HostNib.getInstance();
        hostNib.setHosts(hosts);

        stream.close();
    }

    @Override
    public void loadDeviceNib() throws IOException {
        InputStream stream = new FileInputStream(new File(rootDir + "ports.json"));
        JsonNode jsonTree = mapper().readTree(stream);
        Map<Device, Set<Port>> devicePortMap = new HashMap<>();

        // note: the parsing structure depends on DevicePortsListCommand
        jsonTree.forEach(jsonNode -> {
            Device device = codec(Device.class).decode(
                    (ObjectNode) jsonNode.get("device"), this);
            Set<Port> ports = new HashSet<>(codec(Port.class).decode(
                    (ArrayNode) jsonNode.get("ports"), this));
            devicePortMap.put(device, ports);
        });

        DeviceNib deviceNib = DeviceNib.getInstance();
        deviceNib.setDevicePortMap(devicePortMap);

        stream.close();
    }

    @Override
    public void loadDriverNib() throws IOException {
        InputStream stream = new FileInputStream(new File(rootDir + "device-drivers.json"));
        JsonNode jsonTree = mapper().readTree(stream);
        Map<DeviceId, String> deviceDriverMap = new HashMap<>();

        // note: the parsing structure depends on DeviceDriversCommand
        jsonTree.fields().forEachRemaining(e -> {
            deviceDriverMap.put(DeviceId.deviceId(e.getKey()), e.getValue().asText());
        });

        DriverNib driverNib = DriverNib.getInstance();
        driverNib.setDeviceDriverMap(deviceDriverMap);

        stream.close();
    }

    @Override
    public void loadMastershipNib() throws IOException {
        InputStream stream = new FileInputStream(new File(rootDir + "masters.json"));
        JsonNode jsonTree = mapper().readTree(stream);
        Map<DeviceId, NodeId> deviceMasterMap = new HashMap<>();

        // note: the parsing structure depends on MastersListCommand
        jsonTree.forEach(jsonNode -> {
            ArrayNode devicesNode = ((ArrayNode) jsonNode.get("devices"));
            devicesNode.forEach(deviceNode -> {
                // a device is connected to only one master node at a time
                deviceMasterMap.put(
                        DeviceId.deviceId(deviceNode.asText()),
                        NodeId.nodeId(jsonNode.get("id").asText()));
            });
        });

        MastershipNib mastershipNib = MastershipNib.getInstance();
        mastershipNib.setDeviceMasterMap(deviceMasterMap);

        stream.close();
    }

    @Override
    public void loadEdgePortNib() throws IOException {
        InputStream stream = new FileInputStream(new File(rootDir + "edge-ports.json"));
        JsonNode jsonTree = mapper().readTree(stream);
        Map<DeviceId, Set<ConnectPoint>> edgePorts = new HashMap<>();

        // note: the parsing structure depends on EdgePortsListCommand
        jsonTree.forEach(jsonNode -> {
            DeviceId deviceId = DeviceId.deviceId(jsonNode.fieldNames().next());
            PortNumber portNumber = PortNumber.portNumber(
                    jsonNode.get(deviceId.toString()).asText());
            if (!edgePorts.containsKey(deviceId)) {
                edgePorts.put(deviceId, new HashSet<>());
            }
            edgePorts.get(deviceId).add(new ConnectPoint(deviceId, portNumber));
        });

        EdgePortNib edgePortNib = EdgePortNib.getInstance();
        edgePortNib.setEdgePorts(edgePorts);

        stream.close();
    }

    @Override
    public void loadRouteNib() throws IOException {
        InputStream stream = new FileInputStream(new File(rootDir + "routes.json"));
        JsonNode jsonTree = mapper().readTree(stream);
        Set<ResolvedRoute> routes = new HashSet<>();

        // note: the parsing structure depends on RoutesListCommand
        jsonTree.fields().forEachRemaining(e -> {
            ArrayNode routesNode = (ArrayNode) e.getValue();
            routesNode.forEach(routeNode -> {
                Route route = codec(Route.class).decode((ObjectNode) routeNode, this);
                // parse optional fields needed for ResolvedRoute
                MacAddress nextHopMac = (null == routeNode.get("nextHopMac")) ?
                        null : MacAddress.valueOf(routeNode.get("nextHopMac").asText());
                VlanId nextHopVlan = (null == routeNode.get("nextHopVlan")) ?
                        null : VlanId.vlanId(routeNode.get("nextHopVlan").asText());
                routes.add(new ResolvedRoute(route, nextHopMac, nextHopVlan));
            });
        });

        RouteNib routeNib = RouteNib.getInstance();
        routeNib.setRoutes(routes);

        stream.close();
    }

    @Override
    public void loadNetworkConfigNib() throws IOException {
        InputStream stream = new FileInputStream(new File(rootDir + "netcfg.json"));
        JsonNode jsonTree = mapper().readTree(stream);
        Map<String, Config> portConfigMap = new HashMap<>();
        Map<String, Config> deviceConfigMap = new HashMap<>();

        // note: the parsing structure depends on NetworkConfigCommand
        // TODO: improve the code quality by referring to target json
        jsonTree.fields().forEachRemaining(e -> {
            if (e.getKey().equals("ports")) {
                JsonNode portConfigsNode = e.getValue();
                portConfigsNode.fields().forEachRemaining(portConfigEntry -> {
                    String key = portConfigEntry.getKey();
                    InterfaceConfig config = new InterfaceConfig();
                    config.init(ConnectPoint.fromString(key), "interfaces",
                            portConfigEntry.getValue().get("interfaces"), mapper(), null);
                    portConfigMap.put(key, config);
                });
            } else if (e.getKey().equals("devices")) {
                JsonNode deviceConfigsNode = e.getValue();
                deviceConfigsNode.fields().forEachRemaining(deviceConfigEntry -> {
                    String key = deviceConfigEntry.getKey();
                    SegmentRoutingDeviceConfig config = new SegmentRoutingDeviceConfig();
                    config.init(DeviceId.deviceId(key), "segmentrouting",
                            deviceConfigEntry.getValue().get("segmentrouting"), mapper(), null);
                    deviceConfigMap.put(key, config);
                });
            } else {
                log.warn("Given configuration subject {} is not supported", e.getKey());
            }
        });

        NetworkConfigNib networkConfigNib = NetworkConfigNib.getInstance();
        networkConfigNib.setPortConfigMap(portConfigMap);
        networkConfigNib.setDeviceConfigMap(deviceConfigMap);

        stream.close();
    }

    @Override
    public void loadMulticastRouteNib() throws IOException {
        InputStream stream = new FileInputStream(new File(rootDir + "mcast-host-show.json"));
        JsonNode jsonTree = mapper().readTree(stream);
        Map<McastRoute, McastRouteData> mcastRoutes = new HashMap<>();

        // note: the parsing structure depends on McastShowHostCommand
        jsonTree.forEach(mcastRouteNode -> {
            // use McastHostRouteCodec to decode McastRoute
            McastRoute mcastRoute = codec(McastRoute.class)
                    .decode((ObjectNode) mcastRouteNode, this);
            // create McastRouteData that stores sources and sinks of McastRoute
            McastRouteData mcastRouteData = McastRouteData.empty();
            if (mcastRouteNode.get("sources") != null) {
                JsonNode sourcesNode = mcastRouteNode.get("sources");
                sourcesNode.fields().forEachRemaining(sourceEntry -> {
                    HostId hostId = HostId.hostId(sourceEntry.getKey());
                    Set<ConnectPoint> sources = mapper().convertValue(
                            sourceEntry.getValue(), new TypeReference<Set<ConnectPoint>>() { });
                    mcastRouteData.addSources(hostId, sources);
                });
            }
            if (mcastRouteNode.get("sinks") != null) {
                JsonNode sinksNode = mcastRouteNode.get("sinks");
                sinksNode.fields().forEachRemaining(sinkEntry -> {
                    HostId hostId = HostId.hostId(sinkEntry.getKey());
                    Set<ConnectPoint> sinks = mapper().convertValue(
                            sinkEntry.getValue(), new TypeReference<Set<ConnectPoint>>() { });
                    mcastRouteData.addSinks(hostId, sinks);
                });
            }
            mcastRoutes.put(mcastRoute, mcastRouteData);
        });

        MulticastRouteNib mcastRouteNib = MulticastRouteNib.getInstance();
        mcastRouteNib.setMcastRoutes(mcastRoutes);

        stream.close();
    }

}