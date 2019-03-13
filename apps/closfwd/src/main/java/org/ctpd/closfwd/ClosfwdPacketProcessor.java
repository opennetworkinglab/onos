package org.ctpd.closfwd;

import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.ctpd.closfwd.MacResponse;

import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.packet.*;
import org.onlab.packet.ndp.NeighborAdvertisement;
import org.onlab.packet.ndp.NeighborDiscoveryOptions;
import org.onlab.packet.ndp.NeighborSolicitation;
import org.onosproject.net.ConnectPoint;

import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.*;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

// import static org.ctpd.closfwd.MacResponse.TO_SERVICE;
// import static org.ctpd.closfwd.MacResponse.TO_VPDC;

public class ClosfwdPacketProcessor implements PacketProcessor{

    /*Pedro*/
    public static <T> T get(Class<T> serviceClass) {
        return DefaultServiceDirectory.getService(serviceClass);
    }

    private final Logger log = LoggerFactory.getLogger("log4j.logger.org.ctpdLogger");

    /*Pedro*/

    @Override
    public void process(PacketContext context) {


        ClosDeviceService service = get(ClosDeviceService.class);

        InboundPacket pkt = context.inPacket();
        Ethernet ethPkt = pkt.parsed();
        // log.debug("pkt: "+ethPkt);

        String ctpdIpv4 = service.getCtpdIpv4();
        String ctpdIpv6 = service.getCtpdIpv6();

        Ip6Address ip6AdressCtpd = Ip6Address.valueOf(ctpdIpv6);
        Ip4Address ip4AdressCtpd = Ip4Address.valueOf(ctpdIpv4);


		// /*  Filter for desired packets, NDP and ARP */

        // if (context.isHandled()) {
        //     return;
        // }

        // Skip not ethernet packages...
        if (ethPkt == null) {
            log.debug("[process] Rule not installed: Packet is not Ethernet");
            return;
        }

        /* Bail if this is deemed to be a control packet. */
        if (isControlPacket(ethPkt))
            return;

        // String[] openFlowSwitches=service.getOpenFlowSwitches(); /*Pedro*/

		/*	L4 Filter */
        DeviceId deviceIdReceivedFrom = context.inPacket().receivedFrom().deviceId();
        // if (!deviceIdReceivedFrom.equals(DeviceId.deviceId(openFlowSwitches[3]))) { /*Pedro*/
        //     if (!deviceIdReceivedFrom.equals(DeviceId.deviceId(openFlowSwitches[2]))) {/*Pedro*/
        //         if (!deviceIdReceivedFrom.equals(DeviceId.deviceId(openFlowSwitches[1]))) {/*Pedro*/
        //             if (!deviceIdReceivedFrom.equals(DeviceId.deviceId(openFlowSwitches[0]))) {/*Pedro*/
        //                 log.warn("[process] Device is not L4, L3, L2, L1");
        //                 return;
        //             }
        //         }
        //     }
        // }

        /*  Triggers from L1 are because bypass scenarios */
        if (deviceIdReceivedFrom.toString().startsWith(service.getOpenFlowL1Preffix())) {
            log.debug("[process] Processing L1 request");
            if ( (ethPkt.getEtherType() == Ethernet.TYPE_IPV6) ||
                    (ethPkt.getEtherType() == Ethernet.TYPE_IPV4)) {
                log.debug("[process] Processing IPv4 or IPv6 Vlan request");
                IP ipPkt = (IP) ethPkt.getPayload();
                Thread t1 = new Thread(new Runnable() {
                    public void run() {
                        processClientToService(context, ethPkt, ipPkt);
                    }
                });
                t1.start();
            }
            return;
        }

        // ARP/NDP case ...
        if (ethPkt.getEtherType() == Ethernet.TYPE_ARP) {
            ARP arpPkt = (ARP) ethPkt.getPayload();
            if (arpPkt.getOpCode() == ARP.OP_REQUEST) {
                processARPRequest(context, arpPkt, ethPkt);
                return;
            }
        }
        else if (ethPkt.getEtherType() == Ethernet.TYPE_IPV4){
            IPv4 ipv4Pkt = (IPv4) ethPkt.getPayload();
            if(ipv4Pkt.getProtocol() == IPv4.PROTOCOL_ICMP) {
                ICMP icmpPkt = (ICMP) ipv4Pkt.getPayload();
                if(icmpPkt.getIcmpType() == ICMP.TYPE_ECHO_REQUEST) {
                    if(ip4AdressCtpd.equals(Ip4Address.valueOf(ipv4Pkt.getDestinationAddress()))) {
                        log.debug("Processing ICMP4 Echo request packet");
                        sendIcmp4Response(ethPkt, context.inPacket().receivedFrom());
                        return;
                    }
                }
            }
        }
        else if (ethPkt.getEtherType() == Ethernet.TYPE_IPV6) {

            IPv6 ipv6Pkt = (IPv6) ethPkt.getPayload();

            if (ipv6Pkt.getNextHeader() == IPv6.PROTOCOL_ICMP6) {
                ICMP6 icmp6Pkt = (ICMP6) ipv6Pkt.getPayload();
                // log.debug("Type of request "+ icmp6Pkt.getIcmpType());

                if (icmp6Pkt.getIcmpType() == ICMP6.NEIGHBOR_SOLICITATION) { /* Handle only Neigbour Solicitations */
                    processNDPNeigbourSolicitation(context, ethPkt, (NeighborSolicitation) icmp6Pkt.getPayload());
                    return;
                } else if (icmp6Pkt.getIcmpType() == ICMP6.NEIGHBOR_ADVERTISEMENT) {
                    processNDPNeigbourAdvertisement(context, ethPkt, (NeighborAdvertisement) icmp6Pkt.getPayload());
                    return;
                } else if (icmp6Pkt.getIcmpType() == ICMP6.ECHO_REQUEST) {
                    if(ip6AdressCtpd.equals(Ip6Address.valueOf(ipv6Pkt.getDestinationAddress()))) {
                        log.debug("Processing ICMP6 Echo request packet");
                        sendIcmp6Response(ethPkt, context.inPacket().receivedFrom());
                        return;
                    }
                }
                else {
                    log.warn("Unhandled ICMP "+Integer.toHexString(icmp6Pkt.getIcmpType())+" received in "+deviceIdReceivedFrom.toString()+" from "+Ip6Address.valueOf(ipv6Pkt.getSourceAddress())
                        + " to " + Ip6Address.valueOf(ipv6Pkt.getDestinationAddress())+" with srcMac "+MacAddress.valueOf(ethPkt.getSourceMACAddress())
                        + " and dstMAC "+MacAddress.valueOf(ethPkt.getDestinationMACAddress()));
                }
            }
        }
    }

