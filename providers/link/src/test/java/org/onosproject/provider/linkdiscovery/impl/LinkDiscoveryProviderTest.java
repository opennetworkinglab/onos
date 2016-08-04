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
package org.onosproject.provider.linkdiscovery.impl;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ComponentContextAdapter;
import org.onlab.packet.ChassisId;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.net.AbstractProjectableModel;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Annotations;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.LinkDiscovery;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverAdapter;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverServiceAdapter;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkProviderRegistryAdapter;
import org.onosproject.net.link.LinkProviderServiceAdapter;
import org.onosproject.net.link.LinkServiceAdapter;
import org.onosproject.net.provider.ProviderId;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static org.onlab.junit.TestTools.assertAfter;
import static org.onosproject.provider.linkdiscovery.impl.LinkDiscoveryProvider.APP_NAME;
import static org.onosproject.provider.linkdiscovery.impl.LinkDiscoveryProvider.SCHEME_NAME;

/**
 * Test for polling mechanism of the NetconfLinkProvider.
 */
public class LinkDiscoveryProviderTest {

    private static final ComponentContextAdapter CONTEXT =
            new ComponentContextAdapter() {
                @Override
                public Dictionary getProperties() {
                    Hashtable<String, Integer> props = new Hashtable<>();
                    props.put("linkPollFrequencySeconds", 2);
                    props.put("linkPollDelaySeconds", 1);
                    return props;
                }
            };
    // Network Mocks
    private static final DeviceId DEVICE_ID_1 = DeviceId.deviceId("netconf:1.1.1.1");
    private static final DeviceId DEVICE_ID_2 = DeviceId.deviceId("netconf:1.1.1.2");
    private static final ConnectPoint CP11 = new ConnectPoint(DEVICE_ID_1, PortNumber.portNumber(1));
    private static final ConnectPoint CP12 = new ConnectPoint(DEVICE_ID_1, PortNumber.portNumber(2));
    private static final ConnectPoint CP13 = new ConnectPoint(DEVICE_ID_1, PortNumber.portNumber(3));
    private static final ConnectPoint CP14 = new ConnectPoint(DEVICE_ID_1, PortNumber.portNumber(4));
    private static final ConnectPoint CP21 = new ConnectPoint(DEVICE_ID_2, PortNumber.portNumber(1));
    private static final ConnectPoint CP22 = new ConnectPoint(DEVICE_ID_2, PortNumber.portNumber(2));
    private static final ConnectPoint CP23 = new ConnectPoint(DEVICE_ID_2, PortNumber.portNumber(3));
    private static final ConnectPoint CP24 = new ConnectPoint(DEVICE_ID_2, PortNumber.portNumber(4));
    private static final DefaultAnnotations DEVICE_ANNOTATIONS = DefaultAnnotations.builder()
            .set(AnnotationKeys.PROTOCOL, SCHEME_NAME.toUpperCase()).build();
    private static final DefaultAnnotations LINK_ANNOTATIONS = DefaultAnnotations.builder()
            .set(AnnotationKeys.PROTOCOL, SCHEME_NAME.toUpperCase()).build();
    private static final LinkKey LINKKEY1 = LinkKey.linkKey(CP11, CP21);
    private static final LinkDescription LINK1 = new DefaultLinkDescription(CP11, CP21,
                                                                            Link.Type.DIRECT,
                                                                            LINK_ANNOTATIONS);
    private static final LinkKey LINKKEY2 = LinkKey.linkKey(CP12, CP22);
    private static final LinkDescription LINK2 = new DefaultLinkDescription(CP12, CP22,
                                                                            Link.Type.DIRECT,
                                                                            LINK_ANNOTATIONS);
    private static final LinkKey LINKKEY3 = LinkKey.linkKey(CP13, CP23);
    private static final LinkDescription LINK3 = new DefaultLinkDescription(CP13, CP23,
                                                                            Link.Type.DIRECT,
                                                                            DefaultAnnotations.builder()
                                                                                    .build());
    private static final LinkKey LINKKEY4 = LinkKey.linkKey(CP14, CP24);
    private static final LinkDescription LINK4 = new DefaultLinkDescription(CP14, CP24,
                                                                            Link.Type.DIRECT,
                                                                            DefaultAnnotations.builder().build());
    //Service Mocks
    private final MockDeviceService deviceService = new MockDeviceService();
    private final LinkProviderRegistryAdapter linkRegistry = new LinkProviderRegistryAdapter();
    private final MastershipService mastershipService = new MockMastershipService();
    private final MockLinkService linkService = new MockLinkService();
    private final Driver driver = new MockDriver();
    //Provider related classes
    private LinkProviderServiceAdapter providerService;
    private CoreService coreService;
    private LinkDiscoveryProvider provider = new LinkDiscoveryProvider();
    private final Device device1 = new MockDevice(provider.id(), DEVICE_ID_1, Device.Type.SWITCH,
                                                  "foo.inc", "0", "0", "0", new ChassisId(),
                                                  DEVICE_ANNOTATIONS);
    private Set<DeviceListener> deviceListeners = new HashSet<>();
    private ApplicationId appId =
            new DefaultApplicationId(100, APP_NAME);
    private TestLink testLink = new TestLink();

    @Before
    public void setUp() {
        coreService = createMock(CoreService.class);
        expect(coreService.registerApplication(appId.name()))
                .andReturn(appId).anyTimes();
        replay(coreService);
        provider.coreService = coreService;
        provider.providerRegistry = linkRegistry;
        provider.deviceService = deviceService;
        provider.mastershipService = mastershipService;
        provider.linkService = linkService;
        provider.cfgService = new ComponentConfigAdapter();
        AbstractProjectableModel.setDriverService(null, new DriverServiceAdapter());
        provider.activate(null);
        providerService = linkRegistry.registeredProvider();
    }

