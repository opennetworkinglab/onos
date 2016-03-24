package org.onosproject.store.cluster.impl;

import org.junit.After;
import org.junit.Before;

import static org.junit.Assert.*;

/**
 * Unit test for DistributedClusterStore.
 */
public class DistributedClusterStoreTest {
    DistributedClusterStore distributedClusterStore;

    @Before
    public void setUp() throws Exception {
        distributedClusterStore = new DistributedClusterStore();
        distributedClusterStore.activate();
    }

    @After
    public void tearDown() throws Exception {
        distributedClusterStore.deactivate();
    }
}