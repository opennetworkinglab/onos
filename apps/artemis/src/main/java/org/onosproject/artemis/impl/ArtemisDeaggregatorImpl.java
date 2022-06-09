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
package org.onosproject.artemis.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;
import com.eclipsesource.json.JsonObject;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;
import org.onosproject.artemis.ArtemisDeaggregator;
import org.onosproject.artemis.ArtemisEventListener;
import org.onosproject.artemis.ArtemisMoasAgent;
import org.onosproject.artemis.ArtemisPacketProcessor;
import org.onosproject.artemis.ArtemisService;
import org.onosproject.artemis.BgpSpeakers;
import org.onosproject.artemis.impl.bgpspeakers.QuaggaBgpSpeakers;
import org.onosproject.artemis.impl.moas.MoasClientController;
import org.onosproject.artemis.impl.moas.MoasServerController;
import org.onosproject.artemis.impl.objects.ArtemisMessage;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.ovsdb.controller.OvsdbBridge;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbInterface;
import org.onosproject.routing.bgp.BgpInfoService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.onlab.packet.Ethernet.TYPE_IPV4;

@Component(immediate = true, service = ArtemisDeaggregator.class)
public class ArtemisDeaggregatorImpl implements ArtemisDeaggregator {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final int PRIORITY = 1000;

    /* Services */
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private BgpInfoService bgpInfoService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private ArtemisService artemisService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private OvsdbController ovsdbController;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private CoreService coreService;

    /* Variables */
    private Set<BgpSpeakers> bgpSpeakers = Sets.newHashSet();
    private MoasServerController moasServer;

    private Port tunnelPort = null;
    private ApplicationId appId;

    private IpAddress remoteTunnelIp = null;
    private IpPrefix remotePrefix = null;
    private boolean rulesInstalled;

    /* Agent */
    private InternalMoasAgent moasAgent = new InternalMoasAgent();
    private InternalPacketProcessor packetProcessor = new InternalPacketProcessor();
    private InternalDeviceListener deviceListener = new InternalDeviceListener();

    private Set<MoasClientController> moasClientControllers = Sets.newConcurrentHashSet();

    private final ArtemisEventListener artemisEventListener = this::handleArtemisEvent;

    @Activate
    protected void activate() {
        rulesInstalled = false;

        // FIXME: add other type of BGP Speakers when Dynamic Configuration is available
        bgpSpeakers.add(new QuaggaBgpSpeakers(bgpInfoService));

        moasServer = new MoasServerController();
        moasServer.start(moasAgent, packetProcessor);

        deviceService.addListener(deviceListener);

        appId = coreService.getAppId("org.onosproject.artemis");

        // enable OVSDB for the switches that we will install the GRE tunnel
        artemisService.getConfig().ifPresent(config -> config.moasInfo().getTunnelPoints()
                .forEach(tunnelPoint -> ovsdbController.connect(tunnelPoint.getOvsdbIp(), TpPort.tpPort(6640)))
        );

        artemisService.addListener(artemisEventListener);

        log.info("Artemis Deaggregator Service Started");

        /*
        log.info("interfaces {}", interfaceService.getInterfaces());

        [{
         "name": "",
         "connectPoint": "of:000000000000000a/2",
         "ipAddresses": "[1.1.1.1/30]",
         "macAddress": "00:00:00:00:00:01"
        },
        {
         "name": "",
         "connectPoint": "of:000000000000000a/3",
         "ipAddresses": "[10.0.0.1/8]",
         "macAddress": "00:00:00:00:00:01"
        }]
        */
    }

    @Deactivate
    protected void deactivate() {
        moasServer.stop();

        moasClientControllers.forEach(MoasClientController::stop);
        moasClientControllers.clear();

        flowRuleService.removeFlowRulesById(appId);
        deviceService.removeListener(deviceListener);

        remoteTunnelIp = null;
        remotePrefix = null;
        tunnelPort = null;

        artemisService.removeListener(artemisEventListener);

        log.info("Artemis Deaggregator Service Stopped");
    }

