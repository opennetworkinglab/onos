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

package org.onosproject.drivers.cisco;


import org.junit.Test;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.PortDescription;

import java.io.InputStream;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;



import static org.junit.Assert.assertEquals;

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
    private static final String TEXT_FILE = "/CiscoIosInterfaces.xml";

    @Test
    public void controllersConfig() {
        InputStream streamOrig = getClass().getResourceAsStream(TEXT_FILE);
        String rpcReply = new Scanner(streamOrig, "UTF-8").useDelimiter("\\Z").next();
        List<PortDescription> actualIntfs = TextBlockParserCisco.parseCiscoIosPorts(rpcReply);
        assertEquals("Interfaces were not retrieved from configuration",
                     getExpectedIntfs(), actualIntfs);
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
        intfs.add(new DefaultPortDescription(INTF1_PORT, IS_ENABLED, COPPER, CONNECTION_SPEED_ETHERNET,
                                             int1Annotations.build()));
        intfs.add(new DefaultPortDescription(INTF2_PORT, IS_NOT_ENABLED, COPPER, CONNECTION_SPEED_ETHERNET,
                                             int2Annotations.build()));
        intfs.add(new DefaultPortDescription(INTF3_PORT, IS_NOT_ENABLED, COPPER, CONNECTION_SPEED_ETHERNET,
                                             int3Annotations.build()));
        intfs.add(new DefaultPortDescription(INTF4_PORT, IS_ENABLED, COPPER, CONNECTION_SPEED_SERIAL,
                                             int4Annotations.build()));
        intfs.add(new DefaultPortDescription(INTF5_PORT, IS_ENABLED, FIBER, CONNECTION_SPEED_POS,
                                             int5Annotations.build()));
        intfs.add(new DefaultPortDescription(INTF6_PORT, IS_ENABLED, FIBER, CONNECTION_SPEED_FDDI,
                                             int6Annotations.build()));
        return intfs;
    }
}
