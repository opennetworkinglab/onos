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
package org.onosproject.openstacknetworking.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.openstacknetworking.api.InstancePortAdminService;
import org.onosproject.store.service.TestStorageService;

import static org.junit.Assert.assertEquals;
import static org.onosproject.openstacknetworking.api.OpenstackNetworkEvent.Type.OPENSTACK_PORT_PRE_REMOVE;
import static org.onosproject.openstacknetworking.api.OpenstackNetworkEvent.Type.OPENSTACK_PORT_PRE_UPDATE;

/**
 * Unit tests for pre-commit port manager.
 */
public class PreCommitPortManagerTest {

    private static final ApplicationId TEST_APP_ID = new DefaultApplicationId(1, "test");

    private static final String PORT_ID_1 = "port-1";
    private static final String PORT_ID_2 = "port-2";

    private static final String CLASS_NAME_1 = "class-1";
    private static final String CLASS_NAME_2 = "class-2";

    private PreCommitPortManager target;

    /**
     * Initializes this unit test.
     */
    @Before
    public void setUp() {
        target = new PreCommitPortManager();
        TestUtils.setField(target, "coreService", new TestCoreService());
        TestUtils.setField(target, "storageService", new TestStorageService());
        target.activate();
    }

    /**
     * Tears down this unit test.
     */
    @After
    public void tearDown() {
        target.deactivate();
        target = null;
    }

    /**
     * Tests subscribe pre-commit method.
     */
    @Test
    public void testSubscribePreCommit() {

        sampleSubscribe();

        assertEquals(1, target.subscriberCountByEventType(PORT_ID_1, OPENSTACK_PORT_PRE_REMOVE));
        assertEquals(2, target.subscriberCountByEventType(PORT_ID_2, OPENSTACK_PORT_PRE_REMOVE));

        assertEquals(0, target.subscriberCountByEventType(PORT_ID_1, OPENSTACK_PORT_PRE_UPDATE));
        assertEquals(1, target.subscriberCountByEventType(PORT_ID_2, OPENSTACK_PORT_PRE_UPDATE));

        assertEquals(1, target.subscriberCount(PORT_ID_1));
        assertEquals(3, target.subscriberCount(PORT_ID_2));
    }

    /**
     * Tests unsubscribe pre-commit method.
     */
    @Test
    public void testUnsubscribePreCommit() {

        sampleSubscribe();

        InstancePortAdminService service = new TestInstancePortAdminService();

        target.unsubscribePreCommit(PORT_ID_1, OPENSTACK_PORT_PRE_REMOVE, service, CLASS_NAME_1);
        target.unsubscribePreCommit(PORT_ID_2, OPENSTACK_PORT_PRE_REMOVE, service, CLASS_NAME_2);

        assertEquals(0, target.subscriberCountByEventType(PORT_ID_1, OPENSTACK_PORT_PRE_REMOVE));
        assertEquals(1, target.subscriberCountByEventType(PORT_ID_2, OPENSTACK_PORT_PRE_REMOVE));

        assertEquals(0, target.subscriberCount(PORT_ID_1));
        assertEquals(2, target.subscriberCount(PORT_ID_2));
    }

    private void sampleSubscribe() {

        target.subscribePreCommit(PORT_ID_1, OPENSTACK_PORT_PRE_REMOVE, CLASS_NAME_1);

        target.subscribePreCommit(PORT_ID_2, OPENSTACK_PORT_PRE_REMOVE, CLASS_NAME_1);
        target.subscribePreCommit(PORT_ID_2, OPENSTACK_PORT_PRE_REMOVE, CLASS_NAME_2);

        target.subscribePreCommit(PORT_ID_2, OPENSTACK_PORT_PRE_UPDATE, CLASS_NAME_1);
    }

    /**
     * Mocks CoreService.
     */
    private static class TestCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return TEST_APP_ID;
        }
    }

    private static class TestInstancePortAdminService extends InstancePortAdminServiceAdapter {
    }
}
