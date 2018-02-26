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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.app.ApplicationService;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostAdminService;
import org.onosproject.routeservice.Route;
import org.onosproject.routeservice.RouteAdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

@Component(immediate = true)
@Service(value = ScaleTestManager.class)
public class ScaleTestManager {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApplicationService applicationService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostAdminService hostAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RouteAdminService routeAdminService;

    private final Random random = new Random(System.currentTimeMillis());

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = applicationService.getId("org.onosproject.routescale");
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    public void createFlows(int flowCount) {
        for (Device device : deviceService.getAvailableDevices()) {
            DeviceId id = device.id();
            FlowRuleOperations.Builder ops = FlowRuleOperations.builder();

            for (int i = 0; i < flowCount; i++) {
                FlowRule.Builder frb = DefaultFlowRule.builder();
                frb.fromApp(appId).makePermanent().withPriority(1000 + i);
                TrafficSelector.Builder tsb = DefaultTrafficSelector.builder();
                TrafficTreatment.Builder ttb = DefaultTrafficTreatment.builder();

                tsb.matchEthType(Ethernet.TYPE_IPV4);
                ttb.setEthDst(randomMac()).setEthSrc(randomMac());
                ttb.setOutput(PortNumber.portNumber(random.nextInt(512)));
                frb.withSelector(tsb.build()).withTreatment(ttb.build());
                ops.add(frb.forDevice(id).build());
            }

            flowRuleService.apply(ops.build());

        }
    }

    public void createRoutes(int routeCount) {
        List<Host> hosts = ImmutableList.copyOf(hostAdminService.getHosts());
        ImmutableSet.Builder<Route> routes = ImmutableSet.builder();
        for (int i = 0; i < routeCount; i++) {
            IpPrefix prefix = randomIp().toIpPrefix();
            IpAddress nextHop = randomIp(hosts);
            routes.add(new Route(Route.Source.STATIC, prefix, nextHop));
        }
        routeAdminService.update(routes.build());
    }

    private IpAddress randomIp() {
        byte[] bytes = new byte[4];
        random.nextBytes(bytes);
        return IpAddress.valueOf(IpAddress.Version.INET, bytes, 0);
    }

    private IpAddress randomIp(List<Host> hosts) {
        Host host = hosts.get(random.nextInt(hosts.size()));
        return host.ipAddresses().iterator().next();
    }

    private MacAddress randomMac() {
        byte[] bytes = new byte[6];
        random.nextBytes(bytes);
        return MacAddress.valueOf(bytes);
    }

}
