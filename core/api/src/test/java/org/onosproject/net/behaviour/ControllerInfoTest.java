/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.net.behaviour;


import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onlab.packet.IpAddress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test for ControllerInfo class.
 */
public class ControllerInfoTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void tcpSslFormat() {
        String target = "tcp:192.168.1.1:6653";
        ControllerInfo controllerInfo = new ControllerInfo(target);
        assertEquals("wrong type", controllerInfo.type(), "tcp");
        assertEquals("wrong ip", controllerInfo.ip(), IpAddress.valueOf("192.168.1.1"));
        assertEquals("wrong port", controllerInfo.port(), 6653);

    }

    @Test
    public void ptcpPsslFormat() {
        String target = "ptcp:6653:192.168.1.1";
        ControllerInfo controllerInfo = new ControllerInfo(target);
        assertEquals("wrong type", controllerInfo.type(), "ptcp");
        assertEquals("wrong ip", controllerInfo.ip(), IpAddress.valueOf("192.168.1.1"));
        assertEquals("wrong port", controllerInfo.port(), 6653);

    }

    @Test
    public void unixFormat() {
        String target = "unix:file";
        thrown.expect(IllegalArgumentException.class);
        ControllerInfo controllerInfo = new ControllerInfo(target);
        assertTrue("wrong type", controllerInfo.type().contains("unix"));
        assertNull("wrong ip", controllerInfo.ip());
        assertEquals("wrong port", controllerInfo.port(), -1);

    }

    @Test
    public void defaultValues() {
        String target = "tcp:192.168.1.1";
        ControllerInfo controllerInfo = new ControllerInfo(target);
        assertEquals("wrong type", controllerInfo.type(), "tcp");
        assertEquals("wrong ip", controllerInfo.ip(), IpAddress.valueOf("192.168.1.1"));
        assertEquals("wrong port", controllerInfo.port(), 6653);
        String target1 = "ptcp:5000:";
        ControllerInfo controllerInfo2 = new ControllerInfo(target1);
        assertEquals("wrong type", controllerInfo2.type(), "ptcp");
        assertEquals("wrong ip", controllerInfo2.ip(), IpAddress.valueOf("0.0.0.0"));
        assertEquals("wrong port", controllerInfo2.port(), 5000);
        String target2 = "ptcp:";
        ControllerInfo controllerInfo3 = new ControllerInfo(target2);
        assertEquals("wrong type", controllerInfo3.type(), "ptcp");
        assertEquals("wrong ip", controllerInfo3.ip(), IpAddress.valueOf("0.0.0.0"));
        assertEquals("wrong port", controllerInfo3.port(), 6653);
    }


    @Test
    public void testEquals() {
        String target1 = "ptcp:6653:192.168.1.1";
        ControllerInfo controllerInfo1 = new ControllerInfo(target1);
        String target2 = "ptcp:6653:192.168.1.1";
        ControllerInfo controllerInfo2 = new ControllerInfo(target2);
        assertTrue("wrong equals method", controllerInfo1.equals(controllerInfo2));
    }

    @Test
    public void testListEquals() {
        String target1 = "ptcp:6653:192.168.1.1";
        ControllerInfo controllerInfo1 = new ControllerInfo(target1);
        String target2 = "ptcp:6653:192.168.1.1";
        ControllerInfo controllerInfo2 = new ControllerInfo(target2);
        String target3 = "tcp:192.168.1.1:6653";
        ControllerInfo controllerInfo3 = new ControllerInfo(target3);
        String target4 = "tcp:192.168.1.1:6653";
        ControllerInfo controllerInfo4 = new ControllerInfo(target4);
        List<ControllerInfo> list1 = new ArrayList<>(Arrays.asList(controllerInfo1, controllerInfo3));
        List<ControllerInfo> list2 = new ArrayList<>(Arrays.asList(controllerInfo2, controllerInfo4));
        assertTrue("wrong equals list method", list1.equals(list2));
    }
}
