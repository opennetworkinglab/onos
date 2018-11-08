/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstackvtap.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onosproject.event.Event;
import org.onosproject.openstackvtap.api.OpenstackVtap;
import org.onosproject.store.service.TestStorageService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.NetTestTools.injectEventDispatcher;
import static org.onosproject.openstackvtap.api.OpenstackVtapEvent.Type.VTAP_ADDED;
import static org.onosproject.openstackvtap.api.OpenstackVtapEvent.Type.VTAP_NETWORK_ADDED;
import static org.onosproject.openstackvtap.api.OpenstackVtapEvent.Type.VTAP_NETWORK_REMOVED;
import static org.onosproject.openstackvtap.api.OpenstackVtapEvent.Type.VTAP_REMOVED;
import static org.onosproject.openstackvtap.api.OpenstackVtapEvent.Type.VTAP_UPDATED;

/**
 * Unit tests for openstack vtap manager.
 */
public class OpenstackVtapManagerTest extends OpenstackVtapTest {

    private OpenstackVtapManager target;
    private DistributedOpenstackVtapStore store;

    private final TestOpenstackVtapListener testListener = new TestOpenstackVtapListener();

    private void validateEvents(Enum... types) {
        int i = 0;
        assertEquals("Number of events did not match", types.length, testListener.events.size());
        for (Event event : testListener.events) {
            assertEquals("Incorrect event received", types[i], event.type());
            i++;
        }
        testListener.events.clear();
    }

    @Before
    public void setUp() {
        store = new DistributedOpenstackVtapStore();
        TestUtils.setField(store, "storageService", new TestStorageService());
        store.activate();

        store.createVtap(VTAP_2);

        target = new OpenstackVtapManager();
        target.coreService = new TestCoreService();
        target.clusterService = new TestClusterService();
        target.leadershipService = new TestLeadershipService();
        target.flowRuleService = new TestFlowRuleService();
        target.groupService = new TestGroupService();
        target.deviceService = new TestDeviceService();
        target.osNodeService = new TestOpenstackNodeService();
        target.hostService = new TestHostService();
        target.componentConfigService = new TestComponentConfigService();

        target.store = store;
        injectEventDispatcher(target, new TestEventDispatcher());
        target.addListener(testListener);
        testListener.events.clear();

        target.activate();
    }

    @After
    public void tearDown() {
        target.clearVtap();
        target.purgeVtap();

        target.deactivate();
        target.removeListener(testListener);
        target = null;

        store.deactivate();
        store = null;
    }

    /**
     * Checks if creating and removing a openstack vtap network work well with proper events.
     */
    @Test
    public void testCreateAndRemoveVtapNetwork() {
        target.createVtapNetwork(VTAP_NETWORK_MODE_1, VTAP_NETWORK_NETWORK_ID_1, SERVER_IP_1);
        assertNotNull(target.getVtapNetwork());

        target.removeVtapNetwork();
        assertNull(target.getVtapNetwork());

        validateEvents(VTAP_NETWORK_ADDED, VTAP_NETWORK_REMOVED);
    }

    /**
     * Checks if creating null openstack vtap network fails with proper exception.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateNullVtapNetwork() {
        target.createVtapNetwork(null, null, null);
    }

    /**
     * Checks if updating a null openstack vtap network fails with proper exception.
     */
    @Test(expected = NullPointerException.class)
    public void testUpdateNullVtapNetwork() {
        target.updateVtapNetwork(null);
    }

