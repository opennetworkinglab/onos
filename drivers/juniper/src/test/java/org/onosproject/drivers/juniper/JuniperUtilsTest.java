/*
 * Copyright 2019-present Open Networking Foundation
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

import com.google.common.io.CharStreams;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Test;
import org.onlab.packet.ChassisId;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.PortDescription;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.onosproject.net.Device.Type.ROUTER;

public class JuniperUtilsTest {


    private static final String DEVICE_ID = "netconf:1.2.3.4:830";
    private final DeviceId deviceId =  DeviceId.deviceId(DEVICE_ID);

    @Test
    public void testDeviceDescriptionParsedFromJunos15() throws IOException {

        HierarchicalConfiguration getSystemInfoResp = XmlConfigParser.loadXml(
                getClass().getResourceAsStream("/Junos_get-system-information_response_15.1.xml"));
        String chassisText = CharStreams.toString(
                new InputStreamReader(
                        getClass().getResourceAsStream("/Junos_get-chassis-mac-addresses_response_15.1.xml")));

        DeviceDescription expected =
                new DefaultDeviceDescription(URI.create(DEVICE_ID), ROUTER, "JUNIPER", "mx240",
                        "junos 15.1R5.5", "JN11AC665AFC", new ChassisId("8418889983c0"));

        assertEquals(expected, JuniperUtils.parseJuniperDescription(deviceId, getSystemInfoResp, chassisText));

    }


    @Test
    public void testDeviceDescriptionParsedFromJunos19() throws IOException {

        HierarchicalConfiguration getSystemInfoResp = XmlConfigParser.loadXml(
                getClass().getResourceAsStream("/Junos_get-system-information_response_19.2.xml"));
        String chassisText = CharStreams.toString(
                new InputStreamReader(
                        getClass().getResourceAsStream("/Junos_get-chassis-mac-addresses_response_19.2.xml")));

        DeviceDescription expected =
                new DefaultDeviceDescription(URI.create(DEVICE_ID), ROUTER, "JUNIPER", "acx6360-or",
                        "junos 19.2I-20190228_dev_common.0.2316", "DX004", new ChassisId("f4b52f1f81c0"));

        assertEquals(expected, JuniperUtils.parseJuniperDescription(deviceId, getSystemInfoResp, chassisText));

    }

    @Test
    public void testLinkAbstractionToString() {
        final JuniperUtils.LinkAbstraction x = new JuniperUtils.LinkAbstraction("foo", 1, 2, null, null);

        assertThat("Null attributes excluded", x.toString(), allOf(
                containsString("LinkAbstraction"),
                containsString("localPortName=foo"),
                not(containsString("remotePortDescription"))));
    }


    @Test
    public void testLldpNeighborsInformationParsedFromJunos18() {
        HierarchicalConfiguration reply = XmlConfigParser.loadXml(
                getClass().getResourceAsStream("/Junos_get-lldp-neighbors-information_response_18.4.xml"));

        final Set<JuniperUtils.LinkAbstraction> expected = new HashSet<>();
        expected.add(new JuniperUtils.LinkAbstraction("ge-0/0/1", 0x2c6bf509b0c0L, 527L, null, null));
        expected.add(new JuniperUtils.LinkAbstraction("ge-0/0/2", 0x2c6bf5bde7c0L, 528L, null, null));

        assertEquals(expected, JuniperUtils.parseJuniperLldp(reply));

    }


    @Test
    public void testStaticRoutesParsedFromJunos18() {
        HierarchicalConfiguration reply = XmlConfigParser.loadXml(
                getClass().getResourceAsStream("/Junos_get-route-information_response_18.4.xml"));

        final Collection<StaticRoute> expected = new HashSet<>();
        expected.add(new StaticRoute(Ip4Prefix.valueOf("0.0.0.0/0"), Ip4Address.valueOf("172.26.138.1"), false, 100));

        assertEquals(expected, JuniperUtils.parseRoutingTable(reply));
    }


    @Test
    public void testInterfacesParsedFromJunos18() {
        HierarchicalConfiguration reply = XmlConfigParser.loadXml(
                getClass().getResourceAsStream("/Junos_get-interface-information_response_18.4.xml"));

        final Collection<PortDescription> expected = new ArrayList<>();
        expected.add(DefaultPortDescription.builder()
                .withPortNumber(PortNumber.portNumber(513L)).isRemoved(false)
                .type(Port.Type.COPPER).portSpeed(JuniperUtils.DEFAULT_PORT_SPEED)
                .annotations(DefaultAnnotations.builder()
                        .set(JuniperUtils.AK_OPER_STATUS, "up")
                        .set(AnnotationKeys.PORT_NAME, "jsrv")
                        .set(JuniperUtils.AK_IF_TYPE, "Ethernet")
                        .set(AnnotationKeys.PORT_MAC, "2c:6b:f5:03:ff:c0")
                        .set(JuniperUtils.AK_ADMIN_STATUS, "up")
                        .build()).build());
        expected.add(DefaultPortDescription.builder()
                .withPortNumber(PortNumber.portNumber(514L))
                .isRemoved(false).type(Port.Type.COPPER)
                .portSpeed(JuniperUtils.DEFAULT_PORT_SPEED)
                .annotations(
                        DefaultAnnotations.builder()
                        .set(JuniperUtils.AK_ENCAPSULATION, "unknown")
                        .set("portName", "jsrv.1")
                        .set(JuniperUtils.AK_PHYSICAL_PORT_NAME, "jsrv")
                        .set("inet", "128.0.0.127")
                        .set("ip", "128.0.0.127")
                        .build()).build());

        assertEquals(expected, JuniperUtils.parseJuniperPorts(reply));
    }

}
