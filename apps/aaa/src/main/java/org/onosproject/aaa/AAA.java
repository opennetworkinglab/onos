/*
 * Copyright 2015 AT&T Foundry
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
package org.onosproject.aaa;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.EAP;
import org.onlab.packet.EAPOL;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.RADIUS;
import org.onlab.packet.RADIUSAttribute;
import org.onlab.packet.TpPort;
import org.onlab.packet.UDP;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.xosintegration.VoltTenantService;
import org.slf4j.Logger;

import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;
import static org.onosproject.net.packet.PacketPriority.CONTROL;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * AAA application for ONOS.
 */
@Component(immediate = true)
public class AAA {

    // for verbose output
    private final Logger log = getLogger(getClass());

    // a list of our dependencies :
    // to register with ONOS as an application - described next
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    // to receive Packet-in events that we'll respond to
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    // end host information
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VoltTenantService voltTenantService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry netCfgService;

    // Parsed RADIUS server addresses
    protected InetAddress radiusIpAddress;
    protected String radiusMacAddress;

    // NAS IP address
    protected InetAddress nasIpAddress;
    protected String nasMacAddress;

    // RADIUS server secret
    protected String radiusSecret;

    // ID of RADIUS switch
    protected String radiusSwitch;

    // RADIUS port number
    protected long radiusPort;

    // RADIUS server TCP port number
    protected short radiusServerPort;

    // our application-specific event handler
    private ReactivePacketProcessor processor = new ReactivePacketProcessor();

    // our unique identifier
    private ApplicationId appId;

    // Configuration properties factory
    private final ConfigFactory factory =
            new ConfigFactory<ApplicationId, AAAConfig>(APP_SUBJECT_FACTORY,
                                                         AAAConfig.class,
                                                         "AAA") {
                @Override
                public AAAConfig createConfig() {
                    return new AAAConfig();
                }
            };

    // Listener for config changes
    private final InternalConfigListener cfgListener = new InternalConfigListener();

    /**
     * Builds an EAPOL packet based on the given parameters.
     *
     * @param dstMac    destination MAC address
     * @param srcMac    source MAC address
     * @param vlan      vlan identifier
     * @param eapolType EAPOL type
     * @param eap       EAP payload
     * @return Ethernet frame
     */
    private static Ethernet buildEapolResponse(MacAddress dstMac, MacAddress srcMac,
                                               short vlan, byte eapolType, EAP eap) {

        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress(dstMac.toBytes());
        eth.setSourceMACAddress(srcMac.toBytes());
        eth.setEtherType(EthType.EtherType.EAPOL.ethType().toShort());
        if (vlan != Ethernet.VLAN_UNTAGGED) {
            eth.setVlanID(vlan);
        }
        //eapol header
        EAPOL eapol = new EAPOL();
        eapol.setEapolType(eapolType);
        eapol.setPacketLength(eap.getLength());

        //eap part
        eapol.setPayload(eap);

        eth.setPayload(eapol);
        eth.setPad(true);
        return eth;
    }

    @Activate
    public void activate() {
        netCfgService.addListener(cfgListener);
        netCfgService.registerConfigFactory(factory);

        // "org.onosproject.aaa" is the FQDN of our app
        appId = coreService.registerApplication("org.onosproject.aaa");

        cfgListener.reconfigureNetwork(netCfgService.getConfig(appId, AAAConfig.class));

        // register our event handler
        packetService.addProcessor(processor, PacketProcessor.director(2));
        requestIntercepts();

        StateMachine.initializeMaps();

        hostService.startMonitoringIp(IpAddress.valueOf(radiusIpAddress));
    }

    @Deactivate
    public void deactivate() {
        appId = coreService.registerApplication("org.onosproject.aaa");
        withdrawIntercepts();
        // de-register and null our handler
        packetService.removeProcessor(processor);
        processor = null;
        StateMachine.destroyMaps();
    }

    /**
     * Request packet in via PacketService.
     */
    private void requestIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(EthType.EtherType.EAPOL.ethType().toShort());
        packetService.requestPackets(selector.build(),
                                     CONTROL, appId);

