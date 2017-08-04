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

package org.onosproject.drivers.utilities;

import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.net.behaviour.ControllerInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Test the XML document Parsing for netconf configuration.
 */
public class XmlConfigParserTest {


    @Test
    public void basics() throws IOException {
        InputStream stream = getClass().getResourceAsStream("/testConfig.xml");
        List<ControllerInfo> controllers = XmlConfigParser
                .parseStreamControllers(XmlConfigParser.loadXml(stream));
        assertTrue(controllers.get(0).equals(new ControllerInfo(
                IpAddress.valueOf("10.128.12.1"), 6653, "tcp")));
        assertTrue(controllers.get(1).equals(new ControllerInfo(
                IpAddress.valueOf("10.128.12.2"), 6654, "tcp")));

    }

    @Test
    public void switchId() {
        InputStream stream = getClass().getResourceAsStream("/testConfig.xml");
        String switchId = XmlConfigParser.parseSwitchId(XmlConfigParser
                                                                .loadXml(stream));
        assertTrue("ofc-bridge".equals(switchId));
    }

    @Test
    public void capableSwitchId() {
        InputStream stream = getClass().getResourceAsStream("/testConfig.xml");
        String capableSwitchId = XmlConfigParser
                .parseCapableSwitchId(XmlConfigParser.loadXml(stream));
        assertTrue("openvswitch".equals(capableSwitchId));
    }

    @Test
    public void controllersConfig() {
        InputStream streamOrig = getClass().getResourceAsStream("/testConfig.xml");
        InputStream streamCfg = XmlConfigParser.class.getResourceAsStream("/controllers.xml");
        String config = XmlConfigParser
                .createControllersConfig(XmlConfigParser.loadXml(streamCfg),
                                         XmlConfigParser.loadXml(streamOrig),
                                         "running", "merge", "create",
                                         new ArrayList<>(
                                                 Arrays.asList(
                                                         new ControllerInfo(
                                                                 IpAddress.valueOf(
                                                                         "192.168.1.1"),
                                                                 5000, "tcp"))));
        assertTrue(config.contains("192.168.1.1"));
        assertTrue(config.contains("tcp"));
        assertTrue(config.contains("5000"));

    }
}
