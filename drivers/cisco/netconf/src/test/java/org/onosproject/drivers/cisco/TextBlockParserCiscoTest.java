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

package org.onosproject.drivers.cisco;


import org.junit.Test;
import org.onlab.packet.ChassisId;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.provider.ProviderId;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import static org.junit.Assert.assertEquals;
import static org.onosproject.net.Device.Type.SWITCH;
import static org.onosproject.net.DeviceId.deviceId;


/**
 * Tests the parser for Netconf TextBlock configurations and replies from Cisco devices.
 */
public class TextBlockParserCiscoTest {

    private static final PortNumber INTF1_PORT = PortNumber.portNumber(0);
    private static final String INTF1_NAME = "FastEthernet0/0";
    private static final PortNumber INTF2_PORT = PortNumber.portNumber(0);
    private static final String INTF2_NAME = "Ethernet1/0";
    private static final PortNumber INTF3_PORT = PortNumber.portNumber(0);
    private static final String INTF3_NAME = "GigabitEthernet2/0";
    private static final PortNumber INTF4_PORT = PortNumber.portNumber(0);
    private static final String INTF4_NAME = "Serial3/0";
    private static final PortNumber INTF5_PORT = PortNumber.portNumber(0);
    private static final String INTF5_NAME = "POS4/0";
    private static final PortNumber INTF6_PORT = PortNumber.portNumber(0);
    private static final String INTF6_NAME = "Fddi5/0";
    private static final Port.Type COPPER = Port.Type.COPPER;
    private static final Port.Type FIBER = Port.Type.FIBER;
    private static final long CONNECTION_SPEED_ETHERNET = 100000;
    private static final long CONNECTION_SPEED_SERIAL = 1544;
    private static final long CONNECTION_SPEED_POS = 9952000;
    private static final long CONNECTION_SPEED_FDDI = 100000;
    private static final boolean IS_ENABLED = true;
    private static final boolean IS_NOT_ENABLED = false;
    private static final String SHOW_VERSION = "/testShowVersion.xml";
    private static final String SHOW_INTFS = "/testShowInterfaces.xml";
    private static final String SW = "IOS C3560E 15.0(2)EJ";
    private static final String HW = "SM-X-ES3-24-P";
    private static final String MFR = "Cisco";
    private static final String SN = "FOC18401Z3R";
    private static final ProviderId PROVIDERID = new ProviderId("of", "foo");
    private static final DeviceId DEVICE = deviceId("of:foo");
    private static final ChassisId CID = new ChassisId();

    @Test
    public void controllersVersion() {
        InputStream streamOrig = getClass().getResourceAsStream(SHOW_VERSION);
        String version = new Scanner(streamOrig, "UTF-8").useDelimiter("\\Z").next();
        version = version.substring(version.indexOf('\n') + 1);
        String[] actualDetails = TextBlockParserCisco.parseCiscoIosDeviceDetails(version);

        assertEquals("Information could not be retrieved",
                     getExpectedInfo(), actualInfo(actualDetails));
    }

    @Test
    public void controllersIntfs() {
        InputStream streamOrig = getClass().getResourceAsStream(SHOW_INTFS);
        String rpcReply = new Scanner(streamOrig, "UTF-8").useDelimiter("\\Z").next();
        List<PortDescription> actualIntfs = TextBlockParserCisco.parseCiscoIosPorts(rpcReply);
        assertEquals("Information could not be retrieved",
                     getExpectedIntfs(), actualIntfs);
    }

    private DefaultDevice getExpectedInfo() {
        return new DefaultDevice(PROVIDERID, DEVICE, SWITCH, MFR, HW, SW, SN, CID);
    }

    private DefaultDevice actualInfo(String[] actualDetails) {

        return new DefaultDevice(PROVIDERID, DEVICE, SWITCH, actualDetails[0],
                                 actualDetails[1], actualDetails[2],
                                 actualDetails[3], CID);
    }

    private List<PortDescription> getExpectedIntfs() {
        DefaultAnnotations.Builder int1Annotations = DefaultAnnotations.builder()
                .set(AnnotationKeys.PORT_NAME, INTF1_NAME);
        DefaultAnnotations.Builder int2Annotations = DefaultAnnotations.builder()
                .set(AnnotationKeys.PORT_NAME, INTF2_NAME);
        DefaultAnnotations.Builder int3Annotations = DefaultAnnotations.builder()
                .set(AnnotationKeys.PORT_NAME, INTF3_NAME);
        DefaultAnnotations.Builder int4Annotations = DefaultAnnotations.builder()
                .set(AnnotationKeys.PORT_NAME, INTF4_NAME);
        DefaultAnnotations.Builder int5Annotations = DefaultAnnotations.builder()
                .set(AnnotationKeys.PORT_NAME, INTF5_NAME);
        DefaultAnnotations.Builder int6Annotations = DefaultAnnotations.builder()
                .set(AnnotationKeys.PORT_NAME, INTF6_NAME);

        List<PortDescription> intfs = new ArrayList<>();
        intfs.add(DefaultPortDescription.builder().withPortNumber(INTF1_PORT).isEnabled(IS_ENABLED)
                .type(COPPER).portSpeed(CONNECTION_SPEED_ETHERNET)
                .annotations(int1Annotations.build()).build());
        intfs.add(DefaultPortDescription.builder().withPortNumber(INTF2_PORT).isEnabled(IS_NOT_ENABLED)
                .type(COPPER).portSpeed(CONNECTION_SPEED_ETHERNET)
                .annotations(int2Annotations.build()).build());
        intfs.add(DefaultPortDescription.builder().withPortNumber(INTF3_PORT).isEnabled(IS_NOT_ENABLED)
                .type(COPPER).portSpeed(CONNECTION_SPEED_ETHERNET)
                .annotations(int3Annotations.build()).build());
        intfs.add(DefaultPortDescription.builder().withPortNumber(INTF4_PORT).isEnabled(IS_ENABLED)
                .type(COPPER).portSpeed(CONNECTION_SPEED_SERIAL)
                .annotations(int4Annotations.build()).build());
        intfs.add(DefaultPortDescription.builder().withPortNumber(INTF5_PORT).isEnabled(IS_ENABLED)
                .type(FIBER).portSpeed(CONNECTION_SPEED_POS)
                .annotations(int5Annotations.build()).build());
        intfs.add(DefaultPortDescription.builder().withPortNumber(INTF6_PORT).isEnabled(IS_ENABLED)
                .type(FIBER).portSpeed(CONNECTION_SPEED_FDDI)
                .annotations(int6Annotations.build()).build());
        return intfs;
    }

}
