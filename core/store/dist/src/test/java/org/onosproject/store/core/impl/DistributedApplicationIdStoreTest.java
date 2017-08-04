/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.store.core.impl;

import org.junit.Test;
import org.junit.After;
import org.junit.Before;

import org.onosproject.app.ApplicationIdStore;
import org.onosproject.core.ApplicationId;
import org.onosproject.store.service.TestStorageService;

import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Test Class for DistributedApplicationIdStore.
 */
public class DistributedApplicationIdStoreTest {
    private DistributedApplicationIdStore appIdStore;
    private ApplicationIdStore idStore;

    private final String app1 = "appID1";
    private final String app2 = "appID2";

    @Before
    public void setUp() throws Exception {
        appIdStore = new DistributedApplicationIdStore();
        appIdStore.storageService = new TestStorageService();
        appIdStore.activate();
        idStore = appIdStore;
    }

    @After
    public void tearDown() throws Exception {
        appIdStore.deactivate();
    }

    @Test(expected = NullPointerException.class)
    public void testEmpty() {
        idStore = new DistributedApplicationIdStore();
        idStore.getAppIds();
    }

    @Test
    public void testIdStore() {
        Collection<ApplicationId> appIds = idStore.getAppIds();
        assertTrue("App ID's should be empty", appIds.isEmpty());

        idStore.registerApplication(app1);
        appIds = idStore.getAppIds();
        assertEquals("There should be one app ID", appIds.size(), 1);
        assertEquals(idStore.getAppId((short) 1).id(), 1);
        assertEquals(idStore.getAppId(app1).name(), "appID1");
        assertEquals(idStore.getAppId(app2), null);

        //Register same appid
        idStore.registerApplication("appID1");
        assertEquals(appIds.size(), 1);

        idStore.registerApplication(app2);
        assertEquals(idStore.getAppIds().size(), 2);
        assertEquals(idStore.getAppId((short) 2).name(), app2);
    }
}