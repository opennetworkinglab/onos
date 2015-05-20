/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.provider.netconf.device.impl;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertFalse;
import static org.onlab.util.Tools.delay;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.ChassisId;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.provider.ProviderId;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import com.tailf.jnc.JNCException;

/**
 * Test Case to Validate Netconf Device Provider.
 *
 */
public class NetconfDeviceProviderTest {
    // private NetconfDevice device;

    TestDeviceCreator create;

    private final Logger log = getLogger(NetconfDeviceProviderTest.class);

    private Map<DeviceId, NetconfDevice> netconfDeviceMap = new ConcurrentHashMap<DeviceId, NetconfDevice>();

    private DeviceProviderService providerService;

    private static final int EVENTINTERVAL = 5;

    private static final String SCHEME = "netconf";

    private static final DeviceId DID1 = DeviceId
            .deviceId("of:0000000000000001");

    private final NetconfDeviceProvider provider = new NetconfDeviceProvider();
    private final TestDeviceRegistry registry = new TestDeviceRegistry();

    private ComponentConfigService mockCfgService;

    @Before
    public void setUp() {
        mockCfgService = EasyMock.createMock(ComponentConfigService.class);
        provider.cfgService = mockCfgService;
        provider.providerRegistry = registry;
    }

    @SuppressWarnings("unchecked")
    private Dictionary<String, String> getDictionaryMock(ComponentContext componentContext) {
        Dictionary<String, String> dictionary = EasyMock
                .createMock(Dictionary.class);
        expect(dictionary.get("devConfigs"))
                .andReturn("cisco:cisco@10.18.11.14:22:active,"
                                   + "sanjay:b33rb3lly@10.18.24.122:2022:inactive");
        replay(dictionary);
        expect(componentContext.getProperties()).andReturn(dictionary);
        return dictionary;
    }

    @SuppressWarnings("unchecked")
    private Dictionary<String, String> getDictionaryMockWithoutValues(ComponentContext componentContext) {
        Dictionary<String, String> dictionary = EasyMock
                .createMock(Dictionary.class);
        expect(dictionary.get("devConfigs")).andReturn("");
        replay(dictionary);
        expect(componentContext.getProperties()).andReturn(dictionary);
        return dictionary;
    }

    @SuppressWarnings("unchecked")
    private Dictionary<String, String> getDictionaryMockWithDeviceEntryNull(ComponentContext componentContext) {
        Dictionary<String, String> dictionary = EasyMock
                .createMock(Dictionary.class);
        expect(dictionary.get("devConfigs")).andReturn("null,null");
        replay(dictionary);
        expect(componentContext.getProperties()).andReturn(dictionary);
        return dictionary;
    }

    @SuppressWarnings("unchecked")
    private Dictionary<String, String> getDictionaryMockDeviceEntryNumberFomatEx(ComponentContext componentContext) {
        Dictionary<String, String> dictionary = EasyMock
                .createMock(Dictionary.class);
        expect(dictionary.get("devConfigs"))
                .andReturn("cisco:cisco@10.18.11.14:cisco:active")
                .andThrow(new NumberFormatException());
        replay(dictionary);
        expect(componentContext.getProperties()).andReturn(dictionary);
        return dictionary;
    }

    @SuppressWarnings("unchecked")
    private Dictionary<String, String> getDictionaryMockWithoutUsernameAndPassword(ComponentContext componentContext) {
        Dictionary<String, String> dictionary = EasyMock
                .createMock(Dictionary.class);
        expect(dictionary.get("devConfigs"))
                .andReturn("null:null@null:0:active");
        replay(dictionary);
        expect(componentContext.getProperties()).andReturn(dictionary);
        return dictionary;
    }

    @SuppressWarnings("unchecked")
    private Dictionary<String, String> getDictionaryMockWithDifferentDeviceState(ComponentContext componentContext) {
        Dictionary<String, String> dictionary = EasyMock
                .createMock(Dictionary.class);
        expect(dictionary.get("devConfigs"))
                .andReturn("cisco:cisco@10.18.11.14:22:active,cisco:cisco@10.18.11.18:22:inactive,"
                                   + "cisco:cisco@10.18.11.14:22:invalid,cisco:cisco@10.18.11.14:22:null");
        replay(dictionary);
        expect(componentContext.getProperties()).andReturn(dictionary);
        return dictionary;
    }

