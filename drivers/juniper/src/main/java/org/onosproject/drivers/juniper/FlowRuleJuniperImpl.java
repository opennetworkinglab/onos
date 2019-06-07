/*
 * Copyright 2016 Open Networking Foundation
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

package org.onosproject.drivers.juniper;

import com.google.common.annotations.Beta;
import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.onlab.packet.Ip4Address;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.link.LinkService;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.juniper.JuniperUtils.OperationType;
import static org.onosproject.drivers.juniper.JuniperUtils.OperationType.ADD;
import static org.onosproject.drivers.juniper.JuniperUtils.OperationType.REMOVE;
import static org.onosproject.drivers.juniper.JuniperUtils.commitBuilder;
import static org.onosproject.drivers.juniper.JuniperUtils.rollbackBuilder;
import static org.onosproject.drivers.juniper.JuniperUtils.routeAddBuilder;
import static org.onosproject.drivers.juniper.JuniperUtils.routeDeleteBuilder;
import static org.onosproject.drivers.utilities.XmlConfigParser.loadXmlString;
import static org.onosproject.net.flow.FlowEntry.FlowEntryState.PENDING_REMOVE;
import static org.onosproject.net.flow.FlowEntry.FlowEntryState.REMOVED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Conversion of FlowRules into static routes and retrieve of installed
 * static routes as FlowRules.
 * The selector of the FlowRule must contains the IPv4 address
 * {@link org.onosproject.net.flow.TrafficSelector.Builder#matchIPDst(org.onlab.packet.IpPrefix)}
 * of the host to connect and the treatment must include an
 * output port {@link org.onosproject.net.flow.TrafficTreatment.Builder#setOutput(PortNumber)}
 * All other instructions in the selector and treatment are ignored.
 * <p>
 * This implementation requires an IP adjacency
 * (e.g., IP link discovered by {@link LinkDiscoveryJuniperImpl}) between the routers
 * to find the next hop IP address.
 */
