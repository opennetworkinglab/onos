/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.isis.io.util;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.isis.controller.IsisInterface;
import org.onosproject.isis.controller.IsisNeighbor;
import org.onosproject.isis.controller.IsisNetworkType;
import org.onosproject.isis.controller.IsisPduType;
import org.onosproject.isis.io.isispacket.IsisHeader;
import org.onosproject.isis.io.isispacket.pdu.AttachedToOtherAreas;
import org.onosproject.isis.io.isispacket.pdu.LsPdu;
import org.onosproject.isis.io.isispacket.tlv.AreaAddressTlv;
import org.onosproject.isis.io.isispacket.tlv.HostNameTlv;
import org.onosproject.isis.io.isispacket.tlv.IpExtendedReachabilityTlv;
import org.onosproject.isis.io.isispacket.tlv.IpInterfaceAddressTlv;
import org.onosproject.isis.io.isispacket.tlv.IpInternalReachabilityTlv;
import org.onosproject.isis.io.isispacket.tlv.IsReachabilityTlv;
import org.onosproject.isis.io.isispacket.tlv.MetricOfInternalReachability;
import org.onosproject.isis.io.isispacket.tlv.MetricsOfReachability;
import org.onosproject.isis.io.isispacket.tlv.ProtocolSupportedTlv;
import org.onosproject.isis.io.isispacket.tlv.TlvHeader;
import org.onosproject.isis.io.isispacket.tlv.TlvType;

import java.util.List;

/**
 * Representation of link state PDU generator.
 */
public class LspGenerator {