    /* Indicates whether this is a control packet, e.g. LLDP, BDDP */
    private boolean isControlPacket(Ethernet eth) {
        short type = eth.getEtherType();
        return type == Ethernet.TYPE_LLDP || type == Ethernet.TYPE_BSN;
    }

    public void processClientToService(PacketContext context, Ethernet ethPkt, IP ipPkt) {
        log.debug("[processClientToService] start");

        ClosDeviceService service = get(ClosDeviceService.class);
        ConsistentMap<UUID, Endpoint> registry = service.getRegistry();
        int vpdcClientPrefixlength = service.getVpdcClientPrefixlength();
        IpPrefix srcIpPrefix;

        IpAddress srcIp, dstIp;

        if(ipPkt.getVersion() == 6){
            IPv6 ip6Pkt = (IPv6) ipPkt;
            srcIp = IpAddress.valueOf(IpAddress.Version.INET6, ip6Pkt.getSourceAddress());
            srcIpPrefix = IpPrefix.valueOf(srcIp, vpdcClientPrefixlength);

            dstIp = IpAddress.valueOf(IpAddress.Version.INET6, ip6Pkt.getDestinationAddress());
            //IpPrefix dstIpPrefix = IpPrefix.valueOf(dstIp, 128);
        }
        else {
            IPv4 ip4Pkt = (IPv4) ipPkt;
            srcIp = IpAddress.valueOf(IpAddress.Version.INET, IPv4.toIPv4AddressBytes(ip4Pkt.getSourceAddress()));
            srcIpPrefix = IpPrefix.valueOf(srcIp, vpdcClientPrefixlength);

            dstIp = IpAddress.valueOf(IpAddress.Version.INET, IPv4.toIPv4AddressBytes(ip4Pkt.getDestinationAddress()));
            //IpPrefix dstIpPrefix = IpPrefix.valueOf(dstIp, 128);
        }

        PortNumber inputPort = context.inPacket().receivedFrom().port();
        DeviceId deviceId = context.inPacket().receivedFrom().deviceId();
        String logs = String.format("[processClientToService] d:%s srcIp:%s dstIp:%s", deviceId, srcIp, dstIp);
        log.debug(logs);

        log.debug("Checking bypass endpoint...");

        ClientServiceBypassEndpoint clientEndpoint = service.getBypassEndpointFromIp(srcIp);

        if (clientEndpoint != null) {
            log.debug("Bypass endpoint found!");
            ArrayList<UUID> services = clientEndpoint.getServiceUUIDs();
            for(UUID serviceUUID: services)
            {
                if(serviceUUID!=null)
                {
                    log.debug("Service UUID found in bypass endpoint!");
                    Endpoint serviceEndpoint = service.getEndpoint(serviceUUID);
                    if(serviceEndpoint != null)
                    {
                        log.debug("Service endpoint found in bypass endpoint!");
                        if(serviceEndpoint.getIpPrefix().contains(dstIp))
                        {
                            service.getDriver().installBypassTemporaryFlows(clientEndpoint, (ServiceEndpoint) serviceEndpoint, ethPkt.getSourceMAC(), srcIpPrefix, true);
                            return;
                        }
                    }
                }
            }
        }

        // /*  Retrieve Client and Service from storage */
        // Endpoint client = getClientFromIp(srcIp);
        // if (client == null) {
        //     log.warn("[processClientToService] client == null");
        //     return;
        // }

        // List<Servicio> servicios = getServiceFromIp(dstIp);
        // for(Servicio servicio: servicios)
        // {
        //     // if (servicio == null) {
        //     //     log.warn("[processClientToService] servicio == null");
        //     //     return;
        //     // }

        //     /* Add Client MacAddress to Registry */
        //     UUID clientUuid = service.getClientUUID(client);
        //     log.debug("[processClientToService] clientUuid = {}", clientUuid);

        //     // client.mac = ethPkt.getSourceMAC();
        //     // registry.put(clientUuid, client);


        //     //TODO: Process result, define first how to process. Idea: Event to Backend
        // }
        /*  Send to pipeline */
        packetOut(ethPkt, deviceId, PortNumber.TABLE);
        log.debug("[processClientToService] end");

    }

