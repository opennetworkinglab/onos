/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.mapping.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.mapping.DefaultMapping;
import org.onosproject.mapping.DefaultMappingEntry;
import org.onosproject.mapping.Mapping;
import org.onosproject.mapping.MappingAdminService;
import org.onosproject.mapping.MappingEntry;
import org.onosproject.mapping.MappingEvent;
import org.onosproject.mapping.MappingKey;
import org.onosproject.mapping.MappingListener;
import org.onosproject.mapping.MappingProvider;
import org.onosproject.mapping.MappingProviderRegistry;
import org.onosproject.mapping.MappingProviderService;
import org.onosproject.mapping.MappingService;
import org.onosproject.mapping.MappingStore.Type;
import org.onosproject.mapping.MappingTreatment;
import org.onosproject.mapping.MappingValue;
import org.onosproject.mapping.actions.MappingAction;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.onosproject.mapping.MappingStore.Type.MAP_DATABASE;
import static org.onosproject.net.NetTestTools.injectEventDispatcher;

/**
 * Unit tests for mapping manager.
 */
public class MappingManagerTest {

    private static final ProviderId LISP_PID = new ProviderId("lisp", "lisp");
    private static final ProviderId BAR_PID = new ProviderId("bar", "bar");

    private static final DeviceId LISP_DID = DeviceId.deviceId("lisp:001");
    private static final DeviceId BAR_DID = DeviceId.deviceId("bar:002");

    private static final DefaultAnnotations ANNOTATIONS =
            DefaultAnnotations.builder().set(AnnotationKeys.DRIVER, "bar").build();

    private static final Device LISP_DEV =
            new DefaultDevice(LISP_PID, LISP_DID, Device.Type.SWITCH, "", "", "", "", null);
    private static final Device BAR_DEV =
            new DefaultDevice(BAR_PID, BAR_DID, Device.Type.SWITCH, "", "", "", "", null, ANNOTATIONS);


    private MappingManager manager;

    private MappingService service;
    private MappingAdminService adminService;
    private MappingProviderRegistry registry;
    private MappingProviderService providerService;
    private TestProvider provider;
    private TestListener listener = new TestListener();
    private ApplicationId appId;

    @Before
    public void setUp() {
        manager = new MappingManager();
        manager.store = new SimpleMappingStore();
        injectEventDispatcher(manager, new TestEventDispatcher());
        manager.deviceService = new TestDeviceService();

        service = manager;
        adminService = manager;
        registry = manager;

        manager.activate();
        manager.addListener(listener);
        provider = new TestProvider(LISP_PID);
        providerService = registry.register(provider);
        appId = new TestApplicationId(0, "MappingManagerTest");
        assertTrue("provider should be registered",
                registry.getProviders().contains(provider.id()));
    }

    @After
    public void tearDown() {
        registry.unregister(provider);
        assertFalse("provider should not be registered",
                registry.getProviders().contains(provider.id()));
        service.removeListener(listener);
        manager.deactivate();
        injectEventDispatcher(manager, null);
        manager.deviceService = null;
    }

    /**
     * Creates a mapping from a specified deviceID, key and value.
     *
     * @param did   device identifier
     * @param key   test value for mapping key
     * @param value test value for mapping value
     * @return mapping instance
     */
    private Mapping mapping(DeviceId did, int key, int value) {
        TestMappingKey mappingKey = new TestMappingKey(key);
        TestMappingValue mappingValue = new TestMappingValue(value);

        return DefaultMapping.builder()
                .forDevice(did)
                .withKey(mappingKey)
                .withValue(mappingValue)
                .fromApp(appId)
                .withId(key + value)
                .build();
    }

    /**
     * Creates a mapping from a specified key and value.
     *
     * @param key   test value for mapping key
     * @param value test value for mapping value
     * @return mapping instance
     */
    private Mapping mapping(int key, int value) {
        return mapping(LISP_DID, key, value);
    }

    /**
     * Adds a new mapping into the mapping store.
     *
     * @param type mapping store type
     * @param tval test value
     * @return a mapping that has been added to the store
     */
    private Mapping addMapping(Type type, int tval) {
        Mapping mapping = mapping(tval, tval);
        MappingEntry entry = new DefaultMappingEntry(mapping);
        adminService.storeMappingEntry(type, entry);

        assertNotNull("mapping should be found",
                                service.getMappingEntries(type, LISP_DID));
        return mapping;
    }

    /**
     * Obtains the number of mappings.
     *
     * @param type mapping store type
     * @return number of mappings
     */
    private int mappingCount(Type type) {
        return Sets.newHashSet(service.getMappingEntries(type, LISP_DID)).size();
    }

    /**
     * Tests retrieving mapping entries method.
     */
    @Test
    public void getMappingEntries() {

        assertTrue("Store should be empty", Sets.newHashSet(
                service.getMappingEntries(MAP_DATABASE, LISP_DID)).isEmpty());
        addMapping(MAP_DATABASE, 1);
        addMapping(MAP_DATABASE, 2);
        assertEquals("2 mappings should exist", 2, mappingCount(MAP_DATABASE));

        addMapping(MAP_DATABASE, 1);
        assertEquals("should still be 2 mappings", 2, mappingCount(MAP_DATABASE));
    }