    @Test
    public void activate() throws Exception {
        assertFalse("Provider should be registered", linkRegistry.getProviders().contains(provider));
        assertEquals("Device service should be registered", provider.deviceService, deviceService);
        assertEquals("Device listener should be added", 1, deviceListeners.size());
        assertNotNull("Registration expected", providerService);
        assertEquals("Incorrect  provider", provider, providerService.provider());
        assertFalse("Executor should be running", provider.executor.isShutdown());
        assertFalse("Executor should be running", provider.executor.isTerminated());
        assertEquals("Incorrect polling frequency, should be default", 10,
                     provider.linkPollFrequencySeconds);
        assertEquals("Incorrect polling delay , should be default", 20,
                     provider.linkPollDelaySeconds);
    }

    @Test
    public void modified() throws Exception {
        provider.modified(CONTEXT);
        assertEquals("Incorrect polling frequency, should be default", 2,
                     provider.linkPollFrequencySeconds);
        assertEquals("Incorrect polling delay , should be default", 1,
                     provider.linkPollDelaySeconds);

    }


    @Test
    public void deactivate() throws Exception {
        provider.deactivate();
        assertEquals("Device listener should be removed", 0, deviceListeners.size());
        assertFalse("Provider should not be registered", linkRegistry.getProviders().contains(provider));
        assertTrue(provider.executor.isShutdown());
        assertNull(provider.providerService);
    }


    @Test
    public void linksTestForStoredDevice() {
        provider.modified(CONTEXT);
        providerService.discoveredLinkDescriptions().put(LINKKEY1, LINK1);
        providerService.discoveredLinkDescriptions().put(LINKKEY2, LINK2);
        providerService.discoveredLinkDescriptions().put(LINKKEY4, LINK4);
        testLink.addLinkDesc(LINK2);
        testLink.addLinkDesc(LINK3);
        assertAfter(1100, () -> {
            assertEquals("Total number of link must be 3", 3, providerService.discoveredLinkDescriptions().size());
            assertFalse("Link1 should be removed",
                        providerService.discoveredLinkDescriptions().containsKey(LINKKEY1));
            assertTrue("Link2 should be present",
                       providerService.discoveredLinkDescriptions().containsKey(LINKKEY2));
            assertTrue("Link3 should be added",
                       providerService.discoveredLinkDescriptions().containsKey(LINKKEY3));
            assertEquals("Link3 should be annotated", SCHEME_NAME.toUpperCase(),
                         providerService.discoveredLinkDescriptions()
                    .get(LINKKEY3).annotations().value(AnnotationKeys.PROTOCOL));
            assertTrue("Link4 should be present because it is not related to the LinkDiscovery",
                       providerService.discoveredLinkDescriptions().containsKey(LINKKEY4));

        });
        clear();
    }

    private void clear() {
        testLink.clearLinkDesc();
        providerService.discoveredLinkDescriptions().clear();
        providerService.discoveredLinks().clear();
    }

    private class MockDeviceService extends DeviceServiceAdapter {

        @Override
        public Iterable<Device> getAvailableDevices() {
            return ImmutableSet.of(device1);
        }

        @Override
        public void addListener(DeviceListener listener) {
            deviceListeners.add(listener);
        }

        @Override
        public void removeListener(DeviceListener listener) {
            deviceListeners.remove(listener);
        }
    }

    private class MockMastershipService extends MastershipServiceAdapter {

        @Override
        public boolean isLocalMaster(DeviceId deviceId) {
            return true;
        }
    }

    private class MockLinkService extends LinkServiceAdapter {
        @Override
        public Set<Link> getDeviceEgressLinks(DeviceId deviceId) {
            Set<Link> links = new HashSet<>();
            providerService.discoveredLinkDescriptions().values()
                    .forEach(x -> links.add(DefaultLink.builder()
                                                    .providerId(provider.id())
                                                    .src(x.src())
                                                    .dst(x.dst())
                                                    .type(x.type())
                                                    .isExpected(x.isExpected())
                                                    .annotations(x.annotations()).build()));
            return ImmutableSet.copyOf(links);
        }
    }

    private class MockDevice extends DefaultDevice {

        public MockDevice(ProviderId providerId, DeviceId id, Type type,
                          String manufacturer, String hwVersion, String swVersion,
                          String serialNumber, ChassisId chassisId, Annotations... annotations) {
            super(providerId, id, type, manufacturer, hwVersion, swVersion, serialNumber,
                  chassisId, annotations);
        }

        @Override
        protected Driver locateDriver() {
            return driver;
        }

        @Override
        public Driver driver() {
            return driver;
        }
    }

    private class MockDriver extends DriverAdapter {
        @Override
        public <T extends Behaviour> T createBehaviour(DriverHandler handler, Class<T> behaviourClass) {

            return (T) testLink;
        }
    }

    private class TestLink extends AbstractHandlerBehaviour implements LinkDiscovery {
        Set<LinkDescription> linkDescriptions = new HashSet<>();

        @Override
        public Set<LinkDescription> getLinks() {
            return ImmutableSet.copyOf(linkDescriptions);
        }

        private void addLinkDesc(LinkDescription link) {
            linkDescriptions.add(link);
        }

        private void clearLinkDesc() {
            linkDescriptions.clear();
        }

    }
}