    public void processNDPNeigbourSolicitation(PacketContext context , Ethernet requestEthPkt, NeighborSolicitation neigbourSolicitationPkt) {

        DeviceId deviceId = context.inPacket().receivedFrom().deviceId();
        PortNumber port = context.inPacket().receivedFrom().port();
        MacAddress srcMac = requestEthPkt.getSourceMAC();
        MacAddress dstMac = requestEthPkt.getDestinationMAC();
        IPv6 ipv6Pkt = (IPv6) requestEthPkt.getPayload();
        Ip6Address srcIp = Ip6Address.valueOf(ipv6Pkt.getSourceAddress());
        Ip6Address dstIp = Ip6Address.valueOf(ipv6Pkt.getDestinationAddress());
        Ip6Address targetIp = Ip6Address.valueOf(neigbourSolicitationPkt.getTargetAddress());
        VlanId vlan = VlanId.vlanId(requestEthPkt.getVlanID());
        VlanId vlanQinQ = VlanId.vlanId(requestEthPkt.getQinQVID());

        ClosDeviceService service = get(ClosDeviceService.class);
        // String[] openFlowSwitches=service.getOpenFlowSwitches();
        String ctpdFakeInternalMacAddress = service.getCtpdFakeInternalMacAddress();
        // String ctpdFakeExternalMacAddress = service.getCtpdFakeExternalMacAddress();

        String packetDescription = String.format("[processNDPNeigbourSolicitation] D:%s P:%s NS vlan:%s valnQinQ:%s scrMac:%s dstMac:%s srcIp:%s dstIp:%s trgtIp:%s", deviceId, port, vlan, vlanQinQ, srcMac, dstMac, srcIp, dstIp, targetIp);
        log.debug(packetDescription);

        // boolean isLeaf2 = false;
        // if (deviceId.equals(DeviceId.deviceId(openFlowSwitches[1]))) {
        //     isLeaf2 = true;
        // }

		/*	Choose to respond with Internal or External Fake Ctpd Mac or even drop packet */
        Ethernet ndpReplyPkt = null;
        // IpPrefix prefix = null;
        switch (selectTypeofResponseToRequest(targetIp, srcIp, requestEthPkt.getSourceMAC(), VlanId.vlanId(requestEthPkt.getVlanID()), deviceId, port)) { //, isLeaf2)) {
            case MacResponse.USE_INT_CTPD_FAKE_MAC:
				/*  Build NDP Reply, the returned mac address will be the Fake Internal Ctpd Mac Address */
                ndpReplyPkt = buildNdpReply(targetIp, MacAddress.valueOf(ctpdFakeInternalMacAddress), requestEthPkt);
				/*  Respond via the same input port and vlan it was received */
                packetOut(ndpReplyPkt, deviceId, port);
                break;
            // case MacResponse.USE_EXT_CTPD_FAKE_MAC:
			// 	/*  Build NDP Reply, the returned mac address will be the Fake External Ctpd Mac Address */
            //     ndpReplyPkt = buildNdpReply(targetIp, MacAddress.valueOf(ctpdFakeExternalMacAddress), requestEthPkt);
			// 	/*  Respond via the same input port and vlan it was received */
            //     packetOut(ndpReplyPkt, deviceId, port);
            //     break;
            case MacResponse.DROP:
                log.debug("[processNDPNeigbourSolicitation] Dropping the NDP Request");
                break;
            default:
                log.warn("[processNDPNeigbourSolicitation] No use case matching this Request");
                break;
        }
    }

