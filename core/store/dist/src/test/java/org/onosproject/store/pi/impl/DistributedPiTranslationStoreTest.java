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

package org.onosproject.store.pi.impl;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.runtime.PiEntity;
import org.onosproject.net.pi.runtime.PiEntityType;
import org.onosproject.net.pi.runtime.PiHandle;
import org.onosproject.net.pi.service.PiTranslatable;
import org.onosproject.net.pi.service.PiTranslatedEntity;
import org.onosproject.store.service.TestStorageService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for {@link AbstractDistributedPiTranslationStore}.
 */
public class DistributedPiTranslationStoreTest {

    private AbstractDistributedPiTranslationStore<PiTranslatable, PiEntity> store;

    private static final int HANDLE_HASH = RandomUtils.nextInt();
    private static final PiTranslatable PI_TRANSLATABLE =
            new PiTranslatable() {
            };
    private static final PiHandle PI_HANDLE =
            new PiHandle(DeviceId.NONE) {
                @Override
                public PiEntityType entityType() {
                    return PI_ENTITY.piEntityType();
                }

                @Override
                public int hashCode() {
                    return HANDLE_HASH;
                }

                @Override
                public boolean equals(Object other) {
                    return other instanceof PiHandle && other.hashCode() == hashCode();
                }

                @Override
                public String toString() {
                    return String.valueOf(HANDLE_HASH);
                }
            };
    private static final PiEntity PI_ENTITY = new PiEntity() {
        @Override
        public PiEntityType piEntityType() {
            return PiEntityType.TABLE_ENTRY;
        }

        @Override
        public PiHandle handle(DeviceId deviceId) {
            return PI_HANDLE;
        }
    };
    private static final PiTranslatedEntity<PiTranslatable, PiEntity> TRANSLATED_ENTITY =
            new PiTranslatedEntity<>(PI_TRANSLATABLE, PI_ENTITY, PI_HANDLE);

    /**
     * Sets up the store and the storage service test harness.
     */
    @Before
    public void setUp() {
        store = new AbstractDistributedPiTranslationStore<PiTranslatable, PiEntity>() {
            @Override
            protected String mapSimpleName() {
                return "test";
            }
        };
        store.storageService = new TestStorageService();
        store.setDelegate(event -> {
        });
        store.activate();
    }

    /**
     * Tests equality of key and value used in other tests.
     */
    @Test
    public void testEquality() {
        assertEquals(PI_HANDLE, PI_HANDLE);
        assertEquals(TRANSLATED_ENTITY, TRANSLATED_ENTITY);
    }

    /**
     * Test for activate.
     */
    @Test
    public void activate() {
        assertNotNull(store.storageService);
        assertTrue("Store must have delegate",
                   store.hasDelegate());
        assertTrue("No value should be in the map",
                   Lists.newArrayList(store.getAll()).isEmpty());
    }

    /**
     * Test for deactivate.
     */
    @Test(expected = NullPointerException.class)
    public void deactivate() {
        store.deactivate();
        store.getAll();
    }

    /**
     * Test of value add or update.
     */
    @Test
    public void addOrUpdate() {
        store.addOrUpdate(PI_HANDLE, TRANSLATED_ENTITY);
        assertTrue("Value should be in the map",
                   store.get(PI_HANDLE) != null);
        assertTrue("Exactly 1 value should be in the map",
                   Lists.newArrayList(store.getAll()).size() == 1);

        // Add again, expect 1 value.
        store.addOrUpdate(PI_HANDLE, TRANSLATED_ENTITY);
        assertTrue("Exactly 1 value should be in the map",
                   Lists.newArrayList(store.getAll()).size() == 1);
    }

    /**
     * Test of value lookup.
     */
    @Test
    public void lookup() throws Exception {
        clear();
        addOrUpdate();
        assertEquals("Wrong value in the map",
                     store.get(PI_HANDLE), TRANSLATED_ENTITY);
    }

    /**
     * Test of value removal.
     */
    @Test
    public void clear() {
        store.remove(PI_HANDLE);
        assertTrue("Value should NOT be in the map",
                   store.get(PI_HANDLE) == null);
        assertTrue("No value should be in the map",
                   Lists.newArrayList(store.getAll()).isEmpty());
    }
}