@Beta
public class FlowRuleJuniperImpl extends JuniperAbstractHandlerBehaviour
        implements FlowRuleProgrammable {

    private static final String OK = "<ok/>";
    private final org.slf4j.Logger log = getLogger(getClass());

    @Override
    public Collection<FlowEntry> getFlowEntries() {

        DeviceId devId = checkNotNull(this.data().deviceId());
        NetconfSession session = lookupNetconfSession(devId);
        if (session == null) {
            return Collections.emptyList();
        }

        //Installed static routes
        String reply;
        try {
            reply = session.get(routingTableBuilder());
        } catch (NetconfException e) {
            throw new IllegalStateException(new NetconfException("Failed to retrieve configuration.",
                    e));
        }
        Collection<StaticRoute> devicesStaticRoutes =
                JuniperUtils.parseRoutingTable(loadXmlString(reply));

        //Expected FlowEntries installed
        FlowRuleService flowRuleService = this.handler().get(FlowRuleService.class);
        Iterable<FlowEntry> flowEntries = flowRuleService.getFlowEntries(devId);

        Collection<FlowEntry> installedRules = new HashSet<>();
        flowEntries.forEach(flowEntry -> {
            Optional<IPCriterion> ipCriterion = getIpCriterion(flowEntry);
            if (!ipCriterion.isPresent()) {
                return;
            }

            Optional<OutputInstruction> output = getOutput(flowEntry);
            if (!output.isPresent()) {
                return;
            }
            //convert FlowRule into static route
            getStaticRoute(devId, ipCriterion.get(), output.get(), flowEntry.priority()).ifPresent(staticRoute -> {
                //Two type of FlowRules:
                //1. FlowRules to forward to a remote subnet: they are translated into static route
                // configuration. So a removal request will be processed.
                //2. FlowRules to forward on a subnet directly attached to the router (Generally speaking called local):
                // those routes do not require any configuration because the router is already able to forward on
                // directly attached subnet. In this case, when the driver receive the request to remove,
                // it will report as removed.

                if (staticRoute.isLocalRoute()) {
                    //if the FlowRule is in PENDING_REMOVE or REMOVED, it is not reported.
                    if (flowEntry.state() == PENDING_REMOVE || flowEntry.state() == REMOVED) {
                        devicesStaticRoutes.remove(staticRoute);
                    } else {
                        //FlowRule is reported installed
                        installedRules.add(flowEntry);
                        devicesStaticRoutes.remove(staticRoute);
                    }

                } else {
                    //if the route is found in the device, the FlowRule is reported installed.
                    if (devicesStaticRoutes.contains(staticRoute)) {
                        installedRules.add(flowEntry);
                        devicesStaticRoutes.remove(staticRoute);
                    }
                }
            });
        });

        if (!devicesStaticRoutes.isEmpty()) {
            log.info("Found static routes on device {} not installed by ONOS: {}",
                    devId, devicesStaticRoutes);
//            FIXME: enable configuration to purge already installed flows.
//            It cannot be allowed by default because it may remove needed management routes
//            log.warn("Removing from device {} the FlowEntries not expected {}", deviceId, devicesStaticRoutes);
//            devicesStaticRoutes.forEach(staticRoute -> editRoute(session, REMOVE, staticRoute));
        }
        return installedRules;
    }

    @Override
    public Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules) {
        return manageRules(rules, ADD);
    }

    @Override
    public Collection<FlowRule> removeFlowRules(Collection<FlowRule> rules) {
        return manageRules(rules, REMOVE);
    }

    private Collection<FlowRule> manageRules(Collection<FlowRule> rules, OperationType type) {

        DeviceId deviceId = this.data().deviceId();

        log.debug("{} flow entries to NETCONF device {}", type, deviceId);
        NetconfSession session = lookupNetconfSession(deviceId);
        Collection<FlowRule> managedRules = new HashSet<>();

        for (FlowRule flowRule : rules) {

            Optional<IPCriterion> ipCriterion = getIpCriterion(flowRule);
            if (!ipCriterion.isPresent()) {
                log.error("Currently not supported: IPv4 destination match must be used");
                continue;
            }

            Optional<OutputInstruction> output = getOutput(flowRule);
            if (!output.isPresent()) {
                log.error("Currently not supported: the output action is needed");
                continue;
            }

            getStaticRoute(deviceId, ipCriterion.get(), output.get(), flowRule.priority()).ifPresent(
                    staticRoute -> {
                        //If the static route is not local, the driver tries to install
                        if (!staticRoute.isLocalRoute()) {
                            //Only if the installation is successful, the driver report the
                            // FlowRule as installed.
                            if (editRoute(session, type, staticRoute)) {
                                managedRules.add(flowRule);
                            }
                            //If static route are local, they are not installed
                            // because are not required. Directly connected routes
                            //are automatically forwarded.
                        } else {
                            managedRules.add(flowRule);
                        }
                    }
            );
        }
        return rules;
    }

    private boolean editRoute(NetconfSession session, OperationType type,
                              StaticRoute staticRoute) {
        try {
            boolean reply = false;
            if (type == ADD) {
                reply = session
                        .editConfig(DatastoreId.CANDIDATE, "merge",
                                routeAddBuilder(staticRoute));
            } else if (type == REMOVE) {
                reply = session
                        .editConfig(DatastoreId.CANDIDATE, "none", routeDeleteBuilder(staticRoute));
            }
            if (reply && commit()) {
                return true;
            } else {
                if (!rollback()) {
                    log.error("Something went wrong in the configuration and impossible to rollback");
                } else {
                    log.error("Something went wrong in the configuration: a static route has not been {} {}",
                            type == ADD ? "added" : "removed", staticRoute);
                }
            }
        } catch (NetconfException e) {
            throw new IllegalStateException(new NetconfException("Failed to retrieve configuration.",
                    e));
        }
        return false;
    }

    /**
     * Helper method to convert FlowRule into an abstraction of static route
     * {@link StaticRoute}.
     *
     * @param devId    the device id
     * @param criteria the IP destination criteria
     * @param output   the output instruction
     * @return optional of Static Route
     */
    private Optional<StaticRoute> getStaticRoute(DeviceId devId,
                                                 IPCriterion criteria,
                                                 OutputInstruction output,
                                                 int priority) {

        DeviceService deviceService = this.handler().get(DeviceService.class);
        Collection<Port> ports = deviceService.getPorts(devId);
        Optional<Port> port = ports.stream().filter(x -> x.number().equals(output.port())).findAny();
        if (!port.isPresent()) {
            log.error("The port {} does not exist in the device",
                    output.port());
            return Optional.empty();
        }

        //Find if the route refers to a local interface.
        Optional<Port> local = deviceService.getPorts(devId).stream().filter(this::isIp)
                .filter(p -> criteria.ip().getIp4Prefix().contains(
                        Ip4Address.valueOf(p.annotations().value(JuniperUtils.AK_IP)))).findAny();

        if (local.isPresent()) {
            return Optional.of(new StaticRoute(criteria.ip().getIp4Prefix(),
                    criteria.ip().getIp4Prefix().address(), true, priority));
        }

        Optional<Ip4Address> nextHop = findIpDst(devId, port.get());
        if (nextHop.isPresent()) {
            return Optional.of(
                    new StaticRoute(criteria.ip().getIp4Prefix(), nextHop.get(), false, priority));
        } else {
            log.error("The destination interface has not an IP {}", port.get());
            return Optional.empty();
        }

    }

    /**
     * Helper method to get the IP destination criterion given a flow rule.
     *
     * @param flowRule the flow rule
     * @return optional of IP destination criterion
     */
    private Optional<IPCriterion> getIpCriterion(FlowRule flowRule) {

        Criterion ip = flowRule.selector().getCriterion(Criterion.Type.IPV4_DST);
        return Optional.ofNullable((IPCriterion) ip);
    }

    /**
     * Helper method to get the output instruction given a flow rule.
     *
     * @param flowRule the flow rule
     * @return the output instruction
     */
    private Optional<OutputInstruction> getOutput(FlowRule flowRule) {
        return flowRule
                .treatment().allInstructions().stream()
                .filter(instruction -> instruction
                        .type() == Instruction.Type.OUTPUT)
                .map(OutputInstruction.class::cast).findFirst();
    }

    private String routingTableBuilder() {
        StringBuilder rpc = new StringBuilder("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">");
        rpc.append("<get-route-information/>");
        rpc.append("</rpc>");
        return rpc.toString();
    }

    private boolean commit() {
        NetconfSession session = lookupNetconfSession(handler().data().deviceId());

        String replay;
        try {
            replay = session.get(commitBuilder());
        } catch (NetconfException e) {
            throw new IllegalStateException(new NetconfException("Failed to retrieve configuration.",
                    e));
        }

        return replay != null && replay.contains(OK);
    }

    private boolean rollback() {
        NetconfSession session = lookupNetconfSession(handler().data().deviceId());

        String replay;
        try {
            replay = session.get(rollbackBuilder(0));
        } catch (NetconfException e) {
            throw new IllegalStateException(new NetconfException("Failed to retrieve configuration.",
                    e));
        }

        return replay != null && replay.contains(OK);
    }

    /**
     * Helper method to find the next hop IP address.
     * The logic is to check if the destination ports have an IP address
     * by checking the logical interface (e.g., for port physical ge-2/0/1,
     * a logical interface may be ge-2/0/1.0
     *
     * @param deviceId the device id of the flow rule to be installed
     * @param port     output port of the flow rule treatment
     * @return optional IPv4 address of a next hop
     */
    private Optional<Ip4Address> findIpDst(DeviceId deviceId, Port port) {
        LinkService linkService = this.handler().get(LinkService.class);
        Set<Link> links = linkService.getEgressLinks(new ConnectPoint(deviceId, port.number()));
        DeviceService deviceService = this.handler().get(DeviceService.class);
        //Using only links with adjacency discovered by the LLDP protocol (see LinkDiscoveryJuniperImpl)
        Map<DeviceId, Port> dstPorts = links.stream().filter(l ->
                JuniperUtils.AK_IP.toUpperCase().equals(l.annotations().value(AnnotationKeys.LAYER)))
                .collect(Collectors.toMap(
                        l -> l.dst().deviceId(),
                        l -> deviceService.getPort(l.dst().deviceId(), l.dst().port())));
        for (Map.Entry<DeviceId, Port> entry : dstPorts.entrySet()) {
            String portName = entry.getValue().annotations().value(AnnotationKeys.PORT_NAME);

            Optional<Port> childPort = deviceService.getPorts(entry.getKey()).stream()
                    .filter(p -> Strings.nullToEmpty(
                            p.annotations().value(AnnotationKeys.PORT_NAME)).contains(portName.trim()))
                    .filter(this::isIp)
                    .findAny();
            if (childPort.isPresent()) {
                return Optional.ofNullable(Ip4Address.valueOf(childPort.get().annotations().value(JuniperUtils.AK_IP)));
            }
        }

        return Optional.empty();
    }

    /**
     * Helper method to find if an interface has an IP address.
     * It will check the annotations of the port.
     *
     * @param port the port
     * @return true if the IP address is present. Otherwise false.
     */
    private boolean isIp(Port port) {
        final String ipv4 = port.annotations().value(JuniperUtils.AK_IP);
        if (StringUtils.isEmpty(ipv4)) {
            return false;
        }
        try {
            Ip4Address.valueOf(ipv4);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }
}