    /*  Respond to all requests with Fake Ctpd Mac Address*/

    public void processARPRequest(PacketContext context, ARP arpPkt, Ethernet requestEthPkt) {
        DeviceId deviceId = context.inPacket().receivedFrom().deviceId();
        PortNumber port = context.inPacket().receivedFrom().port();
        MacAddress srcMac = requestEthPkt.getSourceMAC();
        MacAddress dstMac = requestEthPkt.getDestinationMAC();
        Ip4Address srcIp = Ip4Address.valueOf(arpPkt.getSenderProtocolAddress());
        Ip4Address targetIp = Ip4Address.valueOf(arpPkt.getTargetProtocolAddress());

        ClosDeviceService service = get(ClosDeviceService.class);
        // String[] openFlowSwitches=service.getOpenFlowSwitches();
        String ctpdFakeInternalMacAddress = service.getCtpdFakeInternalMacAddress();
        // String ctpdFakeExternalMacAddress = service.getCtpdFakeExternalMacAddress();

        String packetDescription = String.format("[processARPRequest] D:%s P:%s ARP scrMac:%s dstMac:%s srcIp:%s trgtIp:%s", deviceId, port, srcMac, dstMac, srcIp, targetIp);
        log.debug(packetDescription);

        // boolean isLeaf2 = false;
        // if (deviceId.equals(DeviceId.deviceId(openFlowSwitches[1]))) {
        //     isLeaf2 = true;
        // }

		/*	Choose to respond with Internal or External Fake Ctpd Mac or even drop packet */
        Ethernet arpReplyPkt = null;
        switch (selectTypeofResponseToRequest(targetIp, srcIp, requestEthPkt.getSourceMAC(), VlanId.vlanId(requestEthPkt.getVlanID()), deviceId, port)) { //,isLeaf2)) {
            case MacResponse.USE_INT_CTPD_FAKE_MAC:
				/*  Build ARP Reply, the mac returned will be the Fake Internal Ctpd Mac Address */
                arpReplyPkt = ARP.buildArpReply(targetIp, MacAddress.valueOf(ctpdFakeInternalMacAddress), requestEthPkt);
				/*  Respond via the same input port and vlan it was received */
                packetOut(arpReplyPkt, deviceId, port);
                break;
            // case MacResponse.USE_EXT_CTPD_FAKE_MAC:
			// 	/*  Build ARP Reply, the mac returned will be the Fake External Ctpd Mac Address */
            //     arpReplyPkt = ARP.buildArpReply(targetIp, MacAddress.valueOf(ctpdFakeExternalMacAddress), requestEthPkt);
			// 	/*  Respond via the same input port and vlan it was received */
            //     packetOut(arpReplyPkt, deviceId, port);
            //     break;
            case MacResponse.DROP:
                log.debug("[processARPRequest] Dropping the ARP Request");
                break;
            default:
                log.warn("[processARPRequest] No use case matching this Request");
                break;
        }
    }

