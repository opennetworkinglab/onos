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
package org.onosproject.store.flowobjective.impl;

import org.hamcrest.collection.IsMapContaining;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.behaviour.DefaultNextGroup;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.flowobjective.FlowObjectiveStore;
import org.onosproject.store.service.TestStorageService;

import com.google.common.base.Charsets;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for distributed flow objective store.
 */
public class DistributedFlowObjectiveStoreTest {
    DistributedFlowObjectiveStore storeImpl;
    FlowObjectiveStore store;

    @Before
    public void setUp() {
        storeImpl = new DistributedFlowObjectiveStore();
        storeImpl.storageService = new TestStorageService();
        storeImpl.activate();
        store = storeImpl;
    }

    @After
    public void tearDown() {
        storeImpl.deactivate();
    }

    @Test
    public void testFlowObjectiveStore() {
        NextGroup group1 = new DefaultNextGroup("1".getBytes(Charsets.US_ASCII));
        NextGroup group2 = new DefaultNextGroup("2".getBytes(Charsets.US_ASCII));
        NextGroup group3 = new DefaultNextGroup("3".getBytes(Charsets.US_ASCII));
        int group1Id = store.allocateNextId();
        int group2Id = store.allocateNextId();
        int group3Id = store.allocateNextId();

        NextGroup group1add = store.getNextGroup(group1Id);
        assertThat(group1add, nullValue());
        NextGroup dif = store.getNextGroup(3);
        assertThat(dif, is(nullValue()));


        store.putNextGroup(group1Id, group1);
        store.putNextGroup(group2Id, group2);
        NextGroup group2Query = store.getNextGroup(group2Id);
        assertThat(group2Query.data(), is(group2.data()));
        assertTrue(store.getAllGroups().containsKey(group2Id));
        assertTrue(store.getAllGroups().containsKey(group1Id));

        store.removeNextGroup(group2Id);
        assertTrue(store.getAllGroups().containsKey(group1Id));
        assertFalse(store.getAllGroups().containsKey(group2Id));
        store.removeNextGroup(group1Id);
        assertEquals(store.getAllGroups(), Collections.emptyMap());


        store.putNextGroup(group3Id, group3);
        store.removeNextGroup(group2Id);
        NextGroup nullGroup = store.getNextGroup(group2Id);
        assertThat(nullGroup, nullValue());

        assertThat(store.getAllGroups().size(), is(1));
        assertThat(store.getAllGroups(), IsMapContaining.hasKey(group3Id));
    }
}