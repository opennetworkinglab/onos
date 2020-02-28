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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.mcast.api.McastRoute;
import org.onosproject.mcast.api.McastRouteData;
import org.onosproject.mcast.api.MulticastRouteService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.InterfaceConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.edge.EdgePortService;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkService;
import org.onosproject.routeservice.ResolvedRoute;
import org.onosproject.routeservice.RouteService;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * T3 CLI command to load the NIB with snapshots of the network states that are fetched from ONOS stores.
 */
@Service
@Command(scope = "onos", name = "t3-load-snapshot",
        description = "Load the NIB with the network states stored in the ONOS instance where the T3 is running")
public class TroubleshootLoadSnapshotCommand
        extends AbstractShellCommand implements NibLoader {

    @Override
    protected void doExecute() {

        print("Load current network states from ONOS stores");

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

        Lists.newArrayList(FlowNib.getInstance(), GroupNib.getInstance(), LinkNib.getInstance(),
                HostNib.getInstance(), DeviceNib.getInstance(), DriverNib.getInstance(),
                MastershipNib.getInstance(), EdgePortNib.getInstance(), RouteNib.getInstance(),
                NetworkConfigNib.getInstance(), MulticastRouteNib.getInstance())
                .forEach(nib -> {
                    // specify creation time and source which the NIB is filled with
                    nib.setProfile(new NibProfile(System.currentTimeMillis(), NibProfile.SourceType.SNAPSHOT));
                    NibProfile profile = nib.getProfile();
                    print(String.format(
                            nib.getClass().getSimpleName() + " created %s from %s",
                            profile.date(), profile.sourceType()));
                });
    }

    @Override
    public void loadFlowNib() {
        FlowRuleService flowRuleService = get(FlowRuleService.class);
        DeviceService deviceService = get(DeviceService.class);
        Set<FlowEntry> flows = new HashSet<>();

        Lists.newArrayList(deviceService.getDevices().iterator())
                .forEach(device -> flows.addAll(Lists.newArrayList(
                        flowRuleService.getFlowEntries(device.id()))));

        FlowNib flowNib = FlowNib.getInstance();
        flowNib.setFlows(flows);
    }

    @Override
    public void loadGroupNib() {
        GroupService groupService = get(GroupService.class);
        DeviceService deviceService = get(DeviceService.class);
        Set<Group> groups = new HashSet<>();

        Lists.newArrayList(deviceService.getDevices().iterator())
                .forEach(device -> groups.addAll(Lists.newArrayList(
                        groupService.getGroups(device.id()))));

        GroupNib groupNib = GroupNib.getInstance();
        groupNib.setGroups(groups);
    }

    @Override
    public void loadLinkNib() {
        LinkService linkService = get(LinkService.class);
        Set<Link> links = new HashSet<>();

        links.addAll(Lists.newArrayList(linkService.getLinks()));

        LinkNib linkNib = LinkNib.getInstance();
        linkNib.setLinks(links);
    }

    @Override
    public void loadHostNib() {
        HostService hostService = get(HostService.class);
        Set<Host> hosts = new HashSet<>();

        hosts.addAll(Lists.newArrayList(hostService.getHosts()));

        HostNib hostNib = HostNib.getInstance();
        hostNib.setHosts(hosts);
    }

    @Override
    public void loadDeviceNib() {
        DeviceService deviceService = get(DeviceService.class);
        Map<Device, Set<Port>> devicePortMap = new HashMap<>();

        Lists.newArrayList(deviceService.getDevices().iterator())
                .forEach(device -> {
                    // current DeviceNib impl. checks the availability of devices from their annotations
                    DefaultAnnotations annotations = DefaultAnnotations.builder()
                            .set("available", String.valueOf(deviceService.isAvailable(device.id()))).build();
                    DefaultDevice annotated = new DefaultDevice(device.providerId(), device.id(), device.type(),
                            device.manufacturer(), device.hwVersion(), device.swVersion(), device.serialNumber(),
                            device.chassisId(), annotations);
                    devicePortMap.put(annotated, Sets.newHashSet(deviceService.getPorts(device.id())));
                });

        DeviceNib deviceNib = DeviceNib.getInstance();
        deviceNib.setDevicePortMap(devicePortMap);
    }

    @Override
    public void loadDriverNib() {
        DriverService driverService = get(DriverService.class);
        Map<DeviceId, String> deviceDriverMap = driverService.getDeviceDrivers();

        DriverNib driverNib = DriverNib.getInstance();
        driverNib.setDeviceDriverMap(deviceDriverMap);
    }

    @Override
    public void loadMastershipNib() {
        MastershipService mastershipService = get(MastershipService.class);
        DeviceService deviceService = get(DeviceService.class);
        Map<DeviceId, NodeId> deviceMasterMap = new HashMap<>();

        Lists.newArrayList(deviceService.getDevices().iterator())
                .forEach(device -> deviceMasterMap.put(device.id(), mastershipService.getMasterFor(device.id())));

        MastershipNib mastershipNib = MastershipNib.getInstance();
        mastershipNib.setDeviceMasterMap(deviceMasterMap);
    }

    @Override
    public void loadEdgePortNib() {
        EdgePortService edgePortService = get(EdgePortService.class);
        DeviceService deviceService = get(DeviceService.class);
        Map<DeviceId, Set<ConnectPoint>> edgePorts = new HashMap<>();

        Lists.newArrayList(deviceService.getDevices().iterator())
                .forEach(device -> edgePorts.put(device.id(), Sets.newHashSet(edgePortService.getEdgePoints())));

        EdgePortNib edgePortNib = EdgePortNib.getInstance();
        edgePortNib.setEdgePorts(edgePorts);
    }

    @Override
    public void loadRouteNib() {
        RouteService routeService = get(RouteService.class);
        Set<ResolvedRoute> routes = new HashSet<>();

        Lists.newArrayList(routeService.getRouteTables())
                .forEach(routeTableId -> routes.addAll(routeService.getResolvedRoutes(routeTableId)));

        RouteNib routeNib = RouteNib.getInstance();
        routeNib.setRoutes(routes);
    }

    @Override
    public void loadNetworkConfigNib() {
        NetworkConfigService networkConfigService = get(NetworkConfigService.class);
        DeviceService deviceService = get(DeviceService.class);

        // Map of str ConnectPoint : InterfaceConfig
        Map<String, Config> portConfigMap = new HashMap<>();
        Lists.newArrayList(deviceService.getDevices().iterator())
                .forEach(device -> deviceService.getPorts(device.id())
                        .forEach(port -> {
                            ConnectPoint cp = new ConnectPoint(device.id(), port.number());
                            portConfigMap.put(cp.toString(), networkConfigService.getConfig(cp, InterfaceConfig.class));
                        }));

        // Map of str DeviceId : SegmentRoutingDeviceConfig
        Map<String, Config> deviceConfigMap = new HashMap<>();
        Lists.newArrayList(deviceService.getDevices().iterator())
                .forEach(device -> deviceConfigMap.put(device.id().toString(),
                        networkConfigService.getConfig(device.id(), SegmentRoutingDeviceConfig.class)));

        NetworkConfigNib networkConfigNib = NetworkConfigNib.getInstance();
        networkConfigNib.setPortConfigMap(portConfigMap);
        networkConfigNib.setDeviceConfigMap(deviceConfigMap);
    }

    @Override
    public void loadMulticastRouteNib() {
        MulticastRouteService mcastRouteService = get(MulticastRouteService.class);
        Map<McastRoute, McastRouteData> mcastRoutes = new HashMap<>();

        Lists.newArrayList(mcastRouteService.getRoutes())
                .forEach(mcastRoute -> mcastRoutes.put(mcastRoute, mcastRouteService.routeData(mcastRoute)));

        MulticastRouteNib mcastRouteNib = MulticastRouteNib.getInstance();
        mcastRouteNib.setMcastRoutes(mcastRoutes);
    }

}