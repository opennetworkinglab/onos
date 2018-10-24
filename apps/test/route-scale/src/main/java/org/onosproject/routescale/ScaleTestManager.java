/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.routescale;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.app.ApplicationService;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostAdminService;
import org.onosproject.net.link.LinkService;
import org.onosproject.routeservice.Route;
import org.onosproject.routeservice.RouteAdminService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;
import static org.onosproject.routescale.OsgiPropertyConstants.FLOW_COUNT;
import static org.onosproject.routescale.OsgiPropertyConstants.FLOW_COUNT_DEFAULT;
import static org.onosproject.routescale.OsgiPropertyConstants.ROUTE_COUNT;
import static org.onosproject.routescale.OsgiPropertyConstants.ROUTE_COUNT_DEFAULT;

@Component(
    immediate = true,
    service = ScaleTestManager.class,
    property = {
        FLOW_COUNT + ":Integer=" + FLOW_COUNT_DEFAULT,
        ROUTE_COUNT + ":Integer=" + ROUTE_COUNT_DEFAULT,
    }
)
public class ScaleTestManager {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ApplicationService applicationService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostAdminService hostAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected RouteAdminService routeAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService componentConfigService;

    /** Number of flows to be maintained in the system. */
    private int flowCount = FLOW_COUNT_DEFAULT;

    /** Number of routes to be maintained in the system. */
    private int routeCount = ROUTE_COUNT_DEFAULT;

    private final Random random = new Random(System.currentTimeMillis());

    private ApplicationId appId;

    private long macBase = System.currentTimeMillis();

    @Activate
    protected void activate() {
        appId = applicationService.getId("org.onosproject.routescale");
        componentConfigService.registerProperties(getClass());
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        componentConfigService.unregisterProperties(getClass(), false);
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        if (context == null) {
            return;
        }

        Dictionary<?, ?> properties = context.getProperties();
        try {
            String s = get(properties, FLOW_COUNT);
            flowCount = isNullOrEmpty(s) ? flowCount : Integer.parseInt(s.trim());

            s = get(properties, ROUTE_COUNT);
            routeCount = isNullOrEmpty(s) ? routeCount : Integer.parseInt(s.trim());

            log.info("Reconfigured; flowCount={}; routeCount={}", flowCount, routeCount);

            adjustFlows();
            adjustRoutes();

        } catch (NumberFormatException | ClassCastException e) {
            log.warn("Misconfigured", e);
        }
    }

    private void adjustFlows() {
        int deviceCount = deviceService.getAvailableDeviceCount();
        if (deviceCount == 0) {
            return;
        }

        int flowsPerDevice = flowCount / deviceCount;
        for (Device device : deviceService.getAvailableDevices()) {
            DeviceId id = device.id();
            if (deviceService.getRole(id) != MastershipRole.MASTER ||
                    flowsPerDevice == 0) {
                continue;
            }

            int currentFlowCount = flowRuleService.getFlowRuleCount(id);
            if (flowsPerDevice > currentFlowCount) {
                addMoreFlows(flowsPerDevice, device, id, currentFlowCount);

            } else if (flowsPerDevice < currentFlowCount) {
                removeExcessFlows(flowsPerDevice, id, currentFlowCount);
            }
        }
    }

    private void addMoreFlows(int flowsPerDevice, Device device, DeviceId id,
                              int currentFlowCount) {
        int c = flowsPerDevice - currentFlowCount;
        log.info("Adding {} flows for device {}", c, id);
        List<PortNumber> ports = devicePorts(device);
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        for (int i = 0; i < c; i++) {
            FlowRule.Builder frb = DefaultFlowRule.builder();
            frb.fromApp(appId).makePermanent().withPriority((currentFlowCount + i) % FlowRule.MAX_PRIORITY);
            TrafficSelector.Builder tsb = DefaultTrafficSelector.builder();
            TrafficTreatment.Builder ttb = DefaultTrafficTreatment.builder();

            tsb.matchEthType(Ethernet.TYPE_IPV4);
            tsb.matchEthDst(randomMac());
            ttb.setEthDst(randomMac()).setEthSrc(randomMac());
            ttb.setOutput(randomPort(ports));
            frb.withSelector(tsb.build()).withTreatment(ttb.build());
            ops.add(frb.forDevice(id).build());
        }
        flowRuleService.apply(ops.build());
    }

    private void removeExcessFlows(int flowsPerDevice, DeviceId id,
                                   int currentFlowCount) {
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        int c = currentFlowCount - flowsPerDevice;
        log.info("Removing {} flows from device {}", c, id);
        for (FlowEntry e : flowRuleService.getFlowEntries(id)) {
            if (Objects.equals(e.appId(), appId.id()) && c > 0) {
                ops.remove(e);
                c--;
            } else if (c == 0) {
                break;
            }
        }
        flowRuleService.apply(ops.build());
    }

    private void adjustRoutes() {
        int currentRouteCount =
                routeAdminService.getRouteTables().parallelStream()
                        .mapToInt(t -> routeAdminService.getRoutes(t).size()).sum();
        if (currentRouteCount < routeCount) {
            createRoutes(routeCount - currentRouteCount);
        } else if (currentRouteCount > routeCount) {
            removeRoutes(currentRouteCount - routeCount);
        }
    }

    // Returns a list of ports on the given device that have either links or
    // hosts connected to them.
    private List<PortNumber> devicePorts(Device device) {
        DeviceId id = device.id();
        ImmutableList.Builder<PortNumber> ports = ImmutableList.builder();
        linkService.getDeviceEgressLinks(id).forEach(l -> ports.add(l.src().port()));
        hostAdminService.getConnectedHosts(id)
                .forEach(h -> h.locations().stream()
                        .filter(l -> Objects.equals(id, l.elementId()))
                        .findFirst()
                        .ifPresent(l -> ports.add(l.port())));
        return ports.build();
    }

    // Creates the specified number of random routes. Such routes are generated
    // using random IP prefices with next hop being an IP address of a randomly
    // chosen hosts.
    private void createRoutes(int routeCount) {
        List<Host> hosts = ImmutableList.copyOf(hostAdminService.getHosts());
        ImmutableSet.Builder<Route> routes = ImmutableSet.builder();
        for (int i = 0; i < routeCount; i++) {
            IpPrefix prefix = randomIp().toIpPrefix();
            IpAddress nextHop = randomIp(hosts);
            routes.add(new Route(Route.Source.STATIC, prefix, nextHop));
        }
        routeAdminService.update(routes.build());
    }

    // Removes the specified number of routes chosen at random.
    private void removeRoutes(int routeCount) {
        log.warn("Not implemented yet");
    }

    // Generates a random IP address.
    private IpAddress randomIp() {
        byte[] bytes = new byte[4];
        random.nextBytes(bytes);
        return IpAddress.valueOf(IpAddress.Version.INET, bytes, 0);
    }

    // Generates a random MAC address.
    private MacAddress randomMac() {
        return MacAddress.valueOf(macBase++);
    }

    // Returns IP address of a host randomly chosen from the specified list.
    private IpAddress randomIp(List<Host> hosts) {
        Host host = hosts.get(random.nextInt(hosts.size()));
        return host.ipAddresses().iterator().next();
    }

    // Returns port number randomly chosen from the given list of port numbers.
    private PortNumber randomPort(List<PortNumber> ports) {
        return ports.get(random.nextInt(ports.size()));
    }

}
