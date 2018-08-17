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

package org.onosproject.evpnopenflow.manager.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.packet.EthType;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MplsLabel;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.evpnopenflow.manager.EvpnService;
import org.onosproject.evpnopenflow.rsc.VpnAfConfig;
import org.onosproject.evpnopenflow.rsc.VpnInstance;
import org.onosproject.evpnopenflow.rsc.VpnInstanceId;
import org.onosproject.evpnopenflow.rsc.VpnPort;
import org.onosproject.evpnopenflow.rsc.VpnPortId;
import org.onosproject.evpnopenflow.rsc.baseport.BasePortService;
import org.onosproject.evpnopenflow.rsc.vpnafconfig.VpnAfConfigEvent;
import org.onosproject.evpnopenflow.rsc.vpnafconfig.VpnAfConfigListener;
import org.onosproject.evpnopenflow.rsc.vpnafconfig.VpnAfConfigService;
import org.onosproject.evpnopenflow.rsc.vpninstance.VpnInstanceService;
import org.onosproject.evpnopenflow.rsc.vpnport.VpnPortEvent;
import org.onosproject.evpnopenflow.rsc.vpnport.VpnPortListener;
import org.onosproject.evpnopenflow.rsc.vpnport.VpnPortService;
import org.onosproject.evpnrouteservice.EvpnInstanceName;
import org.onosproject.evpnrouteservice.EvpnInstanceNextHop;
import org.onosproject.evpnrouteservice.EvpnInstancePrefix;
import org.onosproject.evpnrouteservice.EvpnInstanceRoute;
import org.onosproject.evpnrouteservice.EvpnNextHop;
import org.onosproject.evpnrouteservice.EvpnRoute;
import org.onosproject.evpnrouteservice.EvpnRoute.Source;
import org.onosproject.evpnrouteservice.EvpnRouteAdminService;
import org.onosproject.evpnrouteservice.EvpnRouteEvent;
import org.onosproject.evpnrouteservice.EvpnRouteListener;
import org.onosproject.evpnrouteservice.EvpnRouteService;
import org.onosproject.evpnrouteservice.EvpnRouteSet;
import org.onosproject.evpnrouteservice.EvpnRouteStore;
import org.onosproject.evpnrouteservice.Label;
import org.onosproject.evpnrouteservice.RouteDistinguisher;
import org.onosproject.evpnrouteservice.VpnRouteTarget;
import org.onosproject.gluon.rsc.GluonConfig;
import org.onosproject.incubator.net.resource.label.LabelResource;
import org.onosproject.incubator.net.resource.label.LabelResourceAdminService;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.incubator.net.resource.label.LabelResourceService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.Objective.Operation;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.onosproject.evpnopenflow.rsc.EvpnConstants.APP_ID;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.ARP_PRIORITY;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.ARP_RESPONSE;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.BASEPORT;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.BGP_EVPN_ROUTE_DELETE_START;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.BGP_EVPN_ROUTE_UPDATE_START;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.BOTH;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.CANNOT_FIND_TUNNEL_PORT_DEVICE;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.CANT_FIND_CONTROLLER_DEVICE;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.CANT_FIND_VPN_INSTANCE;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.CANT_FIND_VPN_PORT;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.DELETE;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.EVPN_OPENFLOW_START;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.EVPN_OPENFLOW_STOP;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.EXPORT_EXTCOMMUNITY;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.FAILED_TO_SET_TUNNEL_DST;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.GET_PRIVATE_LABEL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.HOST_DETECT;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.HOST_VANISHED;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.IFACEID;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.IFACEID_OF_HOST_IS_NULL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.IMPORT_EXTCOMMUNITY;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.INVALID_EVENT_RECEIVED;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.INVALID_ROUTE_TARGET_TYPE;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.INVALID_TARGET_RECEIVED;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.MPLS_OUT_FLOWS;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.NETWORK_CONFIG_EVENT_IS_RECEIVED;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.NOT_MASTER_FOR_SPECIFIC_DEVICE;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.RELEASE_LABEL_FAILED;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.ROUTE_ADD_ARP_RULES;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.ROUTE_REMOVE_ARP_RULES;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.SET;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.SLASH;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.SWITCH_CHANNEL_ID;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.TUNNEL_DST;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.UPDATE;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_AF_TARGET;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_INSTANCE_TARGET;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_PORT_BIND;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_PORT_TARGET;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_PORT_UNBIND;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VXLAN;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_TUNNEL_DST;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the EVPN service.
 */
@Component(immediate = true, service = EvpnService.class)
public class EvpnManager implements EvpnService {
    private final Logger log = getLogger(getClass());
    private static final EthType.EtherType ARP_TYPE = EthType.EtherType.ARP;

    protected ApplicationId appId;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected EvpnRouteService evpnRouteService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected EvpnRouteStore evpnRouteStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected EvpnRouteAdminService evpnRouteAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LabelResourceAdminService labelAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LabelResourceService labelService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected VpnInstanceService vpnInstanceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected VpnPortService vpnPortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected VpnAfConfigService vpnAfConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigService configService;

