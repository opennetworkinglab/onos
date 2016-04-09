/*
 * Copyright 2015-present Open Networking Laboratory
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.behaviour.DefaultNextGroup;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.flowobjective.FlowObjectiveStore;
import org.onosproject.store.service.TestStorageService;

import com.google.common.base.Charsets;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

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
        NextGroup group2 = new DefaultNextGroup("2".getBytes(Charsets.US_ASCII));
        int group1Id = store.allocateNextId();
        int group2Id = store.allocateNextId();

        NextGroup group1add = store.getNextGroup(group1Id);
        assertThat(group1add, nullValue());

        store.putNextGroup(group2Id, group2);
        NextGroup group2Query = store.getNextGroup(group2Id);
        assertThat(group2Query.data(), is(group2.data()));
    }
}
