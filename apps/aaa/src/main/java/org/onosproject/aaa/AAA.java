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

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
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
import org.onlab.packet.UDP;
import org.onlab.packet.VlanId;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.PortNumber;
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
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.onosproject.net.packet.PacketPriority.CONTROL;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * AAA application for ONOS.
 */
@Component(immediate = true)
public class AAA {
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

    // for verbose output
    private final Logger log = getLogger(getClass());

    // our application-specific event handler
    private ReactivePacketProcessor processor = new ReactivePacketProcessor();

    // our unique identifier
    private ApplicationId appId;

    // Map of state machines. Each state machine is represented by an
    // unique identifier on the switch: dpid + port number
    Map stateMachineMap = null;

    // RADIUS server IP address
    private static final String DEFAULT_RADIUS_IP = "192.168.1.10";
    // NAS IP address
    private static final String DEFAULT_NAS_IP = "192.168.1.11";
    // RADIUS uplink port
    private static final int DEFAULT_RADIUS_UPLINK = 2;
    // RADIUS server shared secret
    private static final String DEFAULT_RADIUS_SECRET = "ONOSecret";
    // RADIUS MAC address
    private static final String RADIUS_MAC_ADDRESS = "00:00:00:00:01:10";
    // NAS MAC address
    private static final String NAS_MAC_ADDRESS = "00:00:00:00:10:01";
    // Radius Switch Id
    private static final String DEFAULT_RADIUS_SWITCH = "of:90e2ba82f97791e9";
    // Radius Port Number
    private static final String DEFAULT_RADIUS_PORT = "129";

    @Property(name = "radiusIpAddress", value = DEFAULT_RADIUS_IP,
            label = "RADIUS IP Address")
    private String radiusIpAddress = DEFAULT_RADIUS_IP;

    @Property(name = "nasIpAddress", value = DEFAULT_NAS_IP,
            label = "NAS IP Address")
    private String nasIpAddress = DEFAULT_NAS_IP;

    @Property(name = "radiusMacAddress", value = RADIUS_MAC_ADDRESS,
            label = "RADIUS MAC Address")
    private String radiusMacAddress = RADIUS_MAC_ADDRESS;

    @Property(name = "nasMacAddress", value = NAS_MAC_ADDRESS,
            label = "NAS MAC Address")
    private String nasMacAddress = NAS_MAC_ADDRESS;

    @Property(name = "radiusSecret", value = DEFAULT_RADIUS_SECRET,
            label = "RADIUS shared secret")
    private String radiusSecret = DEFAULT_RADIUS_SECRET;

    @Property(name = "radiusSwitchId", value = DEFAULT_RADIUS_SWITCH,
            label = "Radius switch")
    private String radiusSwitch = DEFAULT_RADIUS_SWITCH;

    @Property(name = "radiusPortNumber", value = DEFAULT_RADIUS_PORT,
            label = "Radius port")
    private String radiusPort = DEFAULT_RADIUS_PORT;

    // Parsed RADIUS server IP address
    protected InetAddress parsedRadiusIpAddress;

    // Parsed NAS IP address
    protected InetAddress parsedNasIpAddress;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        String s = Tools.get(properties, "radiusIpAddress");
        try {
            parsedRadiusIpAddress = InetAddress.getByName(s);
            radiusIpAddress = Strings.isNullOrEmpty(s) ? DEFAULT_RADIUS_IP : s;
        } catch (UnknownHostException e) {
            log.error("Invalid RADIUS IP address specification: {}", s);
        }
        try {
            s = Tools.get(properties, "nasIpAddress");
            parsedNasIpAddress = InetAddress.getByName(s);
            nasIpAddress = Strings.isNullOrEmpty(s) ? DEFAULT_NAS_IP : s;
        } catch (UnknownHostException e) {
            log.error("Invalid NAS IP address specification: {}", s);
        }

        s = Tools.get(properties, "radiusMacAddress");
        radiusMacAddress = Strings.isNullOrEmpty(s) ? RADIUS_MAC_ADDRESS : s;

        s = Tools.get(properties, "nasMacAddress");
        nasMacAddress = Strings.isNullOrEmpty(s) ? NAS_MAC_ADDRESS : s;

        s = Tools.get(properties, "radiusSecret");
        radiusSecret = Strings.isNullOrEmpty(s) ? DEFAULT_RADIUS_SECRET : s;