    /**
     * Checks if getting device ids by openstack vtap network returns correct.
     */
    @Test
    public void testGetDeviceIdsFromVtapNetwork() {
        store.createVtapNetwork(VTAP_NETWORK_KEY, VTAP_NETWORK_1);

        store.addDeviceToVtapNetwork(VTAP_NETWORK_KEY, DEVICE_ID_1);
        store.addDeviceToVtapNetwork(VTAP_NETWORK_KEY, DEVICE_ID_2);
        assertTrue(ERR_NOT_FOUND, target.getVtapNetworkDevices().contains(DEVICE_ID_1));
        assertTrue(ERR_NOT_FOUND, target.getVtapNetworkDevices().contains(DEVICE_ID_2));

        store.removeDeviceFromVtapNetwork(VTAP_NETWORK_KEY, DEVICE_ID_1);
        store.removeDeviceFromVtapNetwork(VTAP_NETWORK_KEY, DEVICE_ID_2);
        assertTrue(ERR_NOT_FOUND, !target.getVtapNetworkDevices().contains(DEVICE_ID_1));
        assertTrue(ERR_NOT_FOUND, !target.getVtapNetworkDevices().contains(DEVICE_ID_2));

        store.removeVtapNetwork(VTAP_NETWORK_KEY);
    }

    /**
     * Checks if creating and removing a openstack vtap work well with proper events.
     */
    @Test
    public void testCreateAndRemoveVtap() {
        OpenstackVtap vtap = target.createVtap(VTAP_TYPE_1, VTAP_CRITERION_1);
        assertEquals(ERR_SIZE, 2, target.getVtapCount(OpenstackVtap.Type.VTAP_ANY));
        assertNotNull(target.getVtap(vtap.id()));

        target.removeVtap(vtap.id());
        assertEquals(ERR_SIZE, 1, target.getVtapCount(OpenstackVtap.Type.VTAP_ANY));
        assertNull(target.getVtap(vtap.id()));

        validateEvents(VTAP_ADDED, VTAP_REMOVED);
    }

    /**
     * Checks if updating a openstack vtap works well with proper event.
     */
    @Test
    public void testUpdateVtap() {
        OpenstackVtap updated = DefaultOpenstackVtap.builder(VTAP_2)
                .vtapCriterion(VTAP_CRITERION_1)
                .build();
        target.updateVtap(updated);
        assertEquals(ERR_NOT_MATCH, updated.vtapCriterion(), target.getVtap(VTAP_ID_2).vtapCriterion());

        updated = DefaultOpenstackVtap.builder(VTAP_2)
                .build();
        target.updateVtap(updated);

        validateEvents(VTAP_UPDATED, VTAP_UPDATED);
    }

    /**
     * Checks if updating not existing openstack vtap fails with proper exception.
     */
    @Test
    public void testUpdateNotExistingVtap() {
        assertNull(target.updateVtap(VTAP_1));
    }

    /**
     * Checks if creating null openstack vtap fails with proper exception.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateNullVtap() {
        target.createVtap(null, null);
    }

    /**
     * Checks if updating a null openstack vtap fails with proper exception.
     */
    @Test(expected = NullPointerException.class)
    public void testUpdateNullVtap() {
        target.updateVtap(null);
    }

    /**
     * Checks if removing null openstack vtap fails with proper exception.
     */
    @Test(expected = NullPointerException.class)
    public void testRemoveNullVtap() {
        target.removeVtap(null);
    }

    /**
     * Checks if getting all by getVtaps method returns correct set of openstack vtaps.
     */
    @Test
    public void testGetAllVtaps() {
        assertEquals(ERR_SIZE, 1, target.getVtapCount(OpenstackVtap.Type.VTAP_ANY));
        assertTrue(ERR_NOT_FOUND, target.getVtaps(OpenstackVtap.Type.VTAP_ANY).contains(VTAP_2));
    }

    /**
     * Checks if getting device ids by openstack vtap returns correct.
     */
    @Test
    public void testGetDeviceIdsFromVtap() {
        assertTrue(ERR_NOT_FOUND, target.getVtapsByDeviceId(DEVICE_ID_3).contains(VTAP_2));
        assertTrue(ERR_NOT_FOUND, target.getVtapsByDeviceId(DEVICE_ID_4).contains(VTAP_2));
    }

}