    /**
     * Create a GRE tunnel interface pointing to remote MOAS.
     *
     * @param remoteIp remote ip on GRE tunnel
     */
    private void createTunnelInterface(IpAddress remoteIp) {
        ovsdbController.getNodeIds().forEach(nodeId -> artemisService.getConfig().flatMap(config ->
                config.moasInfo().getTunnelPoints()
                        .stream()
                        .filter(tunnelPoint -> tunnelPoint.getOvsdbIp().toString().equals(nodeId.getIpAddress()))
                        .findFirst()
        ).ifPresent(tunnelPoint -> {
            OvsdbClientService ovsdbClient = ovsdbController.getOvsdbClient(nodeId);
            ovsdbClient.dropInterface("gre-int");
            Map<String, String> options = Maps.newHashMap();
            options.put("remote_ip", remoteIp.toString());
            OvsdbInterface ovsdbInterface = OvsdbInterface.builder()
                    .name("gre-int")
                    .options(options)
                    .type(OvsdbInterface.Type.GRE)
                    .build();
            OvsdbBridge mainBridge = ovsdbClient.getBridges().iterator().next();
            ovsdbClient.createInterface(mainBridge.name(), ovsdbInterface);
            log.info("Tunnel setup at {} - {}", nodeId, tunnelPoint);
        }));
    }

    /**
     * Install rules.
     */
    private void installRules() {
        log.info("Remote Data {} - {} - {}", tunnelPort, remoteTunnelIp, remotePrefix);
        // FIXME: currently works only for a simple pair of client-server
        if (!rulesInstalled && tunnelPort != null && remoteTunnelIp != null) {
            if (remotePrefix != null) {
                installServerRules();
            } else {
                installClientRules();
            }
            rulesInstalled = true;
        }
    }

    /**
     * Rules to be installed on MOAS Client.
     */
    private void installClientRules() {
        log.info("installClientRules");
        artemisService.getConfig().ifPresent(config -> {
            // selector
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchEthType(TYPE_IPV4)
                    .matchIPSrc(remoteTunnelIp.toIpPrefix())
                    .matchIPDst(config.moasInfo().getTunnelPoint().getLocalIp().toIpPrefix())
                    .build();
            // treatment
            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(PortNumber.LOCAL)
                    .build();
            // forwarding objective builder
            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .withPriority(PRIORITY)
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .fromApp(appId)
                    .add();
            // send flow objective to specified switch
            flowObjectiveService.forward(DeviceId.deviceId(tunnelPort.element().id().toString()),
                    forwardingObjective);

            log.info("Installing flow rule = {}", forwardingObjective);
        });
    }

    /**
     * Rules to be isntalled on MOAS Server.
     */
    private void installServerRules() {
        log.info("installServerRules");
        artemisService.getConfig().ifPresent(config -> {
            // selector
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchEthType(TYPE_IPV4)
                    .matchIPDst(remotePrefix)
                    .build();
            // treatment
            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(tunnelPort.number())
                    .build();
            // forwarding objective builder
            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .withPriority(PRIORITY)
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .fromApp(appId)
                    .add();
            // send flow objective to specified switch
            flowObjectiveService.forward(DeviceId.deviceId(tunnelPort.element().id().toString()),
                    forwardingObjective);

            log.info("Installing flow rule = {}", forwardingObjective);

            // selector
            selector = DefaultTrafficSelector.builder()
                    .matchEthType(TYPE_IPV4)
                    .matchIPSrc(config.moasInfo().getTunnelPoint().getLocalIp().toIpPrefix())
                    .matchIPDst(remoteTunnelIp.toIpPrefix())
                    .build();
            // treatment
            treatment = DefaultTrafficTreatment.builder()
                    // FIXME: find a better way
                    .setOutput(PortNumber.portNumber(2))
                    .build();
            // forwarding objective builder
            forwardingObjective = DefaultForwardingObjective.builder()
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .withPriority(PRIORITY)
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .fromApp(appId)
                    .add();
            // send flow objective to specified switch
            flowObjectiveService.forward(DeviceId.deviceId(tunnelPort.element().id().toString()),
                    forwardingObjective);

            log.info("Installing flow rule = {}", forwardingObjective);
        });
    }