    @SuppressWarnings("unchecked")
    private Dictionary<String, String> getDictionaryMockDeviceWithArrayOutOFBoundEx(ComponentContext componentContext) {
        Dictionary<String, String> dictionary = EasyMock
                .createMock(Dictionary.class);
        expect(dictionary.get("devConfigs"))
                .andReturn("@10.18.11.14:22:active")
                .andThrow(new ArrayIndexOutOfBoundsException());
        replay(dictionary);
        expect(componentContext.getProperties()).andReturn(dictionary);
        return dictionary;
    }

    @SuppressWarnings("unchecked")
    private Dictionary<String, String> getDictionaryMockDeviceEntryForDeactivate(ComponentContext componentContext) {
        Dictionary<String, String> dictionary = EasyMock
                .createMock(Dictionary.class);
        expect(dictionary.get("devConfigs"))
                .andReturn("netconf:cisco@10.18.11.14:22:active")
                .andThrow(new ArrayIndexOutOfBoundsException());
        replay(dictionary);
        expect(componentContext.getProperties()).andReturn(dictionary);
        return dictionary;
    }

    @Test(expected = SocketTimeoutException.class)
    public void testSSHAuthentication() throws JNCException, IOException {
        NetconfDevice netconfDevice = new NetconfDevice("10.18.14.19", 22,
                                                        "cisco", "cisco");
        netconfDevice.setConnectTimeout(1000);
        TestDeviceCreator objForTestDev = new TestDeviceCreator(netconfDevice,
                                                                true);
        objForTestDev.run();
    }

    @After
    public void tearDown() {
        provider.providerRegistry = null;
        provider.cfgService = null;
    }

    @Test
    public void testActiveWithComponentContext() {

        ComponentContext componentContext = EasyMock
                .createMock(ComponentContext.class);
        getDictionaryMock(componentContext);
        replay(componentContext);
        provider.activate(componentContext);
    }

    // To check if deviceCfgValue is empty or null
    @Test
    public void testActiveWithcomponentContextIsNull() {

        ComponentContext componentContext = EasyMock
                .createMock(ComponentContext.class);
        getDictionaryMockWithoutValues(componentContext);
        replay(componentContext);
        provider.activate(componentContext);
    }

    // To check deviceEntry and device is null
    @Test
    public void testActiveWithDeviceEntryIsNull() {

        ComponentContext componentContext = EasyMock
                .createMock(ComponentContext.class);
        getDictionaryMockWithDeviceEntryNull(componentContext);
        replay(componentContext);
        provider.activate(componentContext);
    }

    @Test
    public void testActiveWithDeviceEntryWithoutUsernameAndPassword() {

        ComponentContext componentContext = EasyMock
                .createMock(ComponentContext.class);
        getDictionaryMockWithoutUsernameAndPassword(componentContext);
        replay(componentContext);
        provider.activate(componentContext);
    }

    @Test
    public void testActiveWithDeviceEntryWithNumberFomatEx() {

        ComponentContext componentContext = EasyMock
                .createMock(ComponentContext.class);
        getDictionaryMockDeviceEntryNumberFomatEx(componentContext);
        replay(componentContext);
        provider.activate(componentContext);
    }

    @Test
    public void testActiveWithDeviceEntryWithDifferentDeviceState() {

        ComponentContext componentContext = EasyMock
                .createMock(ComponentContext.class);
        getDictionaryMockWithDifferentDeviceState(componentContext);
        replay(componentContext);
        provider.activate(componentContext);
    }

    @Test
    public void testActiveWithDeviceEntryWithArrayOutOFBoundEx() {

        ComponentContext componentContext = EasyMock
                .createMock(ComponentContext.class);
        getDictionaryMockDeviceWithArrayOutOFBoundEx(componentContext);
        replay(componentContext);
        provider.activate(componentContext);
    }

    @Test
    public void isReachableWithInvalidDeviceId() {
        assertFalse("Initially the Device ID Should not be reachable",
                    provider.isReachable(DID1));
        NetconfDevice device = new NetconfDevice("", 0, "", "");
        provider.netconfDeviceMap.put(DID1, device);
        assertFalse("Particular Device ID cannot be Reachable",
                    provider.isReachable(DID1));
    }