    public void processNDPNeigbourAdvertisement(PacketContext context, Ethernet replyEthRequest, NeighborAdvertisement neigbourAdvertisementPkt) {
        log.debug("[processNDPNeigbourAdvertisement] Processing NDP Neigbour-Advertisement packet");

        // TO DO: Support multiple external services...

        // ClosDeviceService service = get(ClosDeviceService.class);
        // String[] openFlowSwitches=service.getOpenFlowSwitches();
        // ConsistentMap<UUID, BaseDevice> registry = service.getRegistry();

		// /*	We only want Neighbour Advertisements from L4 */
        // if (context.inPacket().receivedFrom().deviceId().equals(DeviceId.deviceId(openFlowSwitches[3]))) {
        //     Router router = getRouterDevice();
        //     Servicio externalService = getExternalServiceDevice();

        //     Ip6Address targetIp = Ip6Address.valueOf(neigbourAdvertisementPkt.getTargetAddress());
        //     if (targetIp.equals(router.getIpAddress())) {/*	The only packet generated with this ip is the Neighbour Solicitation */
		// 		/*	Get Internet Router MAC address */
        //         MacAddress newInternetRouterMacAddress = replyEthRequest.getSourceMAC();

		// 		/*	Replace fake External Service mac address if it has changed*/
        //         if (!newInternetRouterMacAddress.equals(externalService.mac)) {
        //             log.info("[processNDPNeigbourAdvertisement] External Router MAC has changed");

		// 			/*	Save new Mac to External Service Device */
        //             externalService.mac = newInternetRouterMacAddress;
        //             UUID externalServiceId = service.addDevice(externalService);
        //             registry.put(externalServiceId, externalService);
        //         }
        //     }
        // }
    }


    private int selectTypeofResponseToRequest(IpAddress targetIp, IpAddress srcIp, MacAddress srcMac, VlanId vlan, DeviceId deviceId, PortNumber port){ //, boolean isLeaf2) {

        ClosDeviceService service = get(ClosDeviceService.class);
        ConsistentMap<UUID, Endpoint> registry = service.getRegistry();
        // DistributedSet<MacAddress> externalServicesMacAddresses = service.getExternalServicesMacAddresses();
        ConsistentMap<IpPrefix, MacAddress> hostMacMap = service.getHostMacMap();
        boolean respondNDPLocally = service.getrespondNDPLocally();
        boolean checkLocalNDP = service.getcheckLocalNDP();
        String ctpdIpv4 = service.getCtpdIpv4();
        String ctpdIpv6 = service.getCtpdIpv6();
        String bgpCtpdIpv4 = service.getBgpCtpdIpv4();
        String bgpCtpdIpv6 = service.getBgpCtpdIpv6();

        log.debug("getVpdcInternalPrefix "+service.getVpdcInternalPrefix());
        IpPrefix vpdcInternalPrefix = IpPrefix.valueOf(service.getVpdcInternalPrefix());
        IpPrefix vpdcClientPrefix = IpPrefix.valueOf(service.getVpdcClientPrefix());
        String ctpdMacPrefix = service.getCtpdMacPrefix();
        Ip6Address ip6AdressCtpd = Ip6Address.valueOf(ctpdIpv6);
        Ip4Address ip4AdressCtpd = Ip4Address.valueOf(ctpdIpv4);

        IpAddress bgpIpv4Address = IpAddress.valueOf(bgpCtpdIpv4);
        IpAddress bgpIpv6Address = IpAddress.valueOf(bgpCtpdIpv6);

		/*	Process Source Mac instead of source Ip if the Source Ip is Link Local */
        if (respondNDPLocally) { // && isLeaf2) {
            return MacResponse.USE_INT_CTPD_FAKE_MAC;
        } else {
            if (registry.isEmpty()) {
                return MacResponse.DROP;
            }
            // boolean isSrcOutsideCtpd  = externalServicesMacAddresses.contains(srcMac);

            boolean isSrcOutsideCtpd = true;

            if(srcMac.toString().startsWith(ctpdMacPrefix))
                isSrcOutsideCtpd = false;

            // If requests comes from outside ctpd...
            if (isSrcOutsideCtpd) {
                log.debug("Request from outside CTPD");
                if(targetIp.isIp6()){
                    ip6AdressCtpd = Ip6Address.valueOf(ctpdIpv6);
                    if (ip6AdressCtpd.equals(targetIp) || bgpIpv6Address.equals(targetIp))
                        return MacResponse.USE_INT_CTPD_FAKE_MAC;
                    else
                        return MacResponse.DROP;
                }
                else {
                    if(targetIp.isIp4()){
                        if (ip4AdressCtpd.equals(targetIp) || bgpIpv4Address.equals(targetIp))
                            return MacResponse.USE_INT_CTPD_FAKE_MAC;
                        else
                            return MacResponse.DROP;
                    }
                }
            }

            // If requests comes from inside CTPD...
            if(targetIp.isIp6())
            {
                ip6AdressCtpd = Ip6Address.valueOf(ctpdIpv6);
                log.debug("Checking ping IPs equals {} with {}",ip6AdressCtpd,targetIp);
                if (ip6AdressCtpd.equals(targetIp)){
                    return MacResponse.USE_INT_CTPD_FAKE_MAC;
                }
                else if (checkLocalNDP){
                    IpPrefix prefix = IpPrefix.valueOf(targetIp, 128);
                    Versioned<MacAddress> match = hostMacMap.get(prefix);
                    if(match != null && vlan.toShort() > 0) {
                        if(checkPortMatch(match.value(), deviceId, port))
                        {
                            // Is is a request about VPDC address? If so, we always answer...
                            if(vpdcClientPrefix.contains(targetIp) || vpdcInternalPrefix.contains(targetIp)) {
                                return MacResponse.USE_INT_CTPD_FAKE_MAC;
                            }
                            else {
                                log.debug("Dropping NDP request for "+ targetIp.toString());
                                return MacResponse.DROP;
                            }
                        }
                    }
                }
            } else {
                log.debug("Checking ping IPs equals {} with {}",ip4AdressCtpd,targetIp);
                if (ip4AdressCtpd.equals(targetIp))
                    return MacResponse.USE_INT_CTPD_FAKE_MAC;
                else if (checkLocalNDP){
                    IpPrefix prefix = IpPrefix.valueOf(targetIp, 32);
                    Versioned<MacAddress> match = hostMacMap.get(prefix);
                    if(match != null && vlan.toShort() > 0)
                    {
                        if(checkPortMatch(match.value(), deviceId, port)){
                            log.debug("Dropping ARP request for "+ targetIp.toString());
                            return MacResponse.DROP;
                        }
                    }
                }
            }
        }
        // Default
        return MacResponse.USE_INT_CTPD_FAKE_MAC;
    }