    public LsPdu getLsp(IsisInterface isisInterface, String lspId, IsisPduType isisPduType,
                        List<Ip4Address> allConfiguredInterfaceIps) {
        IsisHeader header = getHeader(isisPduType);
        LsPdu lsp = new LsPdu(header);

        lsp.setPduLength(0);
        lsp.setRemainingLifeTime(IsisConstants.LSPMAXAGE);
        lsp.setLspId(lspId);
        lsp.setSequenceNumber(isisInterface.isisLsdb().lsSequenceNumber(isisPduType));
        lsp.setCheckSum(0);
        if (isisPduType == IsisPduType.L1LSPDU) {
            lsp.setTypeBlock((byte) 1);
            lsp.setIntermediateSystemType((byte) 1);
        } else if (isisPduType == IsisPduType.L2LSPDU) {
            lsp.setTypeBlock((byte) 3);
            lsp.setIntermediateSystemType((byte) 3);
        }
        lsp.setAttachedToOtherAreas(AttachedToOtherAreas.NONE);
        lsp.setPartitionRepair(false);
        lsp.setLspDbol(false);

        TlvHeader tlvHeader = new TlvHeader();
        tlvHeader.setTlvType(TlvType.AREAADDRESS.value());
        tlvHeader.setTlvLength(0);
        AreaAddressTlv areaAddressTlv = new AreaAddressTlv(tlvHeader);
        areaAddressTlv.addAddress(isisInterface.areaAddress());
        lsp.addTlv(areaAddressTlv);

        tlvHeader.setTlvType(TlvType.PROTOCOLSUPPORTED.value());
        tlvHeader.setTlvLength(0);
        ProtocolSupportedTlv protocolSupportedTlv = new ProtocolSupportedTlv(tlvHeader);
        protocolSupportedTlv.addProtocolSupported((byte) IsisConstants.PROTOCOLSUPPORTED);
        lsp.addTlv(protocolSupportedTlv);

        tlvHeader.setTlvType(TlvType.IPINTERFACEADDRESS.value());
        tlvHeader.setTlvLength(0);
        IpInterfaceAddressTlv ipInterfaceAddressTlv = new IpInterfaceAddressTlv(tlvHeader);
        for (Ip4Address ipaddress : allConfiguredInterfaceIps) {
            ipInterfaceAddressTlv.addInterfaceAddres(ipaddress);
        }
        lsp.addTlv(ipInterfaceAddressTlv);

        tlvHeader.setTlvType(TlvType.HOSTNAME.value());
        tlvHeader.setTlvLength(0);
        HostNameTlv hostNameTlv = new HostNameTlv(tlvHeader);
        hostNameTlv.setHostName(isisInterface.intermediateSystemName());
        lsp.addTlv(hostNameTlv);

        tlvHeader.setTlvType(TlvType.ISREACHABILITY.value());
        tlvHeader.setTlvLength(0);
        IsReachabilityTlv isReachabilityTlv = new IsReachabilityTlv(tlvHeader);
        isReachabilityTlv.setReserved(0);
        MetricsOfReachability metricsOfReachability = new MetricsOfReachability();
        metricsOfReachability.setDefaultMetric((byte) 10);
        metricsOfReachability.setDefaultIsInternal(true);
        metricsOfReachability.setDelayMetric((byte) 10);
        metricsOfReachability.setDelayIsInternal(true);
        metricsOfReachability.setDelayMetricSupported(true);
        metricsOfReachability.setExpenseMetric((byte) 10);
        metricsOfReachability.setExpenseIsInternal(true);
        metricsOfReachability.setExpenseMetricSupported(true);
        metricsOfReachability.setErrorMetric((byte) 10);
        metricsOfReachability.setErrorIsInternal(true);
        metricsOfReachability.setErrorMetricSupported(true);
        if (isisInterface.networkType() == IsisNetworkType.BROADCAST) {
            if (isisPduType == IsisPduType.L1LSPDU) {
                metricsOfReachability.setNeighborId(isisInterface.l1LanId());
            } else if (isisPduType == IsisPduType.L2LSPDU) {
                metricsOfReachability.setNeighborId(isisInterface.l2LanId());
            }
        } else if (isisInterface.networkType() == IsisNetworkType.P2P) {
            MacAddress neighborMac = isisInterface.neighbors().iterator().next();
            IsisNeighbor neighbor = isisInterface.lookup(neighborMac);
            metricsOfReachability.setNeighborId(neighbor.neighborSystemId() + ".00");
        }

        isReachabilityTlv.addMeticsOfReachability(metricsOfReachability);
        lsp.addTlv(isReachabilityTlv);

        tlvHeader.setTlvType(TlvType.IPINTERNALREACHABILITY.value());
        tlvHeader.setTlvLength(0);
        IpInternalReachabilityTlv ipInterReacTlv = new IpInternalReachabilityTlv(tlvHeader);
        MetricOfInternalReachability metricOfIntRea = new MetricOfInternalReachability();
        metricOfIntRea.setDefaultMetric((byte) 10);
        metricOfIntRea.setDefaultIsInternal(true);
        metricOfIntRea.setDefaultDistributionDown(true);
        metricOfIntRea.setDelayMetric((byte) 0);
        metricOfIntRea.setDelayMetricSupported(false);
        metricOfIntRea.setDelayIsInternal(true);
        metricOfIntRea.setExpenseMetric((byte) 0);
        metricOfIntRea.setExpenseMetricSupported(false);
        metricOfIntRea.setExpenseIsInternal(true);
        metricOfIntRea.setErrorMetric((byte) 0);
        metricOfIntRea.setErrorMetricSupported(false);
        metricOfIntRea.setExpenseIsInternal(true);
        Ip4Address ip4Address = isisInterface.interfaceIpAddress();
        byte[] ipAddress = ip4Address.toOctets();
       // ipAddress[ipAddress.length - 1] = 0;
        byte[] networkmass = isisInterface.networkMask();
        // metric calculation part
        byte[] result = new byte[ipAddress.length];
        result[0] = (byte) (ipAddress[0] & networkmass[0]);
        result[1] = (byte) (ipAddress[1] & networkmass[1]);
        result[2] = (byte) (ipAddress[2] & networkmass[2]);
        result[3] = (byte) (ipAddress[3] & networkmass[3]);
        metricOfIntRea.setIpAddress(Ip4Address.valueOf(result));
        metricOfIntRea.setSubnetAddres(Ip4Address.valueOf(isisInterface.networkMask()));
        ipInterReacTlv.addInternalReachabilityMetric(metricOfIntRea);
        lsp.addTlv(ipInterReacTlv);

        tlvHeader.setTlvType(TlvType.IPEXTENDEDREACHABILITY.value());
        tlvHeader.setTlvLength(0);
        IpExtendedReachabilityTlv extendedTlv = new IpExtendedReachabilityTlv(tlvHeader);
        extendedTlv.setDown(false);
        extendedTlv.setMetric(10);
        extendedTlv.setPrefix("192.168.7");
        extendedTlv.setPrefixLength(24);
        extendedTlv.setSubTlvLength((byte) 0);
        extendedTlv.setSubTlvPresence(false);
        lsp.addTlv(extendedTlv);

        return lsp;
    }

    public IsisHeader getHeader(IsisPduType pduType) {
        IsisHeader isisHeader = new IsisHeader();
        isisHeader.setIrpDiscriminator((byte) IsisConstants.IRPDISCRIMINATOR);
        isisHeader.setPduHeaderLength((byte) IsisUtil.getPduHeaderLength(pduType.value()));
        isisHeader.setVersion((byte) IsisConstants.ISISVERSION);
        isisHeader.setIdLength((byte) IsisConstants.SYSTEMIDLENGTH);
        isisHeader.setIsisPduType(pduType.value());
        isisHeader.setVersion2((byte) IsisConstants.ISISVERSION);
        isisHeader.setReserved((byte) IsisConstants.RESERVED);
        isisHeader.setMaximumAreaAddresses((byte) IsisConstants.MAXAREAADDRESS);
        return isisHeader;
    }
}