    @Test
    public void testDeactivate() {

        ComponentContext componentContext = EasyMock
                .createMock(ComponentContext.class);
        getDictionaryMockDeviceEntryForDeactivate(componentContext);
        replay(componentContext);
        testActiveWithComponentContext();
        provider.deactivate(componentContext);
    }

    private class TestDeviceCreator {

        private NetconfDevice device;
        private boolean createFlag;

        public TestDeviceCreator(NetconfDevice device, boolean createFlag) {
            this.device = device;
            this.createFlag = createFlag;
        }

        public void run() throws JNCException, IOException {
            if (createFlag) {
                log.info("Trying to create Device Info on ONOS core");
                advertiseDevices();
            } else {
                log.info("Trying to remove Device Info on ONOS core");
                removeDevices();
            }
        }

        /**
         * For each Netconf Device, remove the entry from the device store.
         * @throws URISyntaxException
         */
        private void removeDevices() {
            if (device == null) {
                log.warn("The Request Netconf Device is null, cannot proceed further");
                return;
            }
            try {
                DeviceId did = getDeviceId();
                if (!netconfDeviceMap.containsKey(did)) {
                    log.error("BAD Request: 'Currently device is not discovered, "
                            + "so cannot remove/disconnect the device: "
                            + device.deviceInfo() + "'");
                    return;
                }
                providerService.deviceDisconnected(did);
                device.disconnect();
                netconfDeviceMap.remove(did);
                delay(EVENTINTERVAL);
            } catch (URISyntaxException uriSyntaxExcpetion) {
                log.error("Syntax Error while creating URI for the device: "
                                  + device.deviceInfo()
                                  + " couldn't remove the device from the store",
                          uriSyntaxExcpetion);
            }
        }

        /**
         * Initialize Netconf Device object, and notify core saying device
         * connected.
         */
        private void advertiseDevices()
                throws JNCException, IOException, SocketTimeoutException {
            try {
                if (device == null) {
                    log.warn("The Request Netconf Device is null, cannot proceed further");
                    return;
                }
                device.init();
                DeviceId did = getDeviceId();
                ChassisId cid = new ChassisId();
                DeviceDescription desc = new DefaultDeviceDescription(
                                                                      did.uri(),
                                                                      Device.Type.OTHER,
                                                                      "", "",
                                                                      "", "",
                                                                      cid);
                log.info("Persisting Device" + did.uri().toString());

                netconfDeviceMap.put(did, device);
                providerService.deviceConnected(did, desc);
                log.info("Done with Device Info Creation on ONOS core. Device Info: "
                        + device.deviceInfo() + " " + did.uri().toString());
                delay(EVENTINTERVAL);
            } catch (URISyntaxException e) {
                log.error("Syntax Error while creating URI for the device: "
                        + device.deviceInfo()
                        + " couldn't persist the device onto the store", e);
            } catch (JNCException e) {
                throw e;
            } catch (SocketTimeoutException e) {
                throw e;
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                log.error("Error while initializing session for the device: "
                        + device.deviceInfo(), e);
            }
        }

        private DeviceId getDeviceId() throws URISyntaxException {
            String additionalSSP = new StringBuilder(device.getUsername())
                    .append("@").append(device.getSshHost()).append(":")
                    .append(device.getSshPort()).toString();
            DeviceId did = DeviceId.deviceId(new URI(SCHEME, additionalSSP,
                                                     null));
            return did;
        }
    }

    private class TestDeviceRegistry implements DeviceProviderRegistry {

        @Override
        public DeviceProviderService register(DeviceProvider provider) {
            return new TestProviderService();
        }

        @Override
        public void unregister(DeviceProvider provider) {
        }

        @Override
        public Set<ProviderId> getProviders() {
            return null;
        }

        private class TestProviderService implements DeviceProviderService {

            public DeviceProvider provider() {
                return null;
            }

            public void deviceConnected(DeviceId deviceId,
                                        DeviceDescription deviceDescription) {
            }

            public void deviceDisconnected(DeviceId deviceId) {

            }

            public void updatePorts(DeviceId deviceId,
                                    List<PortDescription> portDescriptions) {

            }

            public void portStatusChanged(DeviceId deviceId,
                                          PortDescription portDescription) {

            }

            public void receivedRoleReply(DeviceId deviceId,
                                          MastershipRole requested,
                                          MastershipRole response) {

            }

            public void updatePortStatistics(DeviceId deviceId,
                                             Collection<PortStatistics> portStatistics) {

            }
        }
    }
}
