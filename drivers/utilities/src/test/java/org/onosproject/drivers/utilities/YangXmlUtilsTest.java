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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.net.behaviour.ControllerInfo;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Tests for the XMLYangUtils.
 */
public class YangXmlUtilsTest {
    public static final String OF_CONFIG_XML_PATH = "/of-config/of-config.xml";
    private YangXmlUtilsAdap utils;
    private XMLConfiguration testCreateConfig;

    @Before
    public void setUp() throws Exception {
        assertTrue("No resource for test", YangXmlUtilsTest.class.
                getResourceAsStream("/of-config/of-config.xml") != null);
        utils = new YangXmlUtilsAdap();

        testCreateConfig = new XMLConfiguration();
    }

    /**
     * Tests getting a single object configuration via passing the path and the map of the desired values.
     *
     * @throws ConfigurationException if the testing xml file is not there.
     */
    @Test
    public void testGetXmlUtilsInstance() throws ConfigurationException {

        YangXmlUtils instance1 = YangXmlUtils.getInstance();
        YangXmlUtils instance2 = YangXmlUtils.getInstance();

        assertEquals("Duplicate instance", instance1, instance2);

    }

    private String canonicalXml(String s) {
        String[] lines = s.split("\n");
        StringBuilder xml = new StringBuilder();
        for (String line : lines) {
            if (line.contains("<")) {
                xml.append(line);
                xml.append("\n");
            }
        }
        return xml.toString().trim();
    }

    /**
     * Tests getting a single object configuration via passing the path and the map of the desired values.
     *
     * @throws ConfigurationException if the testing xml file is not there.
     */
    @Test
    public void testGetXmlConfigurationFromMap() throws ConfigurationException {
        Map<String, String> pathAndValues = new HashMap<>();
        pathAndValues.put("capable-switch.id", "openvswitch");
        pathAndValues.put("switch.id", "ofc-bridge");
        pathAndValues.put("controller.id", "tcp:1.1.1.1:1");
        pathAndValues.put("controller.ip-address", "1.1.1.1");
        XMLConfiguration cfg = utils.getXmlConfiguration(OF_CONFIG_XML_PATH, pathAndValues);
        testCreateConfig.load(getClass().getResourceAsStream("/testCreateSingleYangConfig.xml"));
        assertNotEquals("Null testConfiguration", new XMLConfiguration(), testCreateConfig);

        assertEquals("Wrong configuaration", IteratorUtils.toList(testCreateConfig.getKeys()),
                     IteratorUtils.toList(cfg.getKeys()));

        assertEquals("Wrong string configuaration", canonicalXml(utils.getString(testCreateConfig)),
                     canonicalXml(utils.getString(cfg)));
    }

    /**
     * Tests getting a multiple object nested configuration via passing the path
     * and a list of YangElements containing with the element and desired value.
     *
     * @throws ConfigurationException
     */
    @Test
    public void getXmlConfigurationFromYangElements() throws ConfigurationException {

        assertNotEquals("Null testConfiguration", new XMLConfiguration(), testCreateConfig);
        testCreateConfig.load(getClass().getResourceAsStream("/testYangConfig.xml"));
        List<YangElement> elements = new ArrayList<>();
        elements.add(new YangElement("capable-switch", ImmutableMap.of("id", "openvswitch")));
        elements.add(new YangElement("switch", ImmutableMap.of("id", "ofc-bridge")));
        List<ControllerInfo> controllers =
                ImmutableList.of(new ControllerInfo(IpAddress.valueOf("1.1.1.1"), 1, "tcp"),
                                 new ControllerInfo(IpAddress.valueOf("2.2.2.2"), 2, "tcp"));
        controllers.forEach(cInfo -> {
            elements.add(new YangElement("controller", ImmutableMap.of("id", cInfo.target(),
                                                                       "ip-address", cInfo.ip().toString())));
        });
        XMLConfiguration cfg =
                new XMLConfiguration(YangXmlUtils.getInstance()
                                             .getXmlConfiguration(OF_CONFIG_XML_PATH, elements));
        assertEquals("Wrong configuaration", IteratorUtils.toList(testCreateConfig.getKeys()),
                     IteratorUtils.toList(cfg.getKeys()));
        assertEquals("Wrong string configuaration", canonicalXml(utils.getString(testCreateConfig)),
                     canonicalXml(utils.getString(cfg)));
    }

    /**
     * Test reading an XML configuration and retrieving the requested elements.
     *
     * @throws ConfigurationException
     */
    @Test
    public void testReadLastXmlConfiguration() throws ConfigurationException {
        testCreateConfig.load(getClass().getResourceAsStream("/testYangConfig.xml"));
        List<YangElement> elements = utils.readXmlConfiguration(testCreateConfig,
                                                                "controller");
        List<YangElement> expected = ImmutableList.of(
                new YangElement("controller", ImmutableMap.of("id", "tcp:1.1.1.1:1",
                                                              "ip-address", "1.1.1.1")),
                new YangElement("controller", ImmutableMap.of("id", "tcp:2.2.2.2:2",
                                                              "ip-address", "2.2.2.2")));
        assertEquals("Wrong elements collected", expected, elements);
    }

    /**
     * Test reading an XML configuration and retrieving the requested elements.
     *
     * @throws ConfigurationException
     */
    @Test
    public void testReadNestedXmlConfiguration() throws ConfigurationException {
        testCreateConfig.load(getClass().getResourceAsStream("/testYangConfig.xml"));
        List<YangElement> elements = utils.readXmlConfiguration(testCreateConfig, "controllers");
        List<YangElement> expected = ImmutableList.of(
                new YangElement("controllers", ImmutableMap.of("controller.id", "tcp:1.1.1.1:1",
                                                               "controller.ip-address", "1.1.1.1")),
                new YangElement("controllers", ImmutableMap.of("controller.id", "tcp:2.2.2.2:2",
                                                               "controller.ip-address", "2.2.2.2")));
        assertEquals("Wrong elements collected", expected, elements);
    }

    //enables to change the path to the resources directory.
    private class YangXmlUtilsAdap extends YangXmlUtils {

        @Override
        protected InputStream getCfgInputStream(String file) {
            return YangXmlUtilsAdap.class.getResourceAsStream(file);
        }
    }
}
