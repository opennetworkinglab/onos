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
import org.onosproject.openstackvtap.api.OpenstackVtap;
import org.onosproject.store.service.TestStorageService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for openstack vtap distributed store.
 */
public class OpenstackVtapStoreTest extends OpenstackVtapTest {

    private DistributedOpenstackVtapStore store;

    @Before
    public void setup() {
        store = new DistributedOpenstackVtapStore();
        TestUtils.setField(store, "storageService", new TestStorageService());
        store.activate();
    }

    @After
    public void tearDown() {
        store.deactivate();
        store = null;
    }

    /**
     * Create, update and remove a openstack vtap network to the store;
     * checks if openstack vtap network store is correct.
     */
    @Test
    public void testCreateUpdateAndRemoveVtapNetwork() {
        store.createVtapNetwork(VTAP_NETWORK_KEY, VTAP_NETWORK_1);
        assertEquals(ERR_NOT_MATCH, store.getVtapNetwork(VTAP_NETWORK_KEY), VTAP_NETWORK_1);

        store.updateVtapNetwork(VTAP_NETWORK_KEY, VTAP_NETWORK_2);
        assertEquals(ERR_NOT_MATCH, store.getVtapNetwork(VTAP_NETWORK_KEY), VTAP_NETWORK_2);

        store.removeVtapNetwork(VTAP_NETWORK_KEY);
        assertNull(store.getVtapNetwork(VTAP_NETWORK_KEY));
    }

    /**
     * Clear all openstack vtap network of the store; checks if openstack vtap network store is empty.
     */
    @Test
    public void testClearVtapNetworks() {
        store.clearVtapNetworks();
        assertEquals(ERR_SIZE, 0, store.getVtapNetworkCount());
    }

    /**
     * Checks if getting device ids by openstack vtap network returns correct.
     */
    @Test
    public void testGetDeviceIdsFromVtapNetwork() {
        store.createVtapNetwork(VTAP_NETWORK_KEY, VTAP_NETWORK_1);

        store.addDeviceToVtapNetwork(VTAP_NETWORK_KEY, DEVICE_ID_1);
        store.addDeviceToVtapNetwork(VTAP_NETWORK_KEY, DEVICE_ID_2);
        assertTrue(ERR_NOT_FOUND, store.getVtapNetworkDevices(VTAP_NETWORK_KEY).contains(DEVICE_ID_1));
        assertTrue(ERR_NOT_FOUND, store.getVtapNetworkDevices(VTAP_NETWORK_KEY).contains(DEVICE_ID_2));

        store.removeDeviceFromVtapNetwork(VTAP_NETWORK_KEY, DEVICE_ID_1);
        store.removeDeviceFromVtapNetwork(VTAP_NETWORK_KEY, DEVICE_ID_2);
        assertTrue(ERR_NOT_FOUND, !store.getVtapNetworkDevices(VTAP_NETWORK_KEY).contains(DEVICE_ID_1));
        assertTrue(ERR_NOT_FOUND, !store.getVtapNetworkDevices(VTAP_NETWORK_KEY).contains(DEVICE_ID_2));

        store.removeVtapNetwork(VTAP_NETWORK_KEY);
    }

    /**
     * Create, update and remove a openstack vtap to the store;
     * checks if openstack vtap store is correct.
     */
    @Test
    public void testCreateUpdateAndRemoveVtap() {
        store.createVtap(VTAP_1);
        assertTrue(ERR_NOT_FOUND, store.getVtaps(OpenstackVtap.Type.VTAP_ANY).contains(VTAP_1));
        assertEquals(ERR_NOT_MATCH, store.getVtap(VTAP_ID_1), VTAP_1);

        OpenstackVtap updated = DefaultOpenstackVtap.builder(VTAP_1)
                .vtapCriterion(VTAP_CRITERION_2)
                .build();
        store.updateVtap(updated, true);
        assertEquals(ERR_NOT_MATCH, updated, store.getVtap(VTAP_ID_1));

        store.removeVtap(VTAP_ID_1);
        assertNull(store.getVtap(VTAP_ID_1));
    }

    /**
     * Checks if getting all by getVtaps method returns correct set of openstack vtaps.
     */
    @Test
    public void testClearVtaps() {
        store.clearVtaps();
        assertEquals(ERR_SIZE, 0, store.getVtapCount(OpenstackVtap.Type.VTAP_ANY));
        assertEquals(ERR_SIZE, 0, store.getVtaps(OpenstackVtap.Type.VTAP_ANY).size());
    }

    /**
     * Checks if getting device ids by openstack vtap returns correct.
     */
    @Test
    public void testGetDeviceIdsFromVtap() {
        store.createVtap(VTAP_2);

        store.addDeviceToVtap(VTAP_ID_2, OpenstackVtap.Type.VTAP_TX, DEVICE_ID_3);
        store.addDeviceToVtap(VTAP_ID_2, OpenstackVtap.Type.VTAP_RX, DEVICE_ID_4);
        assertTrue(ERR_NOT_FOUND, store.getVtapsByDeviceId(DEVICE_ID_3).contains(VTAP_2));
        assertTrue(ERR_NOT_FOUND, store.getVtapsByDeviceId(DEVICE_ID_4).contains(VTAP_2));

        store.removeDeviceFromVtap(VTAP_ID_2, OpenstackVtap.Type.VTAP_TX, DEVICE_ID_3);
        store.removeDeviceFromVtap(VTAP_ID_2, OpenstackVtap.Type.VTAP_RX, DEVICE_ID_4);
        assertTrue(ERR_NOT_FOUND, !store.getVtapsByDeviceId(DEVICE_ID_3).contains(VTAP_2));
        assertTrue(ERR_NOT_FOUND, !store.getVtapsByDeviceId(DEVICE_ID_4).contains(VTAP_2));

        store.removeVtap(VTAP_ID_2);
    }

}