    private boolean checkPortMatch(MacAddress mac, DeviceId deviceId,PortNumber port){
        byte devByte1 = (byte) Short.parseShort(deviceId.toString().substring(17));
		byte devByte2 = (byte) Short.parseShort(deviceId.toString().substring(15, 17));
		byte devByte3 = (byte) Short.parseShort(deviceId.toString().substring(13, 15));

        if (mac.toBytes()[5]==port.toLong())
            if(mac.toBytes()[4]== devByte1 && mac.toBytes()[3]== devByte2 && mac.toBytes()[2]==devByte3)
                return true;
        return false;
    }


    private Ethernet buildNdpReply(Ip6Address srcIp, MacAddress srcMac, Ethernet request) {
        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress(request.getSourceMAC());
        eth.setSourceMACAddress(srcMac);
        eth.setEtherType(Ethernet.TYPE_IPV6);
        eth.setVlanID(request.getVlanID());

        IPv6 requestIp = (IPv6) request.getPayload();
        IPv6 ipv6 = new IPv6();
        ipv6.setSourceAddress(srcIp.toOctets());
        ipv6.setDestinationAddress(requestIp.getSourceAddress());
        ipv6.setHopLimit((byte) 255);

        ICMP6 icmp6 = new ICMP6();
        icmp6.setIcmpType(ICMP6.NEIGHBOR_ADVERTISEMENT);
        icmp6.setIcmpCode((byte) 0);

        NeighborAdvertisement nadv = new NeighborAdvertisement();
        nadv.setTargetAddress(srcIp.toOctets());
        nadv.setSolicitedFlag((byte) 1);
        nadv.setOverrideFlag((byte) 1);
        nadv.addOption(NeighborDiscoveryOptions.TYPE_TARGET_LL_ADDRESS, srcMac.toBytes());

        icmp6.setPayload(nadv);
        ipv6.setPayload(icmp6);
        eth.setPayload(ipv6);

        return eth;
    }