    public Set<EvpnInstanceRoute> evpnInstanceRoutes = new HashSet<>();
    private final HostListener hostListener = new InnerHostListener();
    private final VpnPortListener vpnPortListner = new InnerVpnPortListener();
    private final VpnAfConfigListener vpnAfConfigListener = new
            InnerVpnAfConfigListener();
    private final InternalRouteEventListener routeListener = new
            InternalRouteEventListener();

    private final NetworkConfigListener configListener = new
            InternalNetworkConfigListener();

    @Activate
    public void activate() {
        appId = coreService.registerApplication(APP_ID);
        hostService.addListener(hostListener);
        vpnPortService.addListener(vpnPortListner);
        vpnAfConfigService.addListener(vpnAfConfigListener);
        configService.addListener(configListener);
        evpnRouteService.addListener(routeListener);

        labelAdminService
                .createGlobalPool(LabelResourceId.labelResourceId(1),
                                  LabelResourceId.labelResourceId(1000));
        log.info(EVPN_OPENFLOW_START);
    }

    @Deactivate
    public void deactivate() {
        hostService.removeListener(hostListener);
        vpnPortService.removeListener(vpnPortListner);
        vpnAfConfigService.removeListener(vpnAfConfigListener);
        configService.removeListener(configListener);
        log.info(EVPN_OPENFLOW_STOP);
    }

    @Override
    public void onBgpEvpnRouteUpdate(EvpnRoute route) {
        if (EvpnRoute.Source.LOCAL.equals(route.source())) {
            return;
        }
        log.info(BGP_EVPN_ROUTE_UPDATE_START, route);
        // deal with public route and transfer to private route
        if (vpnInstanceService.getInstances().isEmpty()) {
            log.info("unable to get instnaces from vpninstance");
            return;
        }

        vpnInstanceService.getInstances().forEach(vpnInstance -> {
            log.info("got instnaces from vpninstance but not entered here");
            List<VpnRouteTarget> vpnImportRouteRt = new
                    LinkedList<>(vpnInstance.getImportRouteTargets());
            List<VpnRouteTarget> expRt = route.exportRouteTarget();
            List<VpnRouteTarget> similar = new LinkedList<>(expRt);
            similar.retainAll(vpnImportRouteRt);

            if (!similar.isEmpty()) {
                EvpnInstancePrefix evpnPrefix = EvpnInstancePrefix
                        .evpnPrefix(route.prefixMac(), route.prefixIp());

                EvpnInstanceNextHop evpnNextHop = EvpnInstanceNextHop
                        .evpnNextHop(route.ipNextHop(), route.label());

                EvpnInstanceRoute evpnPrivateRoute = new
                        EvpnInstanceRoute(vpnInstance.vpnInstanceName(),
                                          route.routeDistinguisher(),
                                          vpnImportRouteRt,
                                          route.exportRouteTarget(),
                                          evpnPrefix,
                                          evpnNextHop,
                                          route.prefixIp(),
                                          route.ipNextHop(),
                                          route.label());

                //update route in route subsystem
                //TODO: added by shahid
                evpnInstanceRoutes.add(evpnPrivateRoute);

            }
        });

        deviceService.getAvailableDevices(Device.Type.SWITCH)
                .forEach(device -> {
                    log.info("switch device is found");
                    Set<Host> hosts = getHostsByVpn(device, route);
                    for (Host h : hosts) {
                        addArpFlows(device.id(),
                                    route,
                                    Objective.Operation.ADD,
                                    h);
                        ForwardingObjective.Builder objective =
                                getMplsOutBuilder(device.id(),
                                                  route,
                                                  h);
                        log.info(MPLS_OUT_FLOWS, h);
                        flowObjectiveService.forward(device.id(),
                                                     objective.add());
                    }
                });
        log.info("no switch device is found");
    }

    @Override
    public void onBgpEvpnRouteDelete(EvpnRoute route) {
        if (EvpnRoute.Source.LOCAL.equals(route.source())) {
            return;
        }
        log.info(BGP_EVPN_ROUTE_DELETE_START, route);
        // deal with public route deleted and transfer to private route
        vpnInstanceService.getInstances().forEach(vpnInstance -> {
            List<VpnRouteTarget> vpnRouteRt = new
                    LinkedList<>(vpnInstance.getImportRouteTargets());
            List<VpnRouteTarget> localRt = route.exportRouteTarget();
            List<VpnRouteTarget> similar = new LinkedList<>(localRt);
            similar.retainAll(vpnRouteRt);

            if (!similar.isEmpty()) {
                EvpnInstancePrefix evpnPrefix = EvpnInstancePrefix
                        .evpnPrefix(route.prefixMac(), route.prefixIp());

                EvpnInstanceNextHop evpnNextHop = EvpnInstanceNextHop
                        .evpnNextHop(route.ipNextHop(), route.label());

                EvpnInstanceRoute evpnPrivateRoute = new
                        EvpnInstanceRoute(vpnInstance.vpnInstanceName(),
                                          route.routeDistinguisher(),
                                          vpnRouteRt,
                                          route.exportRouteTarget(),
                                          evpnPrefix,
                                          evpnNextHop,
                                          route.prefixIp(),
                                          route.ipNextHop(),
                                          route.label());
                //TODO: Added by Shahid
                //evpnRouteAdminService.withdraw(Sets.newHashSet
                //       (evpnPrivateRoute));

            }
        });
        deviceService.getAvailableDevices(Device.Type.SWITCH)
                .forEach(device -> {
                    Set<Host> hosts = getHostsByVpn(device, route);
                    for (Host h : hosts) {
                        addArpFlows(device.id(),
                                    route,
                                    Objective.Operation.REMOVE,
                                    h);
                        ForwardingObjective.Builder objective
                                = getMplsOutBuilder(device.id(),
                                                    route,
                                                    h);
                        flowObjectiveService.forward(device.id(),
                                                     objective.remove());
                    }
                });
    }

