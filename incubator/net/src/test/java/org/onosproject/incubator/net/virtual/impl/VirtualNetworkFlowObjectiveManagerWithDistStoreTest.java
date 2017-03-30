/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.incubator.net.virtual.impl;

import org.junit.After;
import org.junit.Before;
import org.onlab.junit.TestUtils;
import org.onosproject.incubator.store.virtual.impl.DistributedVirtualFlowObjectiveStore;

/**
 * Junit tests for VirtualNetworkFlowObjectiveManager using
 * DistributedVirtualFlowObjectiveStore.  This test class extends
 * VirtualNetworkFlowObjectiveManagerTest - all the tests defined in
 * VirtualNetworkFlowObjectiveManagerTest will run using
 * DistributedVirtualFlowObjectiveStore.
 */
public class VirtualNetworkFlowObjectiveManagerWithDistStoreTest
        extends VirtualNetworkFlowObjectiveManagerTest {

    private static final String STORE_FIELDNAME_STORAGESERVICE = "storageService";

    private DistributedVirtualFlowObjectiveStore distStore;

    @Before
    public void setUp() throws Exception {
        setupDistFlowObjectiveStore();
        super.setUp();
    }

    private void setupDistFlowObjectiveStore() throws TestUtils.TestUtilsException {
        distStore = new DistributedVirtualFlowObjectiveStore();
        TestUtils.setField(distStore, STORE_FIELDNAME_STORAGESERVICE, storageService);

        distStore.activate();
        flowObjectiveStore = distStore; // super.setUp() will cause Distributed store to be used.
    }

    @After
    public void tearDown() {
        distStore.deactivate();
    }
}