        TrafficSelector radSelector = DefaultTrafficSelector.builder()
                .matchEthType(EthType.EtherType.IPV4.ethType().toShort())
                .matchIPProtocol(IPv4.PROTOCOL_UDP)
                .matchUdpDst(TpPort.tpPort(radiusServerPort))
                .matchUdpSrc(TpPort.tpPort(radiusServerPort))
                .build();
        packetService.requestPackets(radSelector, CONTROL, appId);
    }

    /**
     * Cancel request for packet in via PacketService.
     */
    private void withdrawIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(EthType.EtherType.EAPOL.ethType().toShort());
        packetService.cancelPackets(selector.build(), CONTROL, appId);

        TrafficSelector radSelector = DefaultTrafficSelector.builder()
                .matchEthType(EthType.EtherType.IPV4.ethType().toShort())
                .matchIPProtocol(IPv4.PROTOCOL_UDP)
                .matchUdpDst(TpPort.tpPort(radiusServerPort))
                .matchUdpSrc(TpPort.tpPort(radiusServerPort))
                .build();
        packetService.cancelPackets(radSelector, CONTROL, appId);
    }

    // our handler defined as a private inner class

    /**
     * Packet processor responsible for forwarding packets along their paths.
     */
    private class ReactivePacketProcessor implements PacketProcessor {
        @Override
        public void process(PacketContext context) {

            // Extract the original Ethernet frame from the packet information
            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();
            if (ethPkt == null) {
                return;
            }
            try {
                // identify if incoming packet comes from supplicant (EAP) or RADIUS
                switch (EthType.EtherType.lookup(ethPkt.getEtherType())) {
                    case EAPOL:
                        handleSupplicantPacket(context.inPacket());
                        break;
                    case IPV4:
                        IPv4 ipv4Packet = (IPv4) ethPkt.getPayload();
                        Ip4Address srcIp = Ip4Address.valueOf(ipv4Packet.getSourceAddress());
                        Ip4Address radiusIp4Address = Ip4Address.valueOf(radiusIpAddress);
                        if (srcIp.equals(radiusIp4Address) && ipv4Packet.getProtocol() == IPv4.PROTOCOL_UDP) {
                            // TODO: check for port as well when it's configurable
                            UDP udpPacket = (UDP) ipv4Packet.getPayload();

                            byte[] datagram = udpPacket.getPayload().serialize();
                            RADIUS radiusPacket;
                            radiusPacket = RADIUS.deserializer().deserialize(datagram, 0, datagram.length);
                            handleRadiusPacket(radiusPacket);
                        }

                        break;
                    default:
                        log.trace("Skipping Ethernet packet type {}",
                                  EthType.EtherType.lookup(ethPkt.getEtherType()));
                }
            } catch (DeserializationException | StateMachineException e) {
                log.warn("Unable to process RADIUS packet:", e);
            }
        }

        /**
         * Creates and initializes common fields of a RADIUS packet.
         *
         * @param identifier RADIUS identifier
         * @param eapPacket  EAP packet
         * @return RADIUS packet
         */
        private RADIUS getRadiusPayload(byte identifier, EAP eapPacket) {
            RADIUS radiusPayload =
                    new RADIUS(RADIUS.RADIUS_CODE_ACCESS_REQUEST,
                               eapPacket.getIdentifier());
            radiusPayload.setIdentifier(identifier);
            radiusPayload.setAttribute(RADIUSAttribute.RADIUS_ATTR_USERNAME,
                                       eapPacket.getData());

            radiusPayload.setAttribute(RADIUSAttribute.RADIUS_ATTR_NAS_IP,
                                       AAA.this.nasIpAddress.getAddress());

            radiusPayload.encapsulateMessage(eapPacket);
            radiusPayload.addMessageAuthenticator(AAA.this.radiusSecret);

            return radiusPayload;
        }

        /**
         * Handles PAE packets (supplicant).
         *
         * @param inPacket Ethernet packet coming from the supplicant
         */
        private void handleSupplicantPacket(InboundPacket inPacket) throws StateMachineException {
            Ethernet ethPkt = inPacket.parsed();
            // Where does it come from?
            MacAddress srcMAC = ethPkt.getSourceMAC();

            DeviceId deviceId = inPacket.receivedFrom().deviceId();
            PortNumber portNumber = inPacket.receivedFrom().port();
            String sessionId = deviceId.toString() + portNumber.toString();
            StateMachine stateMachine = StateMachine.lookupStateMachineBySessionId(sessionId);
            if (stateMachine == null) {
                stateMachine = new StateMachine(sessionId, voltTenantService);
            }


            EAPOL eapol = (EAPOL) ethPkt.getPayload();

            switch (eapol.getEapolType()) {
                case EAPOL.EAPOL_START:
                    stateMachine.start();
                    stateMachine.setSupplicantConnectpoint(inPacket.receivedFrom());

                    //send an EAP Request/Identify to the supplicant
                    EAP eapPayload = new EAP(EAP.REQUEST, stateMachine.identifier(), EAP.ATTR_IDENTITY, null);
                    Ethernet eth = buildEapolResponse(srcMAC, MacAddress.valueOf(1L),
                                                      ethPkt.getVlanID(), EAPOL.EAPOL_PACKET,
                                                      eapPayload);
                    stateMachine.setSupplicantAddress(srcMAC);
                    stateMachine.setVlanId(ethPkt.getVlanID());

                    this.sendPacketToSupplicant(eth, stateMachine.supplicantConnectpoint());

                    break;
                case EAPOL.EAPOL_PACKET:
                    RADIUS radiusPayload;
                    // check if this is a Response/Identify or  a Response/TLS
                    EAP eapPacket = (EAP) eapol.getPayload();

                    byte dataType = eapPacket.getDataType();
                    switch (dataType) {

                        case EAP.ATTR_IDENTITY:
                            // request id access to RADIUS
                            stateMachine.setUsername(eapPacket.getData());

                            radiusPayload = getRadiusPayload(stateMachine.identifier(), eapPacket);

                            // set Request Authenticator in StateMachine
                            stateMachine.setRequestAuthenticator(radiusPayload.generateAuthCode());
                            sendRadiusMessage(radiusPayload);

                            // change the state to "PENDING"
                            stateMachine.requestAccess();
                            break;
                        case EAP.ATTR_MD5:
                            // verify if the EAP identifier corresponds to the
                            // challenge identifier from the client state
                            // machine.
                            if (eapPacket.getIdentifier() == stateMachine.challengeIdentifier()) {
                                //send the RADIUS challenge response
                                radiusPayload = getRadiusPayload(stateMachine.challengeIdentifier(), eapPacket);

                                radiusPayload.setAttribute(RADIUSAttribute.RADIUS_ATTR_STATE,
                                                           stateMachine.challengeState());
                                sendRadiusMessage(radiusPayload);
                            }
                            break;
                        case EAP.ATTR_TLS:
                            // request id access to RADIUS
                            radiusPayload = getRadiusPayload(stateMachine.identifier(), eapPacket);

                            radiusPayload.setAttribute(RADIUSAttribute.RADIUS_ATTR_STATE,
                                                       stateMachine.challengeState());
                            stateMachine.setRequestAuthenticator(radiusPayload.generateAuthCode());

                            sendRadiusMessage(radiusPayload);
                            // TODO: this gets called on every fragment, should only be called at TLS-Start
                            stateMachine.requestAccess();

                            break;
                        default:
                            return;
                    }
                    break;
                default:
                    log.trace("Skipping EAPOL message {}", eapol.getEapolType());
            }
        }

        /**
         * Handles RADIUS packets.
         *
         * @param radiusPacket RADIUS packet coming from the RADIUS server.
         */
        private void handleRadiusPacket(RADIUS radiusPacket) throws StateMachineException {
            StateMachine stateMachine = StateMachine.lookupStateMachineById(radiusPacket.getIdentifier());
            if (stateMachine == null) {
                log.error("Invalid session identifier, exiting...");
                return;
            }

            EAP eapPayload;
            Ethernet eth;
            switch (radiusPacket.getCode()) {
                case RADIUS.RADIUS_CODE_ACCESS_CHALLENGE:
                    byte[] challengeState = radiusPacket.getAttribute(RADIUSAttribute.RADIUS_ATTR_STATE).getValue();
                    eapPayload = radiusPacket.decapsulateMessage();
                    stateMachine.setChallengeInfo(eapPayload.getIdentifier(), challengeState);
                    eth = buildEapolResponse(stateMachine.supplicantAddress(),
                                             MacAddress.valueOf(1L), stateMachine.vlanId(), EAPOL.EAPOL_PACKET,
                                             eapPayload);
                    this.sendPacketToSupplicant(eth, stateMachine.supplicantConnectpoint());
                    break;
                case RADIUS.RADIUS_CODE_ACCESS_ACCEPT:
                    //send an EAPOL - Success to the supplicant.
                    byte[] eapMessage =
                            radiusPacket.getAttribute(RADIUSAttribute.RADIUS_ATTR_EAP_MESSAGE).getValue();
                    eapPayload = new EAP();
                    eapPayload = (EAP) eapPayload.deserialize(eapMessage, 0, eapMessage.length);
                    eth = buildEapolResponse(stateMachine.supplicantAddress(),
                                             MacAddress.valueOf(1L), stateMachine.vlanId(), EAPOL.EAPOL_PACKET,
                                             eapPayload);
                    this.sendPacketToSupplicant(eth, stateMachine.supplicantConnectpoint());

                    stateMachine.authorizeAccess();
                    break;
                case RADIUS.RADIUS_CODE_ACCESS_REJECT:
                    stateMachine.denyAccess();
                    break;
                default:
                    log.warn("Unknown RADIUS message received with code: {}", radiusPacket.getCode());
            }
        }

        private void sendRadiusMessage(RADIUS radiusMessage) {
            Set<Host> hosts = hostService.getHostsByIp(IpAddress.valueOf(radiusIpAddress));
            Optional<Host> odst = hosts.stream().filter(h -> h.vlan().toShort() == VlanId.UNTAGGED).findFirst();

            Host dst;
            if (!odst.isPresent()) {
                log.info("Radius server {} is not present", radiusIpAddress);
                return;
            } else {
                dst = odst.get();
            }

            UDP udp = new UDP();
            IPv4 ip4Packet = new IPv4();
            Ethernet ethPkt = new Ethernet();
            radiusMessage.setParent(udp);
            udp.setDestinationPort(radiusServerPort);
            udp.setSourcePort(radiusServerPort);
            udp.setPayload(radiusMessage);
            udp.setParent(ip4Packet);
            ip4Packet.setSourceAddress(AAA.this.nasIpAddress.getHostAddress());
            ip4Packet.setDestinationAddress(AAA.this.radiusIpAddress.getHostAddress());
            ip4Packet.setProtocol(IPv4.PROTOCOL_UDP);
            ip4Packet.setPayload(udp);
            ip4Packet.setParent(ethPkt);
            ethPkt.setDestinationMACAddress(radiusMacAddress);
            ethPkt.setSourceMACAddress(nasMacAddress);
            ethPkt.setEtherType(Ethernet.TYPE_IPV4);
            ethPkt.setPayload(ip4Packet);

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(PortNumber.portNumber(radiusPort)).build();
            OutboundPacket packet = new DefaultOutboundPacket(DeviceId.deviceId(radiusSwitch),
                                                              treatment, ByteBuffer.wrap(ethPkt.serialize()));
            packetService.emit(packet);

        }

        /**
         * Send the ethernet packet to the supplicant.
         *
         * @param ethernetPkt  the ethernet packet
         * @param connectPoint the connect point to send out
         */
        private void sendPacketToSupplicant(Ethernet ethernetPkt, ConnectPoint connectPoint) {
            TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(connectPoint.port()).build();
            OutboundPacket packet = new DefaultOutboundPacket(connectPoint.deviceId(),
                                                              treatment, ByteBuffer.wrap(ethernetPkt.serialize()));
            packetService.emit(packet);
        }

    }

    private class InternalConfigListener implements NetworkConfigListener {

        /**
         * Reconfigures the DHCP Server according to the configuration parameters passed.
         *
         * @param cfg configuration object
         */
        private void reconfigureNetwork(AAAConfig cfg) {
            AAAConfig newCfg;
            if (cfg == null) {
                newCfg = new AAAConfig();
            } else {
                newCfg = cfg;
            }
            if (newCfg.nasIp() != null) {
                nasIpAddress = newCfg.nasIp();
            }
            if (newCfg.radiusIp() != null) {
                radiusIpAddress = newCfg.radiusIp();
            }
            if (newCfg.radiusMac() != null) {
                radiusMacAddress = newCfg.radiusMac();
            }
            if (newCfg.nasMac() != null) {
                nasMacAddress = newCfg.nasMac();
            }
            if (newCfg.radiusSecret() != null) {
                radiusSecret = newCfg.radiusSecret();
            }
            if (newCfg.radiusSwitch() != null) {
                radiusSwitch = newCfg.radiusSwitch();
            }
            if (newCfg.radiusPort() != -1) {
                radiusPort = newCfg.radiusPort();
            }
            if (newCfg.radiusServerUDPPort() != -1) {
                radiusServerPort = newCfg.radiusServerUDPPort();
            }
        }

        @Override
        public void event(NetworkConfigEvent event) {

            if ((event.type() == NetworkConfigEvent.Type.CONFIG_ADDED ||
                    event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED) &&
                    event.configClass().equals(AAAConfig.class)) {

                AAAConfig cfg = netCfgService.getConfig(appId, AAAConfig.class);
                reconfigureNetwork(cfg);
                log.info("Reconfigured");
            }
        }
    }


}