    private void addArpFlows(DeviceId deviceId,
                             EvpnRoute route,
                             Operation type,
                             Host host) {
        DriverHandler handler = driverService.createHandler(deviceId);
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(ARP_TYPE.ethType().toShort())
                .matchArpTpa(route.prefixIp().address().getIp4Address())
                .matchInPort(host.location().port()).build();

        ExtensionTreatmentResolver resolver = handler
                .behaviour(ExtensionTreatmentResolver.class);
        ExtensionTreatment ethSrcToDst = resolver
                .getExtensionInstruction(ExtensionTreatmentType
                                                 .ExtensionTreatmentTypes
                                                 .NICIRA_MOV_ETH_SRC_TO_DST
                                                 .type());
        ExtensionTreatment arpShaToTha = resolver
                .getExtensionInstruction(ExtensionTreatmentType
                                                 .ExtensionTreatmentTypes
                                                 .NICIRA_MOV_ARP_SHA_TO_THA
                                                 .type());
        ExtensionTreatment arpSpaToTpa = resolver
                .getExtensionInstruction(ExtensionTreatmentType
                                                 .ExtensionTreatmentTypes
                                                 .NICIRA_MOV_ARP_SPA_TO_TPA
                                                 .type());
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .extension(ethSrcToDst, deviceId).setEthSrc(route.prefixMac())
                .setArpOp(ARP_RESPONSE).extension(arpShaToTha, deviceId)
                .extension(arpSpaToTpa, deviceId).setArpSha(route.prefixMac())
                .setArpSpa(route.prefixIp().address().getIp4Address())
                .setOutput(PortNumber.IN_PORT)
                .build();

        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment).withSelector(selector)
                .fromApp(appId).withFlag(ForwardingObjective.Flag.SPECIFIC)
                .withPriority(ARP_PRIORITY);
        if (type.equals(Objective.Operation.ADD)) {
            log.info(ROUTE_ADD_ARP_RULES);
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            log.info(ROUTE_REMOVE_ARP_RULES);
            flowObjectiveService.forward(deviceId, objective.remove());
        }
    }

    private Set<Host> getHostsByVpn(Device device, EvpnRoute route) {
        Set<Host> vpnHosts = Sets.newHashSet();
        Set<Host> hosts = hostService.getConnectedHosts(device.id());
        for (Host h : hosts) {
            String ifaceId = h.annotations().value(IFACEID);
            if (!vpnPortService.exists(VpnPortId.vpnPortId(ifaceId))) {
                continue;
            }

            VpnPort vpnPort = vpnPortService
                    .getPort(VpnPortId.vpnPortId(ifaceId));
            VpnInstanceId vpnInstanceId = vpnPort.vpnInstanceId();

            VpnInstance vpnInstance = vpnInstanceService
                    .getInstance(vpnInstanceId);

            List<VpnRouteTarget> expRt = route.exportRouteTarget();
            List<VpnRouteTarget> similar = new LinkedList<>(expRt);
            similar.retainAll(vpnInstance.getImportRouteTargets());
            //TODO: currently checking for RT comparison.
            //TODO: Need to check about RD comparison is really required.
            //if (route.routeDistinguisher()
            //.equals(vpnInstance.routeDistinguisher())) {
            if (!similar.isEmpty()) {
                vpnHosts.add(h);
            }
        }
        return vpnHosts;
    }

    private ForwardingObjective.Builder getMplsOutBuilder(DeviceId deviceId,
                                                          EvpnRoute route,
                                                          Host h) {
        DriverHandler handler = driverService.createHandler(deviceId);
        ExtensionTreatmentResolver resolver = handler
                .behaviour(ExtensionTreatmentResolver.class);
        ExtensionTreatment treatment = resolver
                .getExtensionInstruction(NICIRA_SET_TUNNEL_DST.type());
        try {
            treatment.setPropertyValue(TUNNEL_DST, route.ipNextHop());
        } catch (Exception e) {
            log.error(FAILED_TO_SET_TUNNEL_DST, deviceId);
        }
        TrafficTreatment.Builder builder = DefaultTrafficTreatment.builder();
        builder.extension(treatment, deviceId);
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(h.location().port()).matchEthSrc(h.mac())
                .matchEthDst(route.prefixMac()).build();

        TrafficTreatment build = builder.pushMpls()
                .setMpls(MplsLabel.mplsLabel(route.label().getLabel()))
                .setOutput(getTunnlePort(deviceId)).build();

        return DefaultForwardingObjective
                .builder().withTreatment(build).withSelector(selector)
                .fromApp(appId).withFlag(ForwardingObjective.Flag.SPECIFIC)
                .withPriority(60000);

    }

    private ForwardingObjective.Builder getMplsInBuilder(DeviceId deviceId,
                                                         Host host,
                                                         Label label) {
        TrafficTreatment.Builder builder = DefaultTrafficTreatment.builder();
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(getTunnlePort(deviceId))
                .matchEthType(EthType.EtherType.MPLS_UNICAST.ethType()
                                      .toShort())
                .matchMplsBos(true)
                .matchMplsLabel(MplsLabel.mplsLabel(label.getLabel())).build();
        TrafficTreatment treatment = builder.popMpls(EthType
                                                             .EtherType
                                                             .IPV4.ethType())
                .setOutput(host.location().port()).build();
        return DefaultForwardingObjective
                .builder().withTreatment(treatment).withSelector(selector)
                .fromApp(appId).withFlag(ForwardingObjective.Flag.SPECIFIC)
                .withPriority(60000);
    }

    /**
     * Get local tunnel ports.
     *
     * @param ports Iterable of Port
     * @return Collection of PortNumber
     */
    private Collection<PortNumber> getLocalTunnelPorts(Iterable<Port>
                                                               ports) {
        Collection<PortNumber> localTunnelPorts = new ArrayList<>();
        if (ports != null) {
            log.info("port value is not null {}", ports);
            Sets.newHashSet(ports).stream()
                    .filter(p -> !p.number().equals(PortNumber.LOCAL))
                    .forEach(p -> {
                        log.info("number is not matched but no vxlan port");
                        if (p.annotations().value(AnnotationKeys.PORT_NAME)
                                .startsWith(VXLAN)) {
                            localTunnelPorts.add(p.number());
                        }
                    });
        }
        return localTunnelPorts;
    }

    private PortNumber getTunnlePort(DeviceId deviceId) {
        Iterable<Port> ports = deviceService.getPorts(deviceId);
        Collection<PortNumber> localTunnelPorts = getLocalTunnelPorts(ports);
        if (localTunnelPorts.isEmpty()) {
            log.error(CANNOT_FIND_TUNNEL_PORT_DEVICE, deviceId);
            return null;
        }
        return localTunnelPorts.iterator().next();
    }

    private void setFlows(DeviceId deviceId, Host host, Label label,
                          List<VpnRouteTarget> rtImport,
                          Operation type) {
        log.info("Set the flows to OVS");
        ForwardingObjective.Builder objective = getMplsInBuilder(deviceId,
                                                                 host,
                                                                 label);
        if (type.equals(Objective.Operation.ADD)) {
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            flowObjectiveService.forward(deviceId, objective.remove());
        }

        // download remote flows if and only routes are present.
        evpnRouteStore.getRouteTables().forEach(routeTableId -> {
            Collection<EvpnRouteSet> routes
                    = evpnRouteStore.getRoutes(routeTableId);
            if (routes != null) {
                routes.forEach(route -> {
                    Collection<EvpnRoute> evpnRoutes = route.routes();
                    for (EvpnRoute evpnRoute : evpnRoutes) {
                        EvpnRoute evpnRouteTem = evpnRoute;
                        Set<Host> hostByMac = hostService
                                .getHostsByMac(evpnRouteTem
                                                       .prefixMac());

                        if (!hostByMac.isEmpty()
                                || (!(compareLists(rtImport, evpnRouteTem
                                .exportRouteTarget())))) {
                            log.info("Route target import/export is not matched");
                            continue;
                        }
                        log.info("Set the ARP flows");
                        addArpFlows(deviceId, evpnRouteTem, type, host);
                        ForwardingObjective.Builder build = getMplsOutBuilder(deviceId,
                                                                              evpnRouteTem,
                                                                              host);
                        log.info("Set the MPLS  flows");
                        if (type.equals(Objective.Operation.ADD)) {
                            flowObjectiveService.forward(deviceId, build.add());
                        } else {
                            flowObjectiveService.forward(deviceId, build.remove());
                        }
                    }
                });
            }
        });
    }

    /**
     * comparison for tow lists.
     *
     * @param list1 import list
     * @param list2 export list
     * @return true or false
     */
    public static boolean compareLists(List<VpnRouteTarget> list1,
                                       List<VpnRouteTarget> list2) {
        if (list1 == null && list2 == null) {
            return true;
        }

        if (list1 != null && list2 != null) {
            if (list1.size() == list2.size()) {
                for (VpnRouteTarget li1Long : list1) {
                    boolean isEqual = false;
                    for (VpnRouteTarget li2Long : list2) {
                        if (li1Long.equals(li2Long)) {
                            isEqual = true;
                            break;
                        }
                    }
                    if (!isEqual) {
                        return false;
                    }
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    @Override
    public void onHostDetected(Host host) {
        log.info(HOST_DETECT, host);
        DeviceId deviceId = host.location().deviceId();
        if (!mastershipService.isLocalMaster(deviceId)) {
            log.info(NOT_MASTER_FOR_SPECIFIC_DEVICE);
            return;
        }

        String ifaceId = host.annotations().value(IFACEID);
        if (ifaceId == null) {
            log.error(IFACEID_OF_HOST_IS_NULL);
            return;
        }
        VpnPortId vpnPortId = VpnPortId.vpnPortId(ifaceId);
        // Get VPN port id from EVPN app store
        if (!vpnPortService.exists(vpnPortId)) {
            log.info(CANT_FIND_VPN_PORT, ifaceId);
            return;
        }

        VpnPort vpnPort = vpnPortService.getPort(vpnPortId);
        VpnInstanceId vpnInstanceId = vpnPort.vpnInstanceId();
        if (!vpnInstanceService.exists(vpnInstanceId)) {
            log.info(CANT_FIND_VPN_INSTANCE, vpnInstanceId);
            return;
        }

        Label privateLabel = applyLabel();
        // create private route and get label
        setPrivateRoute(host, vpnInstanceId, privateLabel,
                        Objective.Operation.ADD);
        VpnInstance vpnInstance = vpnInstanceService.getInstance(vpnInstanceId);

        List<VpnRouteTarget> rtImport
                = new LinkedList<>(vpnInstance.getImportRouteTargets());
        List<VpnRouteTarget> rtExport
                = new LinkedList<>(vpnInstance.getExportRouteTargets());
        //download flows
        setFlows(deviceId, host, privateLabel, rtImport,
                 Objective.Operation.ADD);
    }

    /**
     * update or withdraw evpn route from route admin service.
     *
     * @param host          host
     * @param vpnInstanceId vpn instance id
     * @param privateLabel  private label
     * @param type          operation type
     */
    private void setPrivateRoute(Host host, VpnInstanceId vpnInstanceId,
                                 Label privateLabel,
                                 Operation type) {
        DeviceId deviceId = host.location().deviceId();
        Device device = deviceService.getDevice(deviceId);
        VpnInstance vpnInstance = vpnInstanceService.getInstance(vpnInstanceId);
        RouteDistinguisher rd = vpnInstance.routeDistinguisher();
        Set<VpnRouteTarget> importRouteTargets
                = vpnInstance.getImportRouteTargets();
        Set<VpnRouteTarget> exportRouteTargets
                = vpnInstance.getExportRouteTargets();
        EvpnInstanceName instanceName = vpnInstance.vpnInstanceName();
        String url = device.annotations().value(SWITCH_CHANNEL_ID);
        String controllerIp = url.substring(0, url.lastIndexOf(":"));

        if (controllerIp == null) {
            log.error(CANT_FIND_CONTROLLER_DEVICE, device.id().toString());
            return;
        }
        IpAddress ipAddress = IpAddress.valueOf(controllerIp);
        // create private route
        EvpnInstanceNextHop evpnNextHop = EvpnInstanceNextHop
                .evpnNextHop(ipAddress, privateLabel);
        EvpnInstancePrefix evpnPrefix = EvpnInstancePrefix
                .evpnPrefix(host.mac(), IpPrefix.valueOf(host.ipAddresses()
                                                                 .iterator()
                                                                 .next()
                                                                 .getIp4Address(), 32));
        EvpnInstanceRoute evpnPrivateRoute
                = new EvpnInstanceRoute(instanceName,
                                        rd,
                                        new LinkedList<>(importRouteTargets),
                                        new LinkedList<>(exportRouteTargets),
                                        evpnPrefix,
                                        evpnNextHop,
                                        IpPrefix.valueOf(host.ipAddresses()
                                                                 .iterator()
                                                                 .next()
                                                                 .getIp4Address(), 32),
                                        ipAddress,
                                        privateLabel);

        // change to public route
        EvpnRoute evpnRoute
                = new EvpnRoute(Source.LOCAL,
                                host.mac(),
                                IpPrefix.valueOf(host.ipAddresses()
                                                         .iterator()
                                                         .next()
                                                         .getIp4Address(), 32),
                                ipAddress,
                                rd,
                                new LinkedList<>(importRouteTargets),
                                new LinkedList<>(exportRouteTargets),
                                privateLabel);
        if (type.equals(Objective.Operation.ADD)) {
            //evpnRouteAdminService.update(Sets.newHashSet(evpnPrivateRoute));
            evpnInstanceRoutes.add(evpnPrivateRoute);
            evpnRouteAdminService.update(Sets.newHashSet(evpnRoute));

        } else {
            //evpnRouteAdminService.withdraw(Sets.newHashSet(evpnPrivateRoute));
            evpnInstanceRoutes.remove(evpnPrivateRoute);
            evpnRouteAdminService.withdraw(Sets.newHashSet(evpnRoute));
        }
    }

    /**
     * Generate the label for evpn route from global pool.
     */
    private Label applyLabel() {
        Collection<LabelResource> privateLabels = labelService
                .applyFromGlobalPool(1);
        Label privateLabel = Label.label(0);
        if (!privateLabels.isEmpty()) {
            privateLabel = Label.label(Integer.parseInt(
                    privateLabels.iterator().next()
                            .labelResourceId().toString()));
        }
        log.info(GET_PRIVATE_LABEL, privateLabel);
        return privateLabel;
    }

    @Override
    public void onHostVanished(Host host) {
        log.info(HOST_VANISHED, host);
        DeviceId deviceId = host.location().deviceId();
        if (!mastershipService.isLocalMaster(deviceId)) {
            return;
        }
        String ifaceId = host.annotations().value(IFACEID);
        if (ifaceId == null) {
            log.error(IFACEID_OF_HOST_IS_NULL);
            return;
        }
        // Get info from Gluon Shim
        VpnPort vpnPort = vpnPortService.getPort(VpnPortId.vpnPortId(ifaceId));
        VpnInstanceId vpnInstanceId = vpnPort.vpnInstanceId();
        if (!vpnInstanceService.exists(vpnInstanceId)) {
            log.info(CANT_FIND_VPN_INSTANCE, vpnInstanceId);
            return;
        }
        VpnInstance vpnInstance = vpnInstanceService.getInstance(vpnInstanceId);

        Label label = releaseLabel(vpnInstance, host);
        // create private route and get label
        setPrivateRoute(host, vpnInstanceId, label, Objective.Operation.REMOVE);
        // download flows
        List<VpnRouteTarget> rtImport
                = new LinkedList<>(vpnInstance.getImportRouteTargets());
        List<VpnRouteTarget> rtExport
                = new LinkedList<>(vpnInstance.getExportRouteTargets());
        setFlows(deviceId, host, label, rtImport,
                 Objective.Operation.REMOVE);
    }

    /**
     * Release the label from the evpn route.
     *
     * @param vpnInstance vpn instance
     * @param host        host
     */
    private Label releaseLabel(VpnInstance vpnInstance, Host host) {
        EvpnInstanceName instanceName = vpnInstance.vpnInstanceName();

        //Get all vpn-instance routes and check for label.
        Label label = null;
        for (EvpnInstanceRoute evpnInstanceRoute : evpnInstanceRoutes) {
            if (evpnInstanceRoute.evpnInstanceName().equals(instanceName)) {
                label = evpnInstanceRoute.getLabel();
                // delete private route and get label ,change to public route
                boolean isRelease
                        = labelService
                        .releaseToGlobalPool(
                                Sets.newHashSet(
                                        LabelResourceId
                                                .labelResourceId(label.getLabel())));
                if (!isRelease) {
                    log.error(RELEASE_LABEL_FAILED, label.getLabel());
                }
                break;
            }
        }
        return label;
    }

    private class InternalRouteEventListener implements EvpnRouteListener {

        @Override
        public void event(EvpnRouteEvent event) {
            if (event.subject() != null) {
                EvpnRoute route = (EvpnRoute) event.subject();
                if (EvpnRouteEvent.Type.ROUTE_ADDED == event.type()) {
                    onBgpEvpnRouteUpdate(route);
                } else if (EvpnRouteEvent.Type.ROUTE_REMOVED == event.type()) {
                    onBgpEvpnRouteDelete(route);
                }
            } else {
                return;
            }
        }
    }

    private class InnerHostListener implements HostListener {

        @Override
        public void event(HostEvent event) {
            Host host = event.subject();
            if (HostEvent.Type.HOST_ADDED == event.type()) {
                onHostDetected(host);
            } else if (HostEvent.Type.HOST_REMOVED == event.type()) {
                onHostVanished(host);
            }
        }

    }

    private class InnerVpnPortListener implements VpnPortListener {

        @Override
        public void event(VpnPortEvent event) {
            VpnPort vpnPort = event.subject();
            if (VpnPortEvent.Type.VPN_PORT_DELETE == event.type()) {
                onVpnPortDelete(vpnPort);
            } else if (VpnPortEvent.Type.VPN_PORT_SET == event.type()) {
                onVpnPortSet(vpnPort);
            }
        }
    }

    @Override
    public void onVpnPortDelete(VpnPort vpnPort) {
        // delete the flows of this vpn
        hostService.getHosts().forEach(host -> {
            VpnPortId vpnPortId = vpnPort.id();
            VpnInstanceId vpnInstanceId = vpnPort.vpnInstanceId();
            if (!vpnInstanceService.exists(vpnInstanceId)) {
                log.error(CANT_FIND_VPN_INSTANCE, vpnInstanceId);
                return;
            }
            VpnInstance vpnInstance = vpnInstanceService
                    .getInstance(vpnInstanceId);
            List<VpnRouteTarget> rtImport
                    = new LinkedList<>(vpnInstance.getImportRouteTargets());
            List<VpnRouteTarget> rtExport
                    = new LinkedList<>(vpnInstance.getExportRouteTargets());

            if (vpnPortId.vpnPortId()
                    .equals(host.annotations().value(IFACEID))) {
                log.info(VPN_PORT_UNBIND);
                Label label = releaseLabel(vpnInstance, host);
                // create private route and get label
                DeviceId deviceId = host.location().deviceId();
                setPrivateRoute(host, vpnInstanceId, label,
                                Objective.Operation.REMOVE);
                // download flows
                setFlows(deviceId, host, label, rtImport,
                         Objective.Operation.REMOVE);
            }
        });
    }

    @Override
    public void onVpnPortSet(VpnPort vpnPort) {
        // delete the flows of this vpn
        hostService.getHosts().forEach(host -> {
            VpnPortId vpnPortId = vpnPort.id();
            VpnInstanceId vpnInstanceId = vpnPort.vpnInstanceId();
            VpnInstance vpnInstance = vpnInstanceService
                    .getInstance(vpnInstanceId);
            if (vpnInstance == null) {
                log.info("why vpn instance is null");
                return;
            }
            List<VpnRouteTarget> rtImport
                    = new LinkedList<>(vpnInstance.getImportRouteTargets());
/*            List<VpnRouteTarget> rtExport
                    = new LinkedList<>(vpnInstance.getExportRouteTargets());*/

            if (!vpnInstanceService.exists(vpnInstanceId)) {
                log.error(CANT_FIND_VPN_INSTANCE, vpnInstanceId);
                return;
            }

            if (vpnPortId.vpnPortId()
                    .equals(host.annotations().value(IFACEID))) {
                log.info(VPN_PORT_BIND);
                Label label = applyLabel();
                // create private route and get label
                DeviceId deviceId = host.location().deviceId();
                setPrivateRoute(host, vpnInstanceId, label,
                                Objective.Operation.ADD);
                // download flows
                setFlows(deviceId, host, label, rtImport,
                         Objective.Operation.ADD);
            }
        });
    }

    /**
     * process the gluon configuration and will update the configuration into
     * vpn port service.
     *
     * @param action action
     * @param key    key
     * @param value  json node
     */
    private void processEtcdResponse(String action, String key, JsonNode
            value) {
        String[] list = key.split(SLASH);
        String target = list[list.length - 2];
        switch (target) {
            case VPN_INSTANCE_TARGET:
                VpnInstanceService vpnInstanceService
                        = DefaultServiceDirectory
                        .getService(VpnInstanceService.class);
                vpnInstanceService.processGluonConfig(action, key, value);
                break;
            case VPN_PORT_TARGET:
                VpnPortService vpnPortService = DefaultServiceDirectory
                        .getService(VpnPortService.class);
                vpnPortService.processGluonConfig(action, key, value);
                break;
            case VPN_AF_TARGET:
                VpnAfConfigService vpnAfConfigService =
                        DefaultServiceDirectory.getService(VpnAfConfigService
                                                                   .class);
                vpnAfConfigService.processGluonConfig(action, key, value);
                break;
            case BASEPORT:
                BasePortService basePortService =
                        DefaultServiceDirectory.getService(BasePortService
                                                                   .class);
                basePortService.processGluonConfig(action, key, value);
                break;
            default:
                log.info("why target type is invalid {}", target);
                log.info(INVALID_TARGET_RECEIVED);
                break;
        }
    }

    /**
     * parse the gluon configuration received from network config system.
     *
     * @param jsonNode json node
     * @param key      key
     * @param action   action
     */
    private void parseEtcdResponse(JsonNode jsonNode,
                                   String key,
                                   String action) {
        JsonNode modifyValue = null;
        if (action.equals(SET)) {
            modifyValue = jsonNode.get(key);
        }
        processEtcdResponse(action, key, modifyValue);
    }

    /**
     * Listener for network config events.
     */
    private class InternalNetworkConfigListener implements
            NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            String subject;
            log.info(NETWORK_CONFIG_EVENT_IS_RECEIVED, event.type());
            if (!event.configClass().equals(GluonConfig.class)) {
                return;
            }
            log.info("Event is received from network configuration {}", event
                    .type());
            switch (event.type()) {
                case CONFIG_UPDATED:
                    subject = (String) event.subject();
                    GluonConfig gluonConfig = configService
                            .getConfig(subject, GluonConfig.class);
                    JsonNode jsonNode = gluonConfig.node();
                    parseEtcdResponse(jsonNode, subject, SET);
                    break;
                case CONFIG_REMOVED:
                    subject = (String) event.subject();
                    parseEtcdResponse(null, subject, DELETE);
                    break;
                default:
                    log.info(INVALID_EVENT_RECEIVED);
                    break;
            }
        }
    }

    /**
     * update import and export route target information in route admin service.
     *
     * @param evpnInstanceName   evpn instance name
     * @param exportRouteTargets export route targets
     * @param importRouteTargets import route targets
     * @param action             action holds update or delete
     */
    private void updateImpExpRtInRoute(EvpnInstanceName evpnInstanceName,
                                       Set<VpnRouteTarget> exportRouteTargets,
                                       Set<VpnRouteTarget> importRouteTargets,
                                       String action) {

        for (EvpnInstanceRoute evpnInstanceRoute : evpnInstanceRoutes) {
            if (evpnInstanceRoute.evpnInstanceName().equals(evpnInstanceName)) {
                evpnInstanceRoute
                        .setExportRtList(new LinkedList<>(exportRouteTargets));
                evpnInstanceRoute
                        .setImportRtList(new LinkedList<>(importRouteTargets));
                if (action.equals(UPDATE)) {
                    evpnInstanceRoutes.add(evpnInstanceRoute);
                } else if (action.equals(DELETE)) {
                    evpnInstanceRoutes.remove(evpnInstanceRoute);
                }
                //Get the public route and update route targets.
                EvpnNextHop evpnNextHop = EvpnNextHop
                        .evpnNextHop(evpnInstanceRoute.getNextHopl(),
                                     evpnInstanceRoute.importRouteTarget(),
                                     evpnInstanceRoute.exportRouteTarget(),
                                     evpnInstanceRoute.getLabel());
                Collection<EvpnRoute> evpnPublicRoutes
                        = evpnRouteStore.getRoutesForNextHop(evpnNextHop.nextHop());
                for (EvpnRoute pubRoute : evpnPublicRoutes) {
                    EvpnRoute evpnPubRoute = pubRoute;
                    if (evpnPubRoute.label().equals(evpnInstanceRoute
                                                            .getLabel())) {
                        evpnPubRoute
                                .setExportRtList(new LinkedList<>(exportRouteTargets));
                        evpnPubRoute
                                .setImportRtList(new LinkedList<>(importRouteTargets));
                        if (action.equals(UPDATE)) {
                            evpnRouteAdminService.update(Sets.newHashSet(evpnPubRoute));
                        } else if (action.equals(DELETE)) {
                            evpnRouteAdminService
                                    .withdraw(Sets.newHashSet(evpnPubRoute));
                        }
                    }
                }
            }
        }
    }

    /**
     * update or withdraw evpn route based on vpn af configuration.
     *
     * @param vpnAfConfig vpn af configuration
     * @param action      action holds update or delete
     */

    private void processEvpnRouteUpdate(VpnAfConfig vpnAfConfig,
                                        String action) {
        Collection<VpnInstance> instances
                = vpnInstanceService.getInstances();
        for (VpnInstance vpnInstance : instances) {
            Set<VpnRouteTarget> configRouteTargets
                    = vpnInstance.getConfigRouteTargets();
            for (VpnRouteTarget vpnRouteTarget : configRouteTargets) {
                if (vpnRouteTarget.equals(vpnAfConfig.routeTarget())) {
                    Set<VpnRouteTarget> exportRouteTargets
                            = vpnInstance.getExportRouteTargets();
                    Set<VpnRouteTarget> importRouteTargets
                            = vpnInstance.getImportRouteTargets();
                    String routeTargetType = vpnAfConfig.routeTargetType();
                    if (action.equals(UPDATE)) {
                        vpnInstanceService
                                .updateImpExpRouteTargets(routeTargetType,
                                                          exportRouteTargets,
                                                          importRouteTargets,
                                                          vpnRouteTarget);
                    } else if (action.equals(DELETE)) {
                        switch (routeTargetType) {
                            case EXPORT_EXTCOMMUNITY:
                                exportRouteTargets.remove(vpnRouteTarget);
                                break;
                            case IMPORT_EXTCOMMUNITY:
                                importRouteTargets.remove(vpnRouteTarget);
                                break;
                            case BOTH:
                                exportRouteTargets.remove(vpnRouteTarget);
                                importRouteTargets.remove(vpnRouteTarget);
                                break;
                            default:
                                log.info(INVALID_ROUTE_TARGET_TYPE);
                                break;
                        }
                    }
                    updateImpExpRtInRoute(vpnInstance.vpnInstanceName(),
                                          exportRouteTargets,
                                          importRouteTargets,
                                          action);
                }
            }
        }
    }

    private class InnerVpnAfConfigListener implements VpnAfConfigListener {

        @Override
        public void event(VpnAfConfigEvent event) {
            VpnAfConfig vpnAfConfig = event.subject();
            if (VpnAfConfigEvent.Type.VPN_AF_CONFIG_DELETE == event.type()) {
                processEvpnRouteUpdate(vpnAfConfig, DELETE);
            } else if (VpnAfConfigEvent.Type.VPN_AF_CONFIG_SET
                    == event.type() || VpnAfConfigEvent.Type
                    .VPN_AF_CONFIG_UPDATE == event.type()) {
                processEvpnRouteUpdate(vpnAfConfig, UPDATE);
            }
        }
    }
}