        s = Tools.get(properties, "radiusSwitchId");
        radiusSwitch = Strings.isNullOrEmpty(s) ? DEFAULT_RADIUS_SWITCH : s;

        s = Tools.get(properties, "radiusPortNumber");
        radiusPort = Strings.isNullOrEmpty(s) ? DEFAULT_RADIUS_PORT : s;
    }

    @Activate
    public void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());
        modified(context);
        // "org.onosproject.aaa" is the FQDN of our app
        appId = coreService.registerApplication("org.onosproject.aaa");
        // register our event handler
        packetService.addProcessor(processor, PacketProcessor.ADVISOR_MAX + 2);
        requestIntercepts();
        // Instantiate the map of the state machines
        stateMachineMap = Collections.synchronizedMap(Maps.newHashMap());

        hostService.startMonitoringIp(IpAddress.valueOf(radiusIpAddress));

    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);

        appId = coreService.registerApplication("org.onosproject.aaa");
        withdrawIntercepts();
        // de-register and null our handler
        packetService.removeProcessor(processor);
        processor = null;
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
                .matchUdpDst((short) 1812)
                .matchUdpSrc((short) 1812)
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
                .matchUdpDst((short) 1812)
                .matchUdpSrc((short) 1812)
                .build();
        packetService.cancelPackets(radSelector, CONTROL, appId);
    }

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
            // identify if incoming packet comes from supplicant (EAP) or RADIUS
            switch (EthType.EtherType.lookup(ethPkt.getEtherType())) {
                case EAPOL:
                    handleSupplicantPacket(context.inPacket());
                    break;
                case IPV4:
                    IPv4 ipv4Packet = (IPv4) ethPkt.getPayload();
                    Ip4Address srcIp = Ip4Address.valueOf(ipv4Packet.getSourceAddress());
                    Ip4Address radiusIp4Address = Ip4Address.valueOf(parsedRadiusIpAddress);
                    if (srcIp.equals(radiusIp4Address) && ipv4Packet.getProtocol() == IPv4.PROTOCOL_UDP) {
                        // TODO: check for port as well when it's configurable
                        UDP udpPacket = (UDP) ipv4Packet.getPayload();

                        byte[] datagram = udpPacket.getPayload().serialize();
                        RADIUS radiusPacket;
                        try {
                            radiusPacket = RADIUS.deserializer().deserialize(datagram, 0, datagram.length);
                        } catch (DeserializationException e) {
                            log.warn("Unable to deserialize RADIUS packet:", e);
                            return;
                        }
                        handleRadiusPacket(radiusPacket);
                    }
                    break;
                default:
                    return;
            }
        }


        /**
         * Handles PAE packets (supplicant).
         *
         * @param inPacket Ethernet packet coming from the supplicant
         */
        private void handleSupplicantPacket(InboundPacket inPacket) {
            Ethernet ethPkt = inPacket.parsed();
            // Where does it come from?
            MacAddress srcMAC = ethPkt.getSourceMAC();

            DeviceId deviceId = inPacket.receivedFrom().deviceId();
            PortNumber portNumber = inPacket.receivedFrom().port();
            String sessionId = deviceId.toString() + portNumber.toString();
            StateMachine stateMachine = getStateMachine(sessionId);

            EAPOL eapol = (EAPOL) ethPkt.getPayload();

            switch (eapol.getEapolType()) {
                case EAPOL.EAPOL_START:
                    try {
                        stateMachine.start();
                        stateMachine.supplicantConnectpoint = inPacket.receivedFrom();

                        //send an EAP Request/Identify to the supplicant
                        EAP eapPayload = new EAP(EAP.REQUEST, stateMachine.getIdentifier(), EAP.ATTR_IDENTITY, null);
                        Ethernet eth = buildEapolResponse(srcMAC, MacAddress.valueOf(1L),
                                                          ethPkt.getVlanID(), EAPOL.EAPOL_PACKET,
                                                          eapPayload);
                        stateMachine.supplicantAddress = srcMAC;
                        stateMachine.vlanId = ethPkt.getVlanID();

                        this.sendPacketToSupplicant(eth, stateMachine.supplicantConnectpoint);
                    } catch (StateMachineException e) {
                        e.printStackTrace();
                    }

                    break;
                case EAPOL.EAPOL_PACKET:
                    //check if this is a Response/Identify or  a Response/TLS
                    EAP eapPacket = (EAP) eapol.getPayload();

                    byte dataType = eapPacket.getDataType();
                    switch (dataType) {
                        case EAP.ATTR_IDENTITY:
                            try {
                                //request id access to RADIUS
                                RADIUS radiusPayload = new RADIUS(RADIUS.RADIUS_CODE_ACCESS_REQUEST,
                                                                  eapPacket.getIdentifier());
                                radiusPayload.setIdentifier(stateMachine.getIdentifier());
                                radiusPayload.setAttribute(RADIUSAttribute.RADIUS_ATTR_USERNAME,
                                                           eapPacket.getData());
                                stateMachine.setUsername(eapPacket.getData());
                                radiusPayload.setAttribute(RADIUSAttribute.RADIUS_ATTR_NAS_IP,
                                                           AAA.this.parsedNasIpAddress.getAddress());

                                radiusPayload.encapsulateMessage(eapPacket);

                                // set Request Authenticator in StateMachine
                                stateMachine.setRequestAuthenticator(radiusPayload.generateAuthCode());
                                radiusPayload.addMessageAuthenticator(AAA.this.radiusSecret);
                                sendRadiusMessage(radiusPayload);

                                //change the state to "PENDING"
                                stateMachine.requestAccess();
                            } catch (StateMachineException e) {
                                e.printStackTrace();
                            }
                            break;
                        case EAP.ATTR_MD5:
                            //verify if the EAP identifier corresponds to the challenge identifier from the client state
                            //machine.
                            if (eapPacket.getIdentifier() == stateMachine.getChallengeIdentifier()) {
                                //send the RADIUS challenge response
                                RADIUS radiusPayload = new RADIUS(RADIUS.RADIUS_CODE_ACCESS_REQUEST,
                                                                  eapPacket.getIdentifier());
                                radiusPayload.setIdentifier(stateMachine.getChallengeIdentifier());
                                radiusPayload.setAttribute(RADIUSAttribute.RADIUS_ATTR_USERNAME,
                                                           stateMachine.getUsername());
                                radiusPayload.setAttribute(RADIUSAttribute.RADIUS_ATTR_NAS_IP,
                                                           AAA.this.parsedNasIpAddress.getAddress());

                                radiusPayload.encapsulateMessage(eapPacket);

                                radiusPayload.setAttribute(RADIUSAttribute.RADIUS_ATTR_STATE,
                                                           stateMachine.getChallengeState());
                                radiusPayload.addMessageAuthenticator(AAA.this.radiusSecret);
                                sendRadiusMessage(radiusPayload);
                            }
                            break;
                        case EAP.ATTR_TLS:
                            try {
                                //request id access to RADIUS
                                RADIUS radiusPayload = new RADIUS(RADIUS.RADIUS_CODE_ACCESS_REQUEST,
                                                                  eapPacket.getIdentifier());
                                radiusPayload.setIdentifier(stateMachine.getIdentifier());
                                radiusPayload.setAttribute(RADIUSAttribute.RADIUS_ATTR_USERNAME,
                                                           stateMachine.getUsername());
                                radiusPayload.setAttribute(RADIUSAttribute.RADIUS_ATTR_NAS_IP,
                                                           AAA.this.parsedNasIpAddress.getAddress());

                                radiusPayload.encapsulateMessage(eapPacket);

                                radiusPayload.setAttribute(RADIUSAttribute.RADIUS_ATTR_STATE,
                                                           stateMachine.getChallengeState());
                                stateMachine.setRequestAuthenticator(radiusPayload.generateAuthCode());

                                radiusPayload.addMessageAuthenticator(AAA.this.radiusSecret);

                                sendRadiusMessage(radiusPayload);
                                // TODO: this gets called on every fragment, should only be called at TLS-Start
                                stateMachine.requestAccess();
                            } catch (StateMachineException e) {
                                e.printStackTrace();
                            }
                            break;
                        default:
                            return;
                    }
                    break;
                default:
                    return;
            }
        }

        /**
         * Handles RADIUS packets.
         *
         * @param radiusPacket RADIUS packet coming from the RADIUS server.
         */
        private void handleRadiusPacket(RADIUS radiusPacket) {
            StateMachine stateMachine = getStateMachineById(radiusPacket.getIdentifier());
            if (stateMachine == null) {
                log.error("Invalid session identifier, exiting...");
                return;
            }

            EAP eapPayload = new EAP();
            Ethernet eth = null;
            switch (radiusPacket.getCode()) {
                case RADIUS.RADIUS_CODE_ACCESS_CHALLENGE:
                    byte[] challengeState = radiusPacket.getAttribute(RADIUSAttribute.RADIUS_ATTR_STATE).getValue();
                    eapPayload = radiusPacket.decapsulateMessage();
                    stateMachine.setChallengeInfo(eapPayload.getIdentifier(), challengeState);
                    eth = buildEapolResponse(stateMachine.supplicantAddress,
                                             MacAddress.valueOf(1L), stateMachine.vlanId, EAPOL.EAPOL_PACKET,
                                             eapPayload);
                    this.sendPacketToSupplicant(eth, stateMachine.supplicantConnectpoint);
                    break;
                case RADIUS.RADIUS_CODE_ACCESS_ACCEPT:
                    try {
                        //send an EAPOL - Success to the supplicant.
                        byte[] eapMessage =
                                radiusPacket.getAttribute(RADIUSAttribute.RADIUS_ATTR_EAP_MESSAGE).getValue();
                        eapPayload = new EAP();
                        eapPayload = (EAP) eapPayload.deserialize(eapMessage, 0, eapMessage.length);
                        eth = buildEapolResponse(stateMachine.supplicantAddress,
                                                 MacAddress.valueOf(1L), stateMachine.vlanId, EAPOL.EAPOL_PACKET,
                                                 eapPayload);
                        this.sendPacketToSupplicant(eth, stateMachine.supplicantConnectpoint);

                        stateMachine.authorizeAccess();
                    } catch (StateMachineException e) {
                        e.printStackTrace();
                    }
                    break;
                case RADIUS.RADIUS_CODE_ACCESS_REJECT:
                    try {
                        stateMachine.denyAccess();
                    } catch (StateMachineException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    log.warn("Unknown RADIUS message received with code: {}", radiusPacket.getCode());
            }
        }

        private StateMachine getStateMachineById(byte identifier) {
            StateMachine stateMachine = null;
            Set stateMachineSet = stateMachineMap.entrySet();

            synchronized (stateMachineMap) {
                Iterator itr = stateMachineSet.iterator();
                while (itr.hasNext()) {
                    Map.Entry entry = (Map.Entry) itr.next();
                    stateMachine = (StateMachine) entry.getValue();
                    if (identifier == stateMachine.getIdentifier()) {
                        //the state machine has already been created for this session session
                        stateMachine = (StateMachine) entry.getValue();
                        break;
                    }
                }
            }

            return stateMachine;
        }

        private StateMachine getStateMachine(String sessionId) {
            StateMachine stateMachine = null;
            Set stateMachineSet = stateMachineMap.entrySet();

            synchronized (stateMachineMap) {
                Iterator itr = stateMachineSet.iterator();
                while (itr.hasNext()) {

                    Map.Entry entry = (Map.Entry) itr.next();
                    if (sessionId.equals(entry.getKey())) {
                        //the state machine has already been created for this session session
                        stateMachine = (StateMachine) entry.getValue();
                        break;
                    }
                }
            }

            if (stateMachine == null) {
                stateMachine = new StateMachine(sessionId, voltTenantService);
                stateMachineMap.put(sessionId, stateMachine);
            }

            return stateMachine;
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
            udp.setDestinationPort((short) 1812);
            udp.setSourcePort((short) 1812); // TODO: make this configurable
            udp.setPayload(radiusMessage);
            udp.setParent(ip4Packet);
            ip4Packet.setSourceAddress(AAA.this.nasIpAddress);
            ip4Packet.setDestinationAddress(AAA.this.radiusIpAddress);
            ip4Packet.setProtocol(IPv4.PROTOCOL_UDP);
            ip4Packet.setPayload(udp);
            ip4Packet.setParent(ethPkt);
            ethPkt.setDestinationMACAddress(radiusMacAddress);
            ethPkt.setSourceMACAddress(nasMacAddress);
            ethPkt.setEtherType(Ethernet.TYPE_IPV4);
            ethPkt.setPayload(ip4Packet);

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(PortNumber.portNumber(Integer.parseInt(radiusPort))).build();
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

}