    /**
     * Tests storing mapping entry method.
     */
    @Test
    public void storeMappingEntry() {

        Mapping m1 = mapping(1, 1);
        Mapping m2 = mapping(2, 2);
        Mapping m3 = mapping(3, 3);

        MappingEntry me1 = new DefaultMappingEntry(m1);
        MappingEntry me2 = new DefaultMappingEntry(m2);
        MappingEntry me3 = new DefaultMappingEntry(m3);

        assertTrue("store should be empty", Sets.newHashSet(
                service.getMappingEntries(MAP_DATABASE, LISP_DID)).isEmpty());
        adminService.storeMappingEntry(MAP_DATABASE, me1);
        adminService.storeMappingEntry(MAP_DATABASE, me2);
        adminService.storeMappingEntry(MAP_DATABASE, me3);
        assertEquals("3 mappings should exist", 3, mappingCount(MAP_DATABASE));
    }

    /**
     * Tests removing mapping entries method.
     */
    @Test
    public void removeMappingEntries() {

        Mapping m1 = addMapping(MAP_DATABASE, 1);
        Mapping m2 = addMapping(MAP_DATABASE, 2);
        addMapping(MAP_DATABASE, 3);
        assertEquals("3 mappings should exist", 3, mappingCount(MAP_DATABASE));

        MappingEntry me1 = new DefaultMappingEntry(m1);
        MappingEntry me2 = new DefaultMappingEntry(m2);

        adminService.removeMappingEntries(MAP_DATABASE, me1, me2);
        assertEquals("1 mappings should exist", 1, mappingCount(MAP_DATABASE));
    }

    /**
     * Tests purging all mappings.
     */
    @Test
    public void purgeMappings() {

        addMapping(MAP_DATABASE, 1);
        addMapping(MAP_DATABASE, 2);
        addMapping(MAP_DATABASE, 3);
        assertEquals("3 mappings should exist", 3, mappingCount(MAP_DATABASE));

        adminService.purgeMappings(MAP_DATABASE, LISP_DID);
        assertEquals("0 mappings should exist", 0, mappingCount(MAP_DATABASE));
    }

    /**
     * Tests obtaining mapping entries by application ID.
     */
    @Test
    public void getMappingEntriesByAddId() {
        addMapping(MAP_DATABASE, 1);
        addMapping(MAP_DATABASE, 2);

        assertTrue("should have two mappings",
                Lists.newLinkedList(
                        service.getMappingEntriesByAppId(MAP_DATABASE, appId)).size() == 2);
    }

    /**
     * Tests removing mapping entries by application ID.
     */
    @Test
    public void removeMappingEntriesByAppId() {
        addMapping(MAP_DATABASE, 1);
        addMapping(MAP_DATABASE, 2);

        adminService.removeMappingEntriesByAppId(MAP_DATABASE, appId);

        assertTrue("should not have any mappings",
                Lists.newLinkedList(
                        service.getMappingEntriesByAppId(MAP_DATABASE, appId)).size() == 0);
    }

    private static class TestDeviceService extends DeviceServiceAdapter {
        @Override
        public int getDeviceCount() {
            return 2;
        }

        @Override
        public Iterable<Device> getDevices() {
            return ImmutableList.of(LISP_DEV, BAR_DEV);
        }

        @Override
        public Iterable<Device> getAvailableDevices() {
            return getDevices();
        }

        @Override
        public Device getDevice(DeviceId deviceId) {
            return deviceId.equals(BAR_DID) ? BAR_DEV : LISP_DEV;
        }
    }

    private static class TestListener implements MappingListener {
        final List<MappingEvent> events = new ArrayList<>();

        @Override
        public void event(MappingEvent event) {
            events.add(event);
        }
    }

    private class TestProvider extends AbstractProvider implements MappingProvider {

        /**
         * Creates a provider with the supplied identifier.
         *
         * @param id provider id
         */
        TestProvider(ProviderId id) {
            super(id);
        }
    }

    private class TestApplicationId extends DefaultApplicationId {
        TestApplicationId(int id, String name) {
            super(id, name);
        }
    }

    private class TestMappingKey implements MappingKey {

        private final int val;

        TestMappingKey(int val) {
            this.val = val;
        }

        @Override
        public MappingAddress address() {
            return null;
        }

        @Override
        public int hashCode() {
            return val;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof TestMappingKey && this.val == ((TestMappingKey) o).val;
        }
    }

    private class TestMappingValue implements MappingValue {

        private final int val;

        TestMappingValue(int val) {
            this.val = val;
        }

        @Override
        public MappingAction action() {
            return null;
        }

        @Override
        public List<MappingTreatment> treatments() {
            return null;
        }

        @Override
        public int hashCode() {
            return val;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof TestMappingValue && this.val == ((TestMappingValue) o).val;
        }
    }
}
