package org.projectfloodlight.protocol.match;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Iterator;

import org.junit.Test;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.match.MatchFields;
import org.projectfloodlight.openflow.types.ArpOpcode;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.Masked;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TransportPort;

import com.google.common.collect.Iterables;

public class MatchFieldIterationBase {

    private OFFactory factory;

    protected MatchFieldIterationBase(OFFactory factory) {
        this.factory = factory;
    }
    
    @Test
    public void iterateEmptyMatch() {
        Match match = factory.buildMatch().build();
        Iterator<MatchField<?>> iter = match.getMatchFields().iterator();
        assertThat(iter.hasNext(), is(false));
    }
    
    @Test
    public void iterateSingleExactMatchField() {
        OFPort port5 = OFPort.of(5);
        Match match = factory.buildMatch()
                .setExact(MatchField.IN_PORT, port5)
                .build();
        Iterator<MatchField<?>> iter = match.getMatchFields().iterator();
        assertThat(iter.hasNext(), is(true));
        MatchField<?> matchField = iter.next();
        assertThat(matchField.id, is(MatchFields.IN_PORT));
        assertThat(match.isExact(matchField), is(true));
        @SuppressWarnings("unchecked")
        MatchField<OFPort> portMatchField = (MatchField<OFPort>) matchField;
        OFPort port = match.get(portMatchField);
        assertThat(port, is(port5));
        assertThat(iter.hasNext(), is(false));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void iterateExactMatchFields() {
        OFPort port5 = OFPort.of(5);
        MacAddress macSrc = MacAddress.of("00:01:02:03:04:05");
        MacAddress macDst = MacAddress.of("01:01:02:02:03:03");
        IPv4Address ipSrc = IPv4Address.of("10.192.20.1");
        IPv4Address ipDst = IPv4Address.of("10.192.20.2");
        TransportPort tcpSrc = TransportPort.of(100);
        TransportPort tcpDst = TransportPort.of(200);
        Match match = factory.buildMatch()
                .setExact(MatchField.IN_PORT, port5)
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.ETH_SRC, macSrc)
                .setExact(MatchField.ETH_DST, macDst)
                .setExact(MatchField.IP_PROTO, IpProtocol.TCP)
                .setExact(MatchField.IPV4_SRC, ipSrc)
                .setExact(MatchField.IPV4_DST, ipDst)
                .setExact(MatchField.TCP_SRC, tcpSrc)
                .setExact(MatchField.TCP_DST, tcpDst)
                .build();
        assertThat(Iterables.size(match.getMatchFields()), is(9));
        for (MatchField<?> matchField: match.getMatchFields()) {
            switch (matchField.id) {
            case IN_PORT:
                OFPort port = match.get((MatchField<OFPort>) matchField);
                assertThat(port, is(port5));
                break;
            case ETH_TYPE:
                EthType ethType = match.get((MatchField<EthType>) matchField);
                assertThat(ethType, is(EthType.IPv4));
                break;
            case ETH_SRC:
                MacAddress mac = match.get((MatchField<MacAddress>) matchField);
                assertThat(mac, is(macSrc));
                break;
            case ETH_DST:
                mac = match.get((MatchField<MacAddress>) matchField);
                assertThat(mac, is(macDst));
                break;
            case IP_PROTO:
                IpProtocol ipProtocol = match.get((MatchField<IpProtocol>) matchField);
                assertThat(ipProtocol, is(IpProtocol.TCP));
                break;
            case IPV4_SRC:
                IPv4Address ip = match.get((MatchField<IPv4Address>) matchField);
                assertThat(ip, is(ipSrc));
                break;
            case IPV4_DST:
                ip = match.get((MatchField<IPv4Address>) matchField);
                assertThat(ip, is(ipDst));
                break;
            case TCP_SRC:
                TransportPort tcp = match.get((MatchField<TransportPort>) matchField);
                assertThat(tcp, is(tcpSrc));
                break;
            case TCP_DST:
                tcp = match.get((MatchField<TransportPort>) matchField);
                assertThat(tcp, is(tcpDst));
                break;
            default:
                fail("Unexpected match field returned from iterator");
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void iterateArpFields() {
        MacAddress macSrc = MacAddress.of("00:01:02:03:04:05");
        MacAddress macDst = MacAddress.of("01:01:02:02:03:03");
        IPv4Address ipSrc = IPv4Address.of("10.192.20.1");
        IPv4Address ipDst = IPv4Address.of("10.192.20.2");
        OFVersion version = factory.getVersion();
        boolean supportsArpHardwareAddress = (version != OFVersion.OF_10) &&
                (version != OFVersion.OF_11) && (version != OFVersion.OF_12);
        int matchFieldCount = 4;
        Match.Builder builder = factory.buildMatch();
        builder.setExact(MatchField.ETH_TYPE, EthType.ARP)
                .setExact(MatchField.ARP_OP, ArpOpcode.REPLY)
                .setExact(MatchField.ARP_SPA, ipSrc)
                .setExact(MatchField.ARP_TPA, ipDst);
        if (supportsArpHardwareAddress) {
            builder.setExact(MatchField.ARP_SHA, macSrc);
            builder.setExact(MatchField.ARP_THA, macDst);
            matchFieldCount += 2;
        }
        Match match = builder.build();
        assertThat(Iterables.size(match.getMatchFields()), is(matchFieldCount));
        for (MatchField<?> matchField: match.getMatchFields()) {
            switch (matchField.id) {
            case ETH_TYPE:
                EthType ethType = match.get((MatchField<EthType>) matchField);
                assertThat(ethType, is(EthType.ARP));
                break;
            case ARP_OP:
                ArpOpcode opcode = match.get((MatchField<ArpOpcode>) matchField);
                assertThat(opcode, is(ArpOpcode.REPLY));
                break;
            case ARP_SHA:
                MacAddress mac = match.get((MatchField<MacAddress>) matchField);
                assertThat(mac, is(macSrc));
                break;
            case ARP_THA:
                mac = match.get((MatchField<MacAddress>) matchField);
                assertThat(mac, is(macDst));
                break;
            case ARP_SPA:
                IPv4Address ip = match.get((MatchField<IPv4Address>) matchField);
                assertThat(ip, is(ipSrc));
                break;
            case ARP_TPA:
                ip = match.get((MatchField<IPv4Address>) matchField);
                assertThat(ip, is(ipDst));
                break;
            default:
                fail("Unexpected match field returned from iterator");
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void iterateMaskedFields() {
        MacAddress macSrc = MacAddress.of("01:02:03:04:00:00");
        MacAddress macSrcMask = MacAddress.of("FF:FF:FF:FF:00:00");
        MacAddress macDst = MacAddress.of("11:22:33:00:00:00");
        MacAddress macDstMask = MacAddress.of("FF:FF:FF:00:00:00");
        IPv4Address ipSrc = IPv4Address.of("10.192.20.0");
        IPv4Address ipSrcMask = IPv4Address.of("255.255.255.0");
        IPv4Address ipDst = IPv4Address.of("10.192.20.0");
        IPv4Address ipDstMask = IPv4Address.of("255.255.255.128");
        TransportPort tcpSrcMask = TransportPort.of(0x01F0);
        OFVersion version = factory.getVersion();
        boolean supportsAllMasks = (version != OFVersion.OF_10) &&
                (version != OFVersion.OF_11) && (version != OFVersion.OF_12);
        int matchFieldCount = 4;
        Match.Builder builder = factory.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setMasked(MatchField.IPV4_SRC, ipSrc, ipSrcMask)
                .setMasked(MatchField.IPV4_DST, ipDst, ipDstMask)
                .setExact(MatchField.IP_PROTO, IpProtocol.TCP);
        if (supportsAllMasks) {
            builder.setMasked(MatchField.ETH_SRC, macSrc, macSrcMask);
            builder.setMasked(MatchField.ETH_DST, macDst, macDstMask);
            builder.setMasked(MatchField.TCP_SRC, tcpSrcMask, tcpSrcMask);
            matchFieldCount += 3;
        }
        Match match = builder.build();
        assertThat(Iterables.size(match.getMatchFields()), is(matchFieldCount));
        for (MatchField<?> matchField: match.getMatchFields()) {
            switch (matchField.id) {
            case ETH_TYPE:
                EthType ethType = match.get((MatchField<EthType>) matchField);
                assertThat(ethType, is(EthType.IPv4));
                break;
            case ETH_SRC:
                Masked<MacAddress> mac = match.getMasked((MatchField<MacAddress>) matchField);
                assertThat(mac.getValue(), is(macSrc));
                assertThat(mac.getMask(), is(macSrcMask));
                break;
            case ETH_DST:
                mac = match.getMasked((MatchField<MacAddress>) matchField);
                assertThat(mac.getValue(), is(macDst));
                assertThat(mac.getMask(), is(macDstMask));
                break;
            case IP_PROTO:
                IpProtocol ipProtocol = match.get((MatchField<IpProtocol>) matchField);
                assertThat(ipProtocol, is(IpProtocol.TCP));
                break;
            case IPV4_SRC:
                Masked<IPv4Address> ip = match.getMasked((MatchField<IPv4Address>) matchField);
                assertThat(ip.getValue(), is(ipSrc));
                assertThat(ip.getMask(), is(ipSrcMask));
                break;
            case IPV4_DST:
                ip = match.getMasked((MatchField<IPv4Address>) matchField);
                assertThat(ip.getValue(), is(ipDst));
                assertThat(ip.getMask(), is(ipDstMask));
                break;
            case TCP_SRC:
                Masked<TransportPort> tcp = match.getMasked((MatchField<TransportPort>) matchField);
                assertThat(tcp.getValue(), is(tcpSrcMask));
                assertThat(tcp.getMask(), is(tcpSrcMask));
                break;
            default:
                fail("Unexpected match field returned from iterator");
            }
        }
    }
}