    private void packetOut(Ethernet ethPkt, DeviceId deviceId, PortNumber outputPort) {
        ClosDeviceService service = get(ClosDeviceService.class);

        String logString = String.format("[packetOut] Sending packet via deviceId:%s Output:%d with Vlan:%d SrcMac:%s DstMac:%s", deviceId.toString(),outputPort.toLong(), ethPkt.getVlanID(), ethPkt.getSourceMAC(), ethPkt.getDestinationMAC());
        log.debug("[packetOut] {}",logString);

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(outputPort).build();

        ByteBuffer packet = ByteBuffer.wrap(ethPkt.serialize());

        OutboundPacket outboundPacket = new DefaultOutboundPacket(deviceId, treatment, packet);
        if (outboundPacket != null) {
            service.getPacketService().emit(outboundPacket);
        }
    }

    private void sendIcmp4Response(Ethernet ethRequest, ConnectPoint outport) {

        Ethernet ethReply = ICMP.buildIcmpReply(ethRequest);

        packetOut(ethReply, outport.deviceId(), outport.port());
    }

    private void sendIcmp6Response(Ethernet ethRequest, ConnectPoint outport) {

        Ethernet ethReply = ICMP6.buildIcmp6Reply(ethRequest);

        packetOut(ethReply, outport.deviceId(), outport.port());
    }

    // private Client getClientFromIp(IpAddress ipAddress) {

    //     ClosDeviceService service = get(ClosDeviceService.class);
    //     ConsistentMap<UUID, BaseDevice> registry = service.getRegistry();

    //     if (registry.isEmpty()) {
    //         return null;
    //     }

    //     for (Map.Entry<UUID, Versioned<BaseDevice>> entry : registry.entrySet()) {
    //         Versioned<BaseDevice> deviceV = entry.getValue();
    //         if (deviceV == null) continue;
    //         BaseDevice device = deviceV.value();
    //         if (device instanceof Client) {
    //             IpPrefix clientIpPrefix = device.getIpPrefix();
    //             IpPrefix empty = IpPrefix.valueOf(IpAddress.makeMaskPrefix(IpAddress.Version.INET6, 0), 0);
    //             if (clientIpPrefix != null && !clientIpPrefix.equals(empty) && clientIpPrefix.contains(ipAddress)){
    //                 return (Client) device;
    //             }
    //         }
    //     }
    //     return null;
    // }

    // private List<Servicio> getServiceFromIp(IpAddress ipAddress) {

    //     ClosDeviceService service = get(ClosDeviceService.class);
    //     ConsistentMap<UUID, BaseDevice> registry = service.getRegistry();

    //     if (registry.isEmpty()) {
    //         return null;
    //     }

    //     List<Servicio> result = new ArrayList<Servicio>();

    //     for (Map.Entry<UUID, Versioned<BaseDevice>> entry : registry.entrySet()) {
    //         Versioned<BaseDevice> deviceV = entry.getValue();
    //         if (deviceV == null) continue;
    //         BaseDevice device = deviceV.value();
    //         if (device instanceof Servicio) {
    //             IpPrefix serviceIpPrefix = device.getIpPrefix();
    //             //if (IpPrefix.valueOf(ipAddress, 64).contains(serviceIpPrefix)){
    //             if (serviceIpPrefix != null && serviceIpPrefix.contains(ipAddress)){
    //                 // return (Servicio) device;
    //                 result.add((Servicio)device);
    //             }
    //         }
    //     }
    //     // return null;
    //     return result;
    // }

    // private Router getRouterDevice() {
    //     ClosDeviceService service = get(ClosDeviceService.class);
    //     ConsistentMap<UUID, BaseDevice> registry = service.getRegistry();

    //     if (registry.isEmpty()) {
    //         return null;
    //     }
    //     for (Map.Entry<UUID, Versioned<BaseDevice>> entry : registry.entrySet()) {
    //         if (entry.getValue().value() instanceof Router) {
    //             return (Router) entry.getValue().value();
    //         }
    //     }
    //     return null;
    // }

    // TO DO: There are several external services

    // private Servicio getExternalServiceDevice() {
    //     ClosDeviceService service = get(ClosDeviceService.class);
    //     ConsistentMap<UUID, BaseDevice> registry = service.getRegistry();

    //     if (registry.isEmpty()) {
    //         return null;
    //     }
    //     for (Map.Entry<UUID, Versioned<BaseDevice>> entry : registry.entrySet()) {
    //         if (entry.getValue().value() instanceof Servicio && entry.getValue().value().getRole() == Servicio.EXTERNAL) {
    //             return (Servicio) entry.getValue().value();
    //         }
    //     }
    //     return null;
    // }

}
