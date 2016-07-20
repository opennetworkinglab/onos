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
package org.onosproject.isis.io.isispacket.tlv;

import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.isis.controller.IsisInterfaceState;
import org.onosproject.isis.io.util.IsisConstants;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test class for TlvsToBytes.
 */
public class TlvsToBytesTest {
    private final String areaAddress = "49";
    private final Ip4Address ip4Address = Ip4Address.valueOf("10.10.10.10");
    private final String systemName = "ROUTER";
    private final String neighborId = "2929.2929.2929";
    private List<Byte> tlv;
    private MacAddress macAddress = MacAddress.valueOf("a4:23:05:00:00:00");
    private String prefix = "192.168.7";

    /**
     * Tests TlvToBytes() method.
     */
    @Test
    public void testTlvToBytes() throws Exception {
        TlvHeader tlvHeader = new TlvHeader();
        tlvHeader.setTlvType(TlvType.AREAADDRESS.value());
        tlvHeader.setTlvLength(0);
        AreaAddressTlv areaAddressTlv = new AreaAddressTlv(tlvHeader);
        areaAddressTlv.addAddress(areaAddress);
        tlv = TlvsToBytes.tlvToBytes(areaAddressTlv);
        assertThat(tlv, is(notNullValue()));

        tlvHeader.setTlvType(TlvType.PROTOCOLSUPPORTED.value());
        tlvHeader.setTlvLength(0);
        ProtocolSupportedTlv protocolSupportedTlv = new ProtocolSupportedTlv(tlvHeader);
        protocolSupportedTlv.addProtocolSupported((byte) IsisConstants.PROTOCOLSUPPORTED);
        tlv = TlvsToBytes.tlvToBytes(protocolSupportedTlv);
        assertThat(tlv, is(notNullValue()));

        tlvHeader.setTlvType(TlvType.IPINTERFACEADDRESS.value());
        tlvHeader.setTlvLength(0);
        IpInterfaceAddressTlv ipInterfaceAddressTlv = new IpInterfaceAddressTlv(tlvHeader);
        ipInterfaceAddressTlv.addInterfaceAddres(ip4Address);
        tlv = TlvsToBytes.tlvToBytes(ipInterfaceAddressTlv);
        assertThat(tlv, is(notNullValue()));

        tlvHeader.setTlvType(TlvType.HOSTNAME.value());
        tlvHeader.setTlvLength(0);
        HostNameTlv hostNameTlv = new HostNameTlv(tlvHeader);
        hostNameTlv.setHostName(systemName);
        tlv = TlvsToBytes.tlvToBytes(hostNameTlv);
        assertThat(tlv, is(notNullValue()));

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
        metricsOfReachability.setNeighborId(neighborId);
        isReachabilityTlv.addMeticsOfReachability(metricsOfReachability);
        tlv = TlvsToBytes.tlvToBytes(isReachabilityTlv);
        assertThat(tlv, is(notNullValue()));

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
        metricOfIntRea.setIpAddress(ip4Address);
        metricOfIntRea.setSubnetAddres(ip4Address);
        ipInterReacTlv.addInternalReachabilityMetric(metricOfIntRea);
        tlv = TlvsToBytes.tlvToBytes(ipInterReacTlv);
        assertThat(tlv, is(notNullValue()));

        tlvHeader.setTlvType(TlvType.PADDING.value());
        tlvHeader.setTlvLength(255);
        PaddingTlv paddingTlv = new PaddingTlv(tlvHeader);
        tlv = TlvsToBytes.tlvToBytes(paddingTlv);
        assertThat(tlv, is(notNullValue()));

        tlvHeader.setTlvType(TlvType.IPEXTENDEDREACHABILITY.value());
        tlvHeader.setTlvLength(0);
        IpExtendedReachabilityTlv extendedTlv = new IpExtendedReachabilityTlv(tlvHeader);
        extendedTlv.setDown(false);
        extendedTlv.setMetric(10);
        extendedTlv.setPrefix(prefix);
        extendedTlv.setPrefixLength(24);
        extendedTlv.setSubTlvLength((byte) 0);
        extendedTlv.setSubTlvPresence(false);
        tlv = TlvsToBytes.tlvToBytes(extendedTlv);
        assertThat(tlv, is(notNullValue()));

        tlvHeader.setTlvType(TlvType.ADJACENCYSTATE.value());
        tlvHeader.setTlvLength(0);
        AdjacencyStateTlv adjacencyStateTlv = new AdjacencyStateTlv(tlvHeader);
        adjacencyStateTlv.setAdjacencyType((byte) IsisInterfaceState.DOWN.value());
        tlv = TlvsToBytes.tlvToBytes(adjacencyStateTlv);
        assertThat(tlv, is(notNullValue()));

        tlvHeader.setTlvType(TlvType.ISNEIGHBORS.value());
        tlvHeader.setTlvLength(0);
        IsisNeighborTlv isisNeighborTlv = new IsisNeighborTlv(tlvHeader);
        isisNeighborTlv.addNeighbor(macAddress);
        tlv = TlvsToBytes.tlvToBytes(isisNeighborTlv);
        assertThat(tlv, is(notNullValue()));

        tlvHeader.setTlvType(TlvType.EXTENDEDISREACHABILITY.value());
        tlvHeader.setTlvLength(0);
        IsExtendedReachability reachability = new IsExtendedReachability(tlvHeader);
        NeighborForExtendedIs forExtendedIs = new NeighborForExtendedIs();
        forExtendedIs.setMetric(10);
        forExtendedIs.setNeighborId(neighborId);
        reachability.addNeighbor(forExtendedIs);
        tlv = TlvsToBytes.tlvToBytes(reachability);
        assertThat(tlv, is(notNullValue()));
    }
}