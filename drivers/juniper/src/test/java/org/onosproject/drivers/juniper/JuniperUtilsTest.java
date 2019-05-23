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


import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.onosproject.net.Device.Type.ROUTER;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Test;
import org.onlab.packet.ChassisId;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;

import com.google.common.io.CharStreams;

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

        DeviceDescription actual = JuniperUtils.parseJuniperDescription(deviceId, getSystemInfoResp, chassisText);
        DeviceDescription expected =
                new DefaultDeviceDescription(URI.create(DEVICE_ID), ROUTER, "JUNIPER", "mx240",
                        "junos 15.1R5.5", "JN11AC665AFC", new ChassisId("8418889983c0"));

        assertEquals(expected, actual);

    }


    @Test
    public void testDeviceDescriptionParsedFromJunos19() throws IOException {

        HierarchicalConfiguration getSystemInfoResp = XmlConfigParser.loadXml(
                getClass().getResourceAsStream("/Junos_get-system-information_response_19.2.xml"));
        String chassisText = CharStreams.toString(
                new InputStreamReader(
                        getClass().getResourceAsStream("/Junos_get-chassis-mac-addresses_response_19.2.xml")));

        DeviceDescription actual = JuniperUtils.parseJuniperDescription(deviceId, getSystemInfoResp, chassisText);
        DeviceDescription expected =
                new DefaultDeviceDescription(URI.create(DEVICE_ID), ROUTER, "JUNIPER", "acx6360-or",
                        "junos 19.2I-20190228_dev_common.0.2316", "DX004", new ChassisId("f4b52f1f81c0"));

        assertEquals(expected, actual);

    }


    @Test
    public void testLinkAbstractionToString() throws IOException {
        final JuniperUtils.LinkAbstraction x = new JuniperUtils.LinkAbstraction("foo", 1, 2, null, null);
        assertThat("Null attributes excluded", x.toString(), allOf(
                containsString("LinkAbstraction"),
                containsString("localPortName=foo"),
                not(containsString("remotePortDescription"))));


    }



}