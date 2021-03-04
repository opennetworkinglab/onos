/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.impl;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.packet.IpPrefix;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.event.Event;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtSecurityGroup;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtSecurityGroupRule;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroup;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupListener;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupRule;
import org.onosproject.store.service.TestStorageService;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupEvent.Type.KUBEVIRT_SECURITY_GROUP_CREATED;
import static org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupEvent.Type.KUBEVIRT_SECURITY_GROUP_REMOVED;
import static org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupEvent.Type.KUBEVIRT_SECURITY_GROUP_RULE_CREATED;
import static org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupEvent.Type.KUBEVIRT_SECURITY_GROUP_RULE_REMOVED;

/**
 * Unit tests for kubevirt security group manager.
 */
public class KubevirtSecurityGroupManagerTest {

    private static final ApplicationId TEST_APP_ID = new DefaultApplicationId(1, "test");

    private static final String SECURITY_GROUP_ID_1 = "sg-id-1";
    private static final String SECURITY_GROUP_ID_2 = "sg-id-2";
    private static final String UNKNOWN_ID = "sg-id-x";


    private static final String SECURITY_GROUP_NAME_1 = "sg-name-1";
    private static final String SECURITY_GROUP_NAME_2 = "sg-name-2";

    private static final String SECURITY_GROUP_DESCRIPTION_1 = "description-1";
    private static final String SECURITY_GROUP_DESCRIPTION_2 = "description-2";

    private static final String SECURITY_GROUP_RULE_ID_1_1 = "sgr-id-1-1";
    private static final String SECURITY_GROUP_RULE_ID_1_2 = "sgr-id-1-2";

    private static final String SECURITY_GROUP_ETH_TYPE = "IP";
    private static final String SECURITY_GROUP_DIRECTION = "EGRESS";
    private static final String SECURITY_GROUP_PROTOCOL_1 = "TCP";
    private static final String SECURITY_GROUP_PROTOCOL_2 = "UDP";

    private static final int SECURITY_GROUP_PORT_RANGE_MIN_1 = 1;
    private static final int SECURITY_GROUP_PORT_RANGE_MIN_2 = 101;
    private static final int SECURITY_GROUP_PORT_RANGE_MAX_1 = 100;
    private static final int SECURITY_GROUP_PORT_RANGE_MAX_2 = 200;

    private static final IpPrefix SECURITY_GROUP_REMOTE_IP_PREFIX_1 = IpPrefix.valueOf("1.1.1.0/24");
    private static final IpPrefix SECURITY_GROUP_REMOTE_IP_PREFIX_2 = IpPrefix.valueOf("2.2.2.0/24");

    private KubevirtSecurityGroup sg1;
    private KubevirtSecurityGroup sg2;

    private KubevirtSecurityGroupRule sgRule11;
    private KubevirtSecurityGroupRule sgRule12;

    private KubevirtSecurityGroupManager target;
    private DistributedKubevirtSecurityGroupStore store;

    private final TestKubevirtSecurityGroupListener testListener =
            new TestKubevirtSecurityGroupListener();

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() throws Exception {
        store = new DistributedKubevirtSecurityGroupStore();
        TestUtils.setField(store, "coreService", new TestCoreService());
        TestUtils.setField(store, "storageService", new TestStorageService());
        TestUtils.setField(store, "eventExecutor", MoreExecutors.newDirectExecutorService());
        store.activate();

        target = new KubevirtSecurityGroupManager();
        TestUtils.setField(target, "coreService", new TestCoreService());
        target.sgStore = store;
        target.addListener(testListener);
        target.activate();

        sgRule11 = DefaultKubevirtSecurityGroupRule.builder()
                .id(SECURITY_GROUP_RULE_ID_1_1)
                .securityGroupId(SECURITY_GROUP_ID_1)
                .remoteGroupId(SECURITY_GROUP_ID_1)
                .direction(SECURITY_GROUP_DIRECTION)
                .etherType(SECURITY_GROUP_ETH_TYPE)
                .portRangeMax(SECURITY_GROUP_PORT_RANGE_MAX_1)
                .portRangeMin(SECURITY_GROUP_PORT_RANGE_MIN_1)
                .protocol(SECURITY_GROUP_PROTOCOL_1)
                .remoteIpPrefix(SECURITY_GROUP_REMOTE_IP_PREFIX_1)
                .build();

        sgRule12 = DefaultKubevirtSecurityGroupRule.builder()
                .id(SECURITY_GROUP_RULE_ID_1_2)
                .securityGroupId(SECURITY_GROUP_ID_1)
                .remoteGroupId(SECURITY_GROUP_ID_2)
                .direction(SECURITY_GROUP_DIRECTION)
                .etherType(SECURITY_GROUP_ETH_TYPE)
                .portRangeMax(SECURITY_GROUP_PORT_RANGE_MAX_2)
                .portRangeMin(SECURITY_GROUP_PORT_RANGE_MIN_2)
                .protocol(SECURITY_GROUP_PROTOCOL_2)
                .remoteIpPrefix(SECURITY_GROUP_REMOTE_IP_PREFIX_2)
                .build();

        sg1 = DefaultKubevirtSecurityGroup.builder()
                .id(SECURITY_GROUP_ID_1)
                .name(SECURITY_GROUP_NAME_1)
                .description(SECURITY_GROUP_DESCRIPTION_1)
                .build();

        sg2 = DefaultKubevirtSecurityGroup.builder()
                .id(SECURITY_GROUP_ID_2)
                .name(SECURITY_GROUP_NAME_2)
                .description(SECURITY_GROUP_DESCRIPTION_2)
                .build();
    }

    /**
     * Tears down all of this unit test.
     */
    @After
    public void tearDown() {
        target.removeListener(testListener);
        store.deactivate();
        target.deactivate();
        store = null;
        target = null;
    }

    /**
     * Tests if getting all security groups returns the correct set of groups.
     */
    @Test
    public void testGetSecurityGroups() {
        createBasicSecurityGroups();
        assertEquals("Number of security group did not match",
                2, target.securityGroups().size());
    }

    /**
     * Tests if getting a security group with group ID returns the correct group.
     */
    @Test
    public void testGetSecurityGroupById() {
        createBasicSecurityGroups();
        assertNotNull("Security group did not match", target.securityGroup(SECURITY_GROUP_ID_1));
        assertNotNull("Security group  did not match", target.securityGroup(SECURITY_GROUP_ID_2));
        assertNull("Security group  did not match", target.securityGroup(UNKNOWN_ID));
    }

    /**
     * Tests creating and removing a security group, and checks if it triggers proper events.
     */
    @Test
    public void testCreateAndRemoveSecurityGroup() {
        target.createSecurityGroup(sg1);
        assertEquals("Number of security group did not match",
                1, target.securityGroups().size());
        assertNotNull("Security group did not match",
                target.securityGroup(SECURITY_GROUP_ID_1));

        target.removeSecurityGroup(SECURITY_GROUP_ID_1);
        assertEquals("Number of security group did not match",
                0, target.securityGroups().size());
        assertNull("Security group did not match",
                target.securityGroup(SECURITY_GROUP_ID_1));

        validateEvents(KUBEVIRT_SECURITY_GROUP_CREATED, KUBEVIRT_SECURITY_GROUP_REMOVED);
    }

    /**
     * Tests creating and removing a security group rule, and checks if it triggers proper events.
     */
    @Test
    public void testCreateAndRemoveSecurityGroupRule() {
        target.createSecurityGroup(sg1);
        assertEquals("Number of security group rule did not match",
                0, target.securityGroup(sg1.id()).rules().size());

        target.createSecurityGroupRule(sgRule11);
        assertEquals("Number of security group rule did not match",
                1, target.securityGroup(sg1.id()).rules().size());

        target.createSecurityGroupRule(sgRule12);
        assertEquals("Number of security group rule did not match",
                2, target.securityGroup(sg1.id()).rules().size());

        target.removeSecurityGroupRule(sgRule11.id());
        assertEquals("Number of security group rule did not match",
                1, target.securityGroup(sg1.id()).rules().size());

        target.removeSecurityGroupRule(sgRule12.id());
        assertEquals("Number of security group rule did not match",
                0, target.securityGroup(sg1.id()).rules().size());

        validateEvents(KUBEVIRT_SECURITY_GROUP_CREATED,
                KUBEVIRT_SECURITY_GROUP_RULE_CREATED,
                KUBEVIRT_SECURITY_GROUP_RULE_CREATED,
                KUBEVIRT_SECURITY_GROUP_RULE_REMOVED,
                KUBEVIRT_SECURITY_GROUP_RULE_REMOVED);
    }

    /**
     * Tests if creating a null security group fails with an exception.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateNullSecurityGroup() {
        target.createSecurityGroup(null);
    }

    /**
     * Tests if creating a duplicated security group fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateDuplicateSecurityGroup() {
        target.createSecurityGroup(sg1);
        target.createSecurityGroup(sg1);
    }

    /**
     * Tests if removing security group with null ID fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRemoveSecurityGroupWithNull() {
        target.removeSecurityGroup(null);
    }

    /**
     * Tests if updating an unregistered security group fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateUnregisteredSecurityGroup() {
        target.updateSecurityGroup(sg1);
    }

    private void createBasicSecurityGroups() {
        target.createSecurityGroup(sg1);
        target.createSecurityGroup(sg2);
    }

    private static class TestCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return TEST_APP_ID;
        }
    }

    private static class TestKubevirtSecurityGroupListener
            implements KubevirtSecurityGroupListener {

        private List<KubevirtSecurityGroupEvent> events = Lists.newArrayList();

        @Override
        public void event(KubevirtSecurityGroupEvent event) {
            events.add(event);
        }
    }

    private void validateEvents(Enum... types) {
        int i = 0;
        assertEquals("Number of events did not match", types.length,
                testListener.events.size());
        for (Event event : testListener.events) {
            assertEquals("Incorrect event received", types[i], event.type());
            i++;
        }
        testListener.events.clear();
    }
}
