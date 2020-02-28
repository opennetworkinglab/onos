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
import org.onosproject.t3.api.RouteNib;
import org.onosproject.t3.api.TroubleshootService;
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
 * Reads network states from JSON files of onos-diagnostics
 * and sets them to corresponding Network Information Bases (NIBs).
 */
@Service
@Command(scope = "onos", name = "t3-load-file",
        description = "Command to create a snapshot (cache) of network states called Network Information Bases (NIBs) "
                + "from onos-diagnostics dump files")
public class TroubleshootLoadFileCommand extends AbstractShellCommand {

    private static final Logger log = getLogger(TroubleshootLoadFileCommand.class);

    public static final String ERROR_NULL = "Some NIBs are not ready to trace. " +
            "Make sure t3-troubleshoot-load-file is done correctly";

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
            // fills each NIB (singleton) instance with the contents of the corresponding dump file
            // the file names are defined in the onos-diagnostics script
            createFlowNib(rootDir + "flows.json");
            createGroupNib(rootDir + "groups.json");
            createLinkNib(rootDir + "links.json");
            createHostNib(rootDir + "hosts.json");
            createDeviceNib(rootDir + "ports.json");
            createDriverNib(rootDir + "device-drivers.json");
            createMastershipNib(rootDir + "masters.json");
            createEdgePortNib(rootDir + "edge-ports.json");
            createRouteNib(rootDir + "routes.json");
            createNetworkConfigNib(rootDir + "netcfg.json");
            createMulticastRouteNib(rootDir + "mcast-host-show.json");
        } catch (IOException e) {
            print("Error in creating NIB: %s", e.getMessage());
            log.error("Nib creation error", e);
            return;
        }

        TroubleshootService service = get(TroubleshootService.class);
        service.applyNibs();
        if (service.checkNibsUnavailable()) {
            print(ERROR_NULL);
            return;
        }
    }

    /**
     * Fetches multicast route-related information and creates the multicast route NIB.
     *
     * @param fileName absolute path of JSON file to read
     */
    private void createMulticastRouteNib(String fileName) throws IOException {
        InputStream stream = new FileInputStream(new File(fileName));
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
        print("the number of mcast routes: %d", mcastRouteNib.getMcastRoutes().size());

        stream.close();
    }

    /**
     * Fetches network config-related information and creates the network config NIB.
     *
     * @param fileName absolute path of JSON file to read
     */
    private void createNetworkConfigNib(String fileName) throws IOException {
        InputStream stream = new FileInputStream(new File(fileName));
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
        print("the number of network configurations: %d",
                networkConfigNib.getPortConfigMap().size() + networkConfigNib.getDeviceConfigMap().size());

        stream.close();
    }

    /**
     * Fetches route-related information and creates the route NIB.
     *
     * @param fileName absolute path of JSON file to read
     */
    private void createRouteNib(String fileName) throws IOException {
        InputStream stream = new FileInputStream(new File(fileName));
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
        print("the number of routes: %d", routeNib.getRoutes().size());

        stream.close();
    }

    /**
     * Fetches edge port-related information and creates the edge port NIB.
     *
     * @param fileName absolute path of JSON file to read
     */
    private void createEdgePortNib(String fileName) throws IOException {
        InputStream stream = new FileInputStream(new File(fileName));
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
        print("the number of edge ports: %d", edgePortNib.getEdgePorts().size());

        stream.close();
    }

    /**
     * Fetches mastership-related information and creates the mastership NIB.
     *
     * @param fileName absolute path of JSON file to read
     */
    private void createMastershipNib(String fileName) throws IOException {
        InputStream stream = new FileInputStream(new File(fileName));
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
        print("the number of device-node mappings: %d", mastershipNib.getDeviceMasterMap().size());

        stream.close();
    }

    /**
     * Fetches driver-related information and creates the driver NIB.
     *
     * @param fileName absolute path of JSON file to read
     */
    private void createDriverNib(String fileName) throws IOException {
        InputStream stream = new FileInputStream(new File(fileName));
        JsonNode jsonTree = mapper().readTree(stream);
        Map<DeviceId, String> deviceDriverMap = new HashMap<>();

        // note: the parsing structure depends on DeviceDriversCommand
        jsonTree.fields().forEachRemaining(e -> {
            deviceDriverMap.put(DeviceId.deviceId(e.getKey()), e.getValue().asText());
        });

        DriverNib driverNib = DriverNib.getInstance();
        driverNib.setDeviceDriverMap(deviceDriverMap);
        print("the number of device-driver mappings: %d", deviceDriverMap.size());

        stream.close();
    }

    /**
     * Fetches device-related information and creates the device NIB.
     *
     * @param fileName absolute path of JSON file to read
     */
    private void createDeviceNib(String fileName) throws IOException {
        InputStream stream = new FileInputStream(new File(fileName));
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
        print("the number of devices: %d", deviceNib.getDevicePortMap().size());

        stream.close();
    }

    /**
     * Fetches host-related information and creates the host NIB.
     *
     * @param fileName absolute path of JSON file to read
     */
    private void createHostNib(String fileName) throws IOException {
        InputStream stream = new FileInputStream(new File(fileName));
        JsonNode jsonTree = mapper().readTree(stream);
        Set<Host> hosts = new HashSet<>();

        // note: the parsing structure depends on HostsListCommand
        hosts.addAll(codec(Host.class).decode((ArrayNode) jsonTree, this));

        HostNib hostNib = HostNib.getInstance();
        hostNib.setHosts(hosts);
        print("the number of hosts: %d", hostNib.getHosts().size());

        stream.close();
    }

    /**
     * Fetches link-related information and creates the link NIB.
     *
     * @param fileName absolute path of JSON file to read
     */
    private void createLinkNib(String fileName) throws IOException {
        InputStream stream = new FileInputStream(new File(fileName));
        JsonNode jsonTree = mapper().readTree(stream);
        Set<Link> links = new HashSet<>();

        // note: the parsing structure depends on LinksListCommand
        links.addAll(codec(Link.class).decode((ArrayNode) jsonTree, this));

        LinkNib linkNib = LinkNib.getInstance();
        linkNib.setLinks(links);
        print("the number of links: %d", linkNib.getLinks().size());

        stream.close();
    }

    /**
     * Fetches group-related information and creates the group NIB.
     *
     * @param fileName absolute path of JSON file to read
     */
    private void createGroupNib(String fileName) throws IOException {
        InputStream stream = new FileInputStream(new File(fileName));
        JsonNode jsonTree = mapper().readTree(stream);
        Set<Group> groups = new HashSet<>();

        // note: the parsing structure depends on GroupsListCommand
        groups.addAll(codec(Group.class).decode((ArrayNode) jsonTree, this));

        GroupNib groupNib = GroupNib.getInstance();
        groupNib.setGroups(groups);
        print("the number of groups: %d", groupNib.getGroups().size());

        stream.close();
    }

    /**
     * Fetches flow-related information and creates the flow NIB.
     *
     * @param fileName absolute path of JSON file to read
     */
    private void createFlowNib(String fileName) throws IOException {
        InputStream stream = new FileInputStream(new File(fileName));
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
        print("the number of flows: %d", flowNib.getFlows().size());

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

}