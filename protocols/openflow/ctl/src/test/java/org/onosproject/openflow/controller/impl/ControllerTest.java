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
package org.onosproject.openflow.controller.impl;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.onlab.junit.TestTools;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverAdapter;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverServiceAdapter;
import org.onosproject.openflow.OFDescStatsReplyAdapter;
import org.onosproject.openflow.OpenflowSwitchDriverAdapter;
import org.onosproject.openflow.controller.driver.OpenFlowSwitchDriver;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.io.ByteStreams.toByteArray;
import static com.google.common.io.Files.write;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit tests for the OpenFlow controller class.
 */
public class ControllerTest {

    @ClassRule
    public static TemporaryFolder testFolder = new TemporaryFolder();

    Controller controller;
    protected static final Logger log = LoggerFactory.getLogger(ControllerTest.class);

    private class TestDriver extends DriverAdapter {
        @SuppressWarnings("unchecked")
        @Override
        public <T extends Behaviour> T createBehaviour(DriverHandler handler, Class<T> behaviourClass) {
            if (behaviourClass == OpenFlowSwitchDriver.class) {
                return (T) new OpenflowSwitchDriverAdapter();
            }
            return null;
        }
    }

    /*
     * Writes the necessary file for the tests in the temporary directory
     */
    private static File stageTestResource(String name) throws IOException {
        File file = new File(testFolder.newFolder(), name);
        byte[] bytes = toByteArray(ControllerTest.class.getResourceAsStream(name));
        write(bytes, file);
        return file;
    }

    class MockDriverService extends DriverServiceAdapter {
        static final int NO_SUCH_DRIVER_ID = 1;
        static final int ITEM_NOT_FOUND_DRIVER_ID = 2;
        static final int DRIVER_EXISTS_ID = 3;

        static final String BASE_DRIVER_NAME = "of:000000000000000";

        static final String NO_SUCH_DRIVER = BASE_DRIVER_NAME
                + NO_SUCH_DRIVER_ID;
        static final String ITEM_NOT_FOUND_DRIVER = BASE_DRIVER_NAME
                + ITEM_NOT_FOUND_DRIVER_ID;
        static final String DRIVER_EXISTS = BASE_DRIVER_NAME
                + DRIVER_EXISTS_ID;

        @Override
        public Driver getDriver(DeviceId deviceId) {
            switch (deviceId.toString()) {
                case NO_SUCH_DRIVER:
                    return null;
                case ITEM_NOT_FOUND_DRIVER:
                    throw new ItemNotFoundException();
                case DRIVER_EXISTS:
                    return new TestDriver();
                default:
                    throw new AssertionError();
            }
        }
    }

    /**
     * Creates and initializes a new controller.
     */
    @Before
    public void setUp() {
        controller = new Controller();
        Dictionary<String, String> properties = new Hashtable<>();
        properties.put("openflowPorts",
                       Integer.toString(TestTools.findAvailablePort(0)));
        controller.setConfigParams(properties);
    }

    /**
     * Tests fetching a driver that does not exist.
     */
    @Test
    public void switchInstanceNotFoundTest() {
        controller.start(null, new MockDriverService());
        OpenFlowSwitchDriver driver =
                controller.getOFSwitchInstance(MockDriverService.NO_SUCH_DRIVER_ID,
                                               null,
                                               null);
        assertThat(driver, nullValue());
        controller.stop();
    }

    /**
     * Tests fetching a driver that throws an ItemNotFoundException.
     */
    @Test
    public void switchItemNotFoundTest() {
        controller.start(null, new MockDriverService());
        OFDescStatsReply stats =
                new OFDescStatsReplyAdapter();
        OpenFlowSwitchDriver driver =
                controller.getOFSwitchInstance(MockDriverService.ITEM_NOT_FOUND_DRIVER_ID,
                                               stats,
                                               null);
        assertThat(driver, nullValue());
        controller.stop();
    }

    /**
     * Tests fetching a driver that throws an ItemNotFoundException.
     */
    @Test
    public void driverExistsTest() {
        controller.start(null, new MockDriverService());
        OFDescStatsReply stats =
                new OFDescStatsReplyAdapter();
        OpenFlowSwitchDriver driver =
                controller.getOFSwitchInstance(MockDriverService.DRIVER_EXISTS_ID,
                                               stats,
                                               null);
        assertThat(driver, notNullValue());
        controller.stop();
    }

    /**
     * Tests configuring the controller.
     */
    @Test
    public void testConfiguration() {
        Dictionary<String, String> properties = new Hashtable<>();
        properties.put("openflowPorts", "1,2,3,4,5");
        properties.put("workerThreads", "5");

        controller.setConfigParams(properties);
        IntStream.rangeClosed(1, 5)
                .forEach(i -> assertThat(controller.openFlowPorts, hasItem(i)));
        assertThat(controller.workerThreads, is(5));
    }

    /**
     * Tests the SSL/TLS methods in the controller.
     */
    @Test
    public void testSsl() throws IOException {
        File keystore = stageTestResource("ControllerTestKeystore.jks");
        String keystoreName = keystore.getAbsolutePath();

        System.setProperty("enableOFTLS", Boolean.toString(Boolean.TRUE));
        System.setProperty("javax.net.ssl.keyStore", keystoreName);
        System.setProperty("javax.net.ssl.trustStore", keystoreName);
        System.setProperty("javax.net.ssl.keyStorePassword", "password");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");
        Dictionary<String, String> properties = new Hashtable<>();
        properties.put("openflowPorts",
                       Integer.toString(TestTools.findAvailablePort(0)));
        properties.put("workerThreads", "0");

        controller.setConfigParams(properties);
        controller.start(null, new MockDriverService());

        assertThat(controller.sslContext, notNullValue());

        controller.stop();
        boolean removed = keystore.delete();
        if (!removed) {
            log.warn("Could not remove temporary file");
        }
    }

    /**
     * Tests controll utility health methods.
     */
    @Test
    public void testHealth() {
        Map<String, Long> memory = controller.getMemory();
        assertThat(memory.size(), is(2));
        assertThat(memory.get("total"), is(not(0)));
        assertThat(memory.get("free"), is(not(0)));

        long startTime = controller.getSystemStartTime();
        assertThat(startTime, lessThan(System.currentTimeMillis()));

        long upTime = controller.getSystemUptime();
        assertThat(upTime, lessThan(30L * 1000));
    }
}
