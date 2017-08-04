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

package org.onosproject.vpls.store;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.store.service.TestStorageService;
import org.onosproject.vpls.VplsTest;
import org.onosproject.vpls.api.VplsData;
import org.onosproject.vpls.config.VplsAppConfig;
import org.onosproject.vpls.config.VplsConfig;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.EncapsulationType.VLAN;

public class VplsStoreTest extends VplsTest {
    private DistributedVplsStore vplsStore;

    @Before
    public void setup() {
        vplsStore = new DistributedVplsStore();
        vplsStore.coreService = new TestCoreService();
        vplsStore.storageService = new TestStorageService();
        vplsStore.networkConfigService = new TestConfigService();
        vplsStore.active();
    }

    @After
    public void tearDown() {
        vplsStore.deactive();
    }

    /**
     * Adds a VPLS to the store; checks if config store is also updated.
     */
    @Test
    public void testAddVpls() {
        VplsData vplsData = VplsData.of(VPLS1, VLAN);
        vplsStore.addVpls(vplsData);
        Collection<VplsData> vplsDataCollection = vplsStore.getAllVpls();

        assertEquals(1, vplsDataCollection.size());
        assertTrue(vplsDataCollection.contains(vplsData));

        VplsAppConfig storedConfig = vplsStore.networkConfigService
                .getConfig(null, VplsAppConfig.class);
        assertNotEquals(-1L, storedConfig.updateTime());

        assertEquals(1, storedConfig.vplss().size());

        VplsConfig vplsConfig = storedConfig.vplss().iterator().next();

        assertEquals(VPLS1, vplsConfig.name());
        assertEquals(0, vplsConfig.ifaces().size());
        assertEquals(VLAN, vplsConfig.encap());
    }

    /**
     * Removes a VPLS from store; checks if config store is also updated.
     */
    @Test
    public void testRemoveVpls() {
        VplsData vplsData = VplsData.of(VPLS1, VLAN);
        vplsStore.addVpls(vplsData);
        vplsStore.removeVpls(vplsData);

        Collection<VplsData> vplsDataCollection = vplsStore.getAllVpls();
        assertEquals(0, vplsDataCollection.size());

        VplsAppConfig storedConfig = vplsStore.networkConfigService
                .getConfig(null, VplsAppConfig.class);

        assertNotEquals(-1L, storedConfig.updateTime());

        assertEquals(0, storedConfig.vplss().size());
    }

    /**
     * Updates a VPLS from store; checks if config store is also updated.
     */
    @Test
    public void testUpdateVpls() {
        VplsData vplsData = VplsData.of(VPLS1, VLAN);
        vplsStore.addVpls(vplsData);
        vplsData.addInterface(V100H1);
        vplsData.addInterface(V100H2);
        vplsStore.updateVpls(vplsData);

        Collection<VplsData> vplsDataCollection = vplsStore.getAllVpls();
        assertEquals(1, vplsDataCollection.size());
        VplsData newVplsData = vplsDataCollection.iterator().next();

        assertEquals(vplsData, newVplsData);

        VplsAppConfig storedConfig = vplsStore.networkConfigService
                .getConfig(null, VplsAppConfig.class);

        assertNotEquals(-1L, storedConfig.updateTime());

        assertEquals(1, storedConfig.vplss().size());

        VplsConfig vplsConfig = storedConfig.vplss().iterator().next();

        assertEquals(VPLS1, vplsConfig.name());
        assertEquals(2, vplsConfig.ifaces().size());
        assertTrue(vplsConfig.ifaces().contains(V100H1.name()));
        assertTrue(vplsConfig.ifaces().contains(V100H2.name()));
        assertEquals(VLAN, vplsConfig.encap());
    }
}
