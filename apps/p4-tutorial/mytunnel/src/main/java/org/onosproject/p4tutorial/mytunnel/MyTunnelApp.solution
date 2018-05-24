/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.p4tutorial.mytunnel;

import com.google.common.collect.Lists;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionParamId;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * MyTunnel application which provides forwarding between each pair of hosts via
 * MyTunnel protocol as defined in mytunnel.p4.
 * <p>
 * The app works by listening for host events. Each time a new host is
 * discovered, it provisions a tunnel between that host and all the others.
 */
@Component(immediate = true)
public class MyTunnelApp {

    private static final String APP_NAME = "org.onosproject.p4tutorial.mytunnel";

    // Default priority used for flow rules installed by this app.
    private static final int FLOW_RULE_PRIORITY = 100;

    private final HostListener hostListener = new InternalHostListener();
    private ApplicationId appId;
    private AtomicInteger nextTunnelId = new AtomicInteger();

    private static final Logger log = getLogger(MyTunnelApp.class);

    //--------------------------------------------------------------------------
    // ONOS services needed by this application.
    //--------------------------------------------------------------------------

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private HostService hostService;

    //--------------------------------------------------------------------------
    //--------------------------------------------------------------------------

    @Activate
    public void activate() {
        // Register app and event listeners.
        log.info("Starting...");
        appId = coreService.registerApplication(APP_NAME);
        hostService.addListener(hostListener);
        log.info("STARTED", appId.id());
    }

    @Deactivate
    public void deactivate() {
        // Remove listeners and clean-up flow rules.
        log.info("Stopping...");
        hostService.removeListener(hostListener);
        flowRuleService.removeFlowRulesById(appId);
        log.info("STOPPED");
    }

    /**
     * Provisions a tunnel between the given source and destination host with
     * the given tunnel ID. The tunnel is established using a randomly picked
     * shortest path based on the given topology snapshot.
     *
     * @param tunId   tunnel ID
     * @param srcHost tunnel source host
     * @param dstHost tunnel destination host
     * @param topo    topology snapshot
     */
    private void provisionTunnel(int tunId, Host srcHost, Host dstHost, Topology topo) {

        // Get all shortest paths between switches connected to source and
        // destination hosts.
        DeviceId srcSwitch = srcHost.location().deviceId();
        DeviceId dstSwitch = dstHost.location().deviceId();

        List<Link> pathLinks;
        if (srcSwitch.equals(dstSwitch)) {
            // Source and dest hosts are connected to the same switch.
            pathLinks = Collections.emptyList();
        } else {
            // Compute shortest path.
            Set<Path> allPaths = topologyService.getPaths(topo, srcSwitch, dstSwitch);
            if (allPaths.size() == 0) {
                log.warn("No paths between {} and {}", srcHost.id(), dstHost.id());
                return;
            }
            // If many shortest paths are available, pick a random one.
            pathLinks = pickRandomPath(allPaths).links();
        }

        // Tunnel ingress rules.
        for (IpAddress dstIpAddr : dstHost.ipAddresses()) {
            // In ONOS discovered hosts can have multiple IP addresses.
            // Insert tunnel ingress rule for each IP address.
            // Next switches will forward based only on tunnel ID.
            insertTunnelIngressRule(srcSwitch, dstIpAddr, tunId);
        }

        // Insert tunnel transit rules on all switches in the path, excluded the
        // last one.
        for (Link link : pathLinks) {
            DeviceId sw = link.src().deviceId();
            PortNumber port = link.src().port();
            insertTunnelForwardRule(sw, port, tunId, false);
        }

        // Tunnel egress rule.
        PortNumber egressSwitchPort = dstHost.location().port();
        insertTunnelForwardRule(dstSwitch, egressSwitchPort, tunId, true);

        log.info("** Completed provisioning of tunnel {} (srcHost={} dstHost={})",
                 tunId, srcHost.id(), dstHost.id());
    }

    /**
     * Generates and insert a flow rule to perform the tunnel INGRESS function
     * for the given switch, destination IP address and tunnel ID.
     *
     * @param switchId  switch ID
     * @param dstIpAddr IP address to forward inside the tunnel
     * @param tunId     tunnel ID
     */
    private void insertTunnelIngressRule(DeviceId switchId,
                                         IpAddress dstIpAddr,
                                         int tunId) {


        PiTableId tunnelIngressTableId = PiTableId.of("c_ingress.t_tunnel_ingress");

        // Longest prefix match on IPv4 dest address.
        PiMatchFieldId ipDestMatchFieldId = PiMatchFieldId.of("hdr.ipv4.dst_addr");
        PiCriterion match = PiCriterion.builder()
                .matchLpm(ipDestMatchFieldId, dstIpAddr.toOctets(), 32)
                .build();

        PiActionParam tunIdParam = new PiActionParam(PiActionParamId.of("tun_id"), tunId);

        PiActionId ingressActionId = PiActionId.of("c_ingress.my_tunnel_ingress");
        PiAction action = PiAction.builder()
                .withId(ingressActionId)
                .withParameter(tunIdParam)
                .build();

        log.info("Inserting INGRESS rule on switch {}: table={}, match={}, action={}",
                 switchId, tunnelIngressTableId, match, action);

        insertPiFlowRule(switchId, tunnelIngressTableId, match, action);
    }