    /**
     * Handles a artemis event.
     *
     * @param event the artemis event
     */
    protected void handleArtemisEvent(ArtemisEvent event) {
        if (event.type().equals(ArtemisEvent.Type.HIJACK_ADDED)) {
            IpPrefix receivedPrefix = (IpPrefix) event.subject();

            log.info("Deaggregator received a prefix " + receivedPrefix.toString());

            // can only de-aggregate /23 subnets and higher
            int cidr = receivedPrefix.prefixLength();
            if (receivedPrefix.prefixLength() < 24) {
                byte[] octets = receivedPrefix.address().toOctets();
                int byteGroup = (cidr + 1) / 8,
                        bitPos = 8 - (cidr + 1) % 8;

                octets[byteGroup] = (byte) (octets[byteGroup] & ~(1 << bitPos));
                String low = IpPrefix.valueOf(IpAddress.Version.INET, octets, cidr + 1).toString();
                octets[byteGroup] = (byte) (octets[byteGroup] | (1 << bitPos));
                String high = IpPrefix.valueOf(IpAddress.Version.INET, octets, cidr + 1).toString();

                String[] prefixes = {low, high};
                bgpSpeakers.forEach(bgpSpeakers -> bgpSpeakers.announceSubPrefixes(prefixes));
            } else {
                log.warn("Initiating MOAS");

                artemisService.getConfig().ifPresent(config -> config.monitoredPrefixes().forEach(artemisPrefixes -> {
                            log.info("checking if {} > {}", artemisPrefixes.prefix(), receivedPrefix);
                            if (artemisPrefixes.prefix().contains(receivedPrefix)) {
                                artemisPrefixes.moas().forEach(moasAddress -> {
                                            log.info("Creating a client for {}", moasAddress);
                                            MoasClientController client = new MoasClientController(
                                                    packetProcessor,
                                                    moasAddress,
                                                    config.moasInfo().getTunnelPoints().iterator().next()
                                                            .getLocalIp(),
                                                    receivedPrefix);
                                            log.info("Running client");
                                            client.run();
                                            moasClientControllers.add(client);
                                        }
                                );
                            }
                        }
                ));
            }

        }
    }

    private class InternalPacketProcessor implements ArtemisPacketProcessor {
        @Override
        public void processMoasPacket(ArtemisMessage msg, ChannelHandlerContext ctx) {
            log.info("Received {}", msg);
            switch (msg.getType()) {
                case INITIATE_FROM_CLIENT: {
                    artemisService.getConfig().ifPresent(config -> {
                        // SERVER SIDE CODE
                        createTunnelInterface(IpAddress.valueOf(msg.getLocalIp()));

                        ArtemisMessage message = new ArtemisMessage();
                        message.setType(ArtemisMessage.Type.INITIATE_FROM_SERVER);
                        message.setLocalIp(
                                config.moasInfo().getTunnelPoints()
                                        .iterator()
                                        .next()
                                        .getLocalIp()
                                        .toString());

                        ObjectMapper mapper = new ObjectMapper();
                        try {
                            String jsonInString = mapper.writeValueAsString(message);
                            ByteBuf buffer = Unpooled.copiedBuffer(jsonInString, CharsetUtil.UTF_8);
                            ctx.writeAndFlush(buffer);
                        } catch (JsonProcessingException e) {
                            log.warn("processMoasPacket()", e);
                        }

                        remoteTunnelIp = IpAddress.valueOf(msg.getLocalIp());
                        remotePrefix = IpPrefix.valueOf(msg.getLocalPrefix());
                    });
                    break;
                }
                case INITIATE_FROM_SERVER: {
                    // CLIENT SIDE CODE
                    createTunnelInterface(IpAddress.valueOf(msg.getLocalIp()));

                    remoteTunnelIp = IpAddress.valueOf(msg.getLocalIp());

                    break;
                }
                default:
            }

            installRules();
        }

        @Override
        public void processMonitorPacket(JsonObject msg) {

        }
    }

    private class InternalMoasAgent implements ArtemisMoasAgent {

        @Override
        public void addMoas(IpAddress ipAddress, ChannelHandlerContext ctx) {
            Optional<ArtemisConfig> config = artemisService.getConfig();
            if (config.isPresent() && config.get().moasInfo().getMoasAddresses().contains(ipAddress)) {
                log.info("Received Moas request from legit IP address");
            } else {
                log.info("Received Moas request from unknown IP address; ignoring..");
                ctx.close();
            }
        }

        @Override
        public void removeMoas(IpAddress ipAddress) {

        }
    }

    private class InternalDeviceListener implements DeviceListener {

        /*
            EVENT
            DefaultDevice{id=of:000000000000000a, type=SWITCH, manufacturer=Nicira, Inc., hwVersion=Open vSwitch,
            swVersion=2.8.0, serialNumber=None, driver=ovs}
            DefaultPort{element=of:000000000000000a, number=5, isEnabled=true, type=COPPER, portSpeed=0, annotations=
            {portMac=96:13:4c:12:ca:8a, portName=gre-int}}
         */
        @Override
        public void event(DeviceEvent event) {
            switch (event.type()) {
                case PORT_UPDATED:
                case PORT_ADDED: {
                    log.info("event {}", event);
                    // FIXME: currently only one tunnel is supported
                    if (event.port().annotations().keys().contains("portName") &&
                            event.port().annotations().value("portName").equals("gre-int")) {
                        tunnelPort = event.port();

                        installRules();
                    }
                    break;
                }
                default:
            }
        }
    }
}
