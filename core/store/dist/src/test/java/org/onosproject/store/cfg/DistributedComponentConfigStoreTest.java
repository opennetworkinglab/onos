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

package org.onosproject.store.cfg;

import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.cfg.ComponentConfigEvent;
import org.onosproject.store.service.TestStorageService;

import static org.junit.Assert.*;

public class DistributedComponentConfigStoreTest {

    private static final String C1 = "c1";
    private static final String C2 = "c2";

    private TestStore store;
    private ComponentConfigEvent event;

    /**
     * Sets up the device key store and the storage service test harness.
     */
    @Before
    public void setUp() {
        store = new TestStore();
        store.storageService = new TestStorageService();
        store.setDelegate(e -> this.event = e);
        store.activate();
    }

    /**
     * Tears down the device key store.
     */
    @After
    public void tearDown() {
        store.deactivate();
    }

    @Test
    public void basics() {
        assertNull("property should not be found", store.getProperty(C1, "bar"));
        store.setProperty(C1, "foo", "yo");
        store.setProperty(C1, "bar", "true");
        store.setProperty(C2, "goo", "6.28");
        assertEquals("incorrect event", ComponentConfigEvent.Type.PROPERTY_SET, event.type());
        assertEquals("incorrect event key", "goo", event.name());
        assertEquals("incorrect event value", "6.28", event.value());

        assertEquals("incorrect property value", "true", store.getProperty(C1, "bar"));
        assertEquals("incorrect property count", ImmutableSet.of("foo", "bar"),
                     store.getProperties(C1));

        store.unsetProperty(C1, "bar");
        assertEquals("incorrect event", ComponentConfigEvent.Type.PROPERTY_UNSET, event.type());
        assertEquals("incorrect event key", "bar", event.name());
        assertNull("incorrect event value", event.value());

        assertNull("property should not be found", store.getProperty(C1, "bar"));
        assertEquals("incorrect property count", ImmutableSet.of("foo"),
                     store.getProperties(C1));
    }

    class TestStore extends DistributedComponentConfigStore {
    }
}