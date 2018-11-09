/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.incubator.net.virtual.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.action.Option;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.EthType;
import org.onosproject.cli.net.EthTypeCompleter;
import org.onosproject.cli.net.ExtHeader;
import org.onosproject.cli.net.ExtHeaderCompleter;
import org.onosproject.cli.net.Icmp6Code;
import org.onosproject.cli.net.Icmp6CodeCompleter;
import org.onosproject.cli.net.Icmp6Type;
import org.onosproject.cli.net.Icmp6TypeCompleter;
import org.onosproject.cli.net.IpProtocol;
import org.onosproject.cli.net.IpProtocolCompleter;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketRequest;
import org.onosproject.net.packet.PacketService;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Tests virtual network packet requests.
 */
@Service
@Command(scope = "onos", name = "vnet-packet",
        description = "Tests virtual network packet requests")
public class VirtualNetworkPacketRequestCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "command",
            description = "Command name (requestPackets|getRequests|cancelPackets)",
            required = true, multiValued = false)
    private String command = null;

    @Argument(index = 1, name = "networkId", description = "Network ID",
            required = true, multiValued = false)
    private Long networkId = null;

    @Option(name = "--deviceId", description = "Device ID",
            required = false, multiValued = false)
    private String deviceIdString = null;

    // Traffic selector
    @Option(name = "-s", aliases = "--ethSrc", description = "Source MAC Address",
            required = false, multiValued = false)
    private String srcMacString = null;

    @Option(name = "-d", aliases = "--ethDst", description = "Destination MAC Address",
            required = false, multiValued = false)
    private String dstMacString = null;

    @Option(name = "-t", aliases = "--ethType", description = "Ethernet Type",
            required = false, multiValued = false)
    @Completion(EthTypeCompleter.class)
    private String ethTypeString = null;

    @Option(name = "-v", aliases = "--vlan", description = "VLAN ID",
            required = false, multiValued = false)
    private String vlanString = null;

    @Option(name = "--ipProto", description = "IP Protocol",
            required = false, multiValued = false)
    @Completion(IpProtocolCompleter.class)
    private String ipProtoString = null;

    @Option(name = "--ipSrc", description = "Source IP Prefix",
            required = false, multiValued = false)
    private String srcIpString = null;

    @Option(name = "--ipDst", description = "Destination IP Prefix",
            required = false, multiValued = false)
    private String dstIpString = null;

    @Option(name = "--fLabel", description = "IPv6 Flow Label",
            required = false, multiValued = false)
    private String fLabelString = null;

    @Option(name = "--icmp6Type", description = "ICMPv6 Type",
            required = false, multiValued = false)
    @Completion(Icmp6TypeCompleter.class)
    private String icmp6TypeString = null;

    @Option(name = "--icmp6Code", description = "ICMPv6 Code",
            required = false, multiValued = false)
    @Completion(Icmp6CodeCompleter.class)
    private String icmp6CodeString = null;

    @Option(name = "--ndTarget", description = "IPv6 Neighbor Discovery Target Address",
            required = false, multiValued = false)
    private String ndTargetString = null;

    @Option(name = "--ndSLL", description = "IPv6 Neighbor Discovery Source Link-Layer",
            required = false, multiValued = false)
    private String ndSllString = null;

    @Option(name = "--ndTLL", description = "IPv6 Neighbor Discovery Target Link-Layer",
            required = false, multiValued = false)
    private String ndTllString = null;

    @Option(name = "--tcpSrc", description = "Source TCP Port",
            required = false, multiValued = false)
    private String srcTcpString = null;

    @Option(name = "--tcpDst", description = "Destination TCP Port",
            required = false, multiValued = false)
    private String dstTcpString = null;

    @Option(name = "--extHdr", description = "IPv6 Extension Header Pseudo-field",
            required = false, multiValued = true)
    @Completion(ExtHeaderCompleter.class)
    private List<String> extHdrStringList = null;

    @Override
    protected void doExecute() {
        VirtualNetworkService service = get(VirtualNetworkService.class);
        PacketService virtualPacketService = service.get(NetworkId.networkId(networkId), PacketService.class);

        if (command == null) {
            print("Command is not defined");
            return;
        }

        if (command.equals("getRequests")) {
            getRequests(virtualPacketService);
            return;
        }

        TrafficSelector selector = buildTrafficSelector();
        PacketPriority packetPriority = PacketPriority.CONTROL; //TODO allow user to specify
        Optional<DeviceId> optionalDeviceId = null;
        if (!isNullOrEmpty(deviceIdString)) {
            optionalDeviceId = Optional.of(DeviceId.deviceId(deviceIdString));
        }

        if (command.equals("requestPackets")) {
            if (optionalDeviceId != null) {
                virtualPacketService.requestPackets(selector, packetPriority, appId(), optionalDeviceId);
            } else {
                virtualPacketService.requestPackets(selector, packetPriority, appId());
            }
            print("Virtual packet requested:\n%s", selector);
            return;
        }

       if (command.equals("cancelPackets")) {
            if (optionalDeviceId != null) {
                virtualPacketService.cancelPackets(selector, packetPriority, appId(), optionalDeviceId);
            } else {
                virtualPacketService.cancelPackets(selector, packetPriority, appId());
            }
            print("Virtual packet cancelled:\n%s", selector);
            return;
        }

        print("Unsupported command %s", command);
    }

    private void getRequests(PacketService packetService) {
        List<PacketRequest> packetRequests = packetService.getRequests();
        if (outputJson()) {
            print("%s", json(packetRequests));
        } else {
            packetRequests.forEach(packetRequest -> print(packetRequest.toString()));
        }
    }

    private JsonNode json(List<PacketRequest> packetRequests) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        packetRequests.forEach(packetRequest ->
                                       result.add(jsonForEntity(packetRequest, PacketRequest.class)));
        return result;
    }

    /**
     * Constructs a traffic selector based on the command line arguments
     * presented to the command.
     * @return traffic selector
     */
    private TrafficSelector buildTrafficSelector() {
        IpPrefix srcIpPrefix = null;
        IpPrefix dstIpPrefix = null;

        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

        if (!isNullOrEmpty(srcIpString)) {
            srcIpPrefix = IpPrefix.valueOf(srcIpString);
            if (srcIpPrefix.isIp4()) {
                selectorBuilder.matchIPSrc(srcIpPrefix);
            } else {
                selectorBuilder.matchIPv6Src(srcIpPrefix);
            }
        }

        if (!isNullOrEmpty(dstIpString)) {
            dstIpPrefix = IpPrefix.valueOf(dstIpString);
            if (dstIpPrefix.isIp4()) {
                selectorBuilder.matchIPDst(dstIpPrefix);
            } else {
                selectorBuilder.matchIPv6Dst(dstIpPrefix);
            }
        }

        if ((srcIpPrefix != null) && (dstIpPrefix != null) &&
            (srcIpPrefix.version() != dstIpPrefix.version())) {
            // ERROR: IP src/dst version mismatch
            throw new IllegalArgumentException(
                        "IP source and destination version mismatch");
        }

        //
        // Set the default EthType based on the IP version if the matching
        // source or destination IP prefixes.
        //
        Short ethType = null;
        if ((srcIpPrefix != null) && srcIpPrefix.isIp6()) {
            ethType = EthType.IPV6.value();
        }
        if ((dstIpPrefix != null) && dstIpPrefix.isIp6()) {
            ethType = EthType.IPV6.value();
        }
        if (!isNullOrEmpty(ethTypeString)) {
            ethType = EthType.parseFromString(ethTypeString);
        }
        if (ethType != null) {
            selectorBuilder.matchEthType(ethType);
        }
        if (!isNullOrEmpty(vlanString)) {
            selectorBuilder.matchVlanId(VlanId.vlanId(Short.parseShort(vlanString)));
        }
        if (!isNullOrEmpty(srcMacString)) {
            selectorBuilder.matchEthSrc(MacAddress.valueOf(srcMacString));
        }

        if (!isNullOrEmpty(dstMacString)) {
            selectorBuilder.matchEthDst(MacAddress.valueOf(dstMacString));
        }

        if (!isNullOrEmpty(ipProtoString)) {
            short ipProtoShort = IpProtocol.parseFromString(ipProtoString);
            selectorBuilder.matchIPProtocol((byte) ipProtoShort);
        }

        if (!isNullOrEmpty(fLabelString)) {
            selectorBuilder.matchIPv6FlowLabel(Integer.parseInt(fLabelString));
        }

        if (!isNullOrEmpty(icmp6TypeString)) {
            byte icmp6Type = Icmp6Type.parseFromString(icmp6TypeString);
            selectorBuilder.matchIcmpv6Type(icmp6Type);
        }

        if (!isNullOrEmpty(icmp6CodeString)) {
            byte icmp6Code = Icmp6Code.parseFromString(icmp6CodeString);
            selectorBuilder.matchIcmpv6Code(icmp6Code);
        }

        if (!isNullOrEmpty(ndTargetString)) {
            selectorBuilder.matchIPv6NDTargetAddress(Ip6Address.valueOf(ndTargetString));
        }

        if (!isNullOrEmpty(ndSllString)) {
            selectorBuilder.matchIPv6NDSourceLinkLayerAddress(MacAddress.valueOf(ndSllString));
        }

        if (!isNullOrEmpty(ndTllString)) {
            selectorBuilder.matchIPv6NDTargetLinkLayerAddress(MacAddress.valueOf(ndTllString));
        }

        if (!isNullOrEmpty(srcTcpString)) {
            selectorBuilder.matchTcpSrc(TpPort.tpPort(Integer.parseInt(srcTcpString)));
        }

        if (!isNullOrEmpty(dstTcpString)) {
            selectorBuilder.matchTcpDst(TpPort.tpPort(Integer.parseInt(dstTcpString)));
        }

        if (extHdrStringList != null) {
            short extHdr = 0;
            for (String extHdrString : extHdrStringList) {
                extHdr = (short) (extHdr | ExtHeader.parseFromString(extHdrString));
            }
            selectorBuilder.matchIPv6ExthdrFlags(extHdr);
        }

        return selectorBuilder.build();
    }
}