    /**
     * Generates and insert a flow rule to perform the tunnel FORWARD/EGRESS
     * function for the given switch, output port address and tunnel ID.
     *
     * @param switchId switch ID
     * @param outPort  output port where to forward tunneled packets
     * @param tunId    tunnel ID
     * @param isEgress if true, perform tunnel egress action, otherwise forward
     *                 packet as is to port
     */
    private void insertTunnelForwardRule(DeviceId switchId,
                                         PortNumber outPort,
                                         int tunId,
                                         boolean isEgress) {

        PiTableId tunnelForwardTableId = PiTableId.of("c_ingress.t_tunnel_fwd");

        // Exact match on tun_id
        PiMatchFieldId tunIdMatchFieldId = PiMatchFieldId.of("hdr.my_tunnel.tun_id");
        PiCriterion match = PiCriterion.builder()
                .matchExact(tunIdMatchFieldId, tunId)
                .build();

        // Action depend on isEgress parameter.
        // if true, perform tunnel egress action on the given outPort, otherwise
        // simply forward packet as is (set_out_port action).
        PiActionParamId portParamId = PiActionParamId.of("port");
        PiActionParam portParam = new PiActionParam(portParamId, (short) outPort.toLong());

        final PiAction action;
        if (isEgress) {
            // Tunnel egress action.
            // Remove MyTunnel header and forward to outPort.
            PiActionId egressActionId = PiActionId.of("c_ingress.my_tunnel_egress");
            action = PiAction.builder()
                    .withId(egressActionId)
                    .withParameter(portParam)
                    .build();
        } else {
            // Tunnel transit action.
            // Forward the packet as is to outPort.
            PiActionId egressActionId = PiActionId.of("c_ingress.set_out_port");
            action = PiAction.builder()
                    .withId(egressActionId)
                    .withParameter(portParam)
                    .build();
        }

        log.info("Inserting {} rule on switch {}: table={}, match={}, action={}",
                 isEgress ? "EGRESS" : "TRANSIT",
                 switchId, tunnelForwardTableId, match, action);

        insertPiFlowRule(switchId, tunnelForwardTableId, match, action);
    }

    /**
     * Inserts a flow rule in the system that using a PI criterion and action.
     *
     * @param switchId    switch ID
     * @param tableId     table ID
     * @param piCriterion PI criterion
     * @param piAction    PI action
     */
    private void insertPiFlowRule(DeviceId switchId, PiTableId tableId,
                                  PiCriterion piCriterion, PiAction piAction) {
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(switchId)
                .forTable(tableId)
                .fromApp(appId)
                .withPriority(FLOW_RULE_PRIORITY)
                .makePermanent()
                .withSelector(DefaultTrafficSelector.builder()
                                      .matchPi(piCriterion).build())
                .withTreatment(DefaultTrafficTreatment.builder()
                                       .piTableAction(piAction).build())
                .build();
        flowRuleService.applyFlowRules(rule);
    }

    private int getNewTunnelId() {
        return nextTunnelId.incrementAndGet();
    }

    private Path pickRandomPath(Set<Path> paths) {
        int item = new Random().nextInt(paths.size());
        List<Path> pathList = Lists.newArrayList(paths);
        return pathList.get(item);
    }

    /**
     * A listener of host events that provisions two tunnels for each pair of
     * hosts when a new host is discovered.
     */
    private class InternalHostListener implements HostListener {

        @Override
        public void event(HostEvent event) {
            if (event.type() != HostEvent.Type.HOST_ADDED) {
                // Ignore other host events.
                return;
            }
            synchronized (this) {
                // Synchronizing here is an overkill, but safer for demo purposes.
                Host host = event.subject();
                Topology topo = topologyService.currentTopology();
                for (Host otherHost : hostService.getHosts()) {
                    if (!host.equals(otherHost)) {
                        provisionTunnel(getNewTunnelId(), host, otherHost, topo);
                        provisionTunnel(getNewTunnelId(), otherHost, host, topo);
                    }
                }
            }
        }
    }
}
