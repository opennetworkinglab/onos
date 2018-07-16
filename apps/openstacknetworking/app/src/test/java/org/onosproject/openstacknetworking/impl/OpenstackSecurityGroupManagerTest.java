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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.event.Event;
import org.onosproject.openstacknetworking.api.OpenstackSecurityGroupEvent;
import org.onosproject.openstacknetworking.api.OpenstackSecurityGroupListener;
import org.onosproject.store.service.TestStorageService;
import org.openstack4j.model.network.SecurityGroup;
import org.openstack4j.model.network.SecurityGroupRule;
import org.openstack4j.openstack.networking.domain.NeutronSecurityGroup;
import org.openstack4j.openstack.networking.domain.NeutronSecurityGroupRule;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.onosproject.openstacknetworking.api.OpenstackSecurityGroupEvent.Type.OPENSTACK_SECURITY_GROUP_CREATED;
import static org.onosproject.openstacknetworking.api.OpenstackSecurityGroupEvent.Type.OPENSTACK_SECURITY_GROUP_REMOVED;

/**
 * Unit tests for openstack security group manager.
 */
public class OpenstackSecurityGroupManagerTest {

    private static final ApplicationId TEST_APP_ID = new DefaultApplicationId(1, "test");

    private static final String SECURITY_GROUP_ID_1 = "sg-id-1";
    private static final String SECURITY_GROUP_ID_2 = "sg-id-2";
    private static final String UNKNOWN_ID = "sg-id-x";


    private static final String SECURITY_GROUP_NAME_1 = "sg-name-1";
    private static final String SECURITY_GROUP_NAME_2 = "sg-name-2";

    private static final String SECURITY_GROUP_TENANT_ID_1 = "tenant-id-1";
    private static final String SECURITY_GROUP_TENANT_ID_2 = "tenant-id-2";

    private static final String SECURITY_GROUP_DESCRIPTION_1 = "description-1";
    private static final String SECURITY_GROUP_DESCRIPTION_2 = "description-2";

    private static final String SECURITY_GROUP_RULE_ID_1_1 = "sgr-id-1-1";
    private static final String SECURITY_GROUP_RULE_ID_1_2 = "sgr-id-1-2";
    private static final String SECURITY_GROUP_RULE_ID_2_1 = "sgr-id-2-1";
    private static final String SECURITY_GROUP_RULE_ID_2_2 = "sgr-id-2-2";

    private static final String SECURITY_GROUP_ETH_TYPE = "IP";
    private static final String SECURITY_GROUP_DIRECTION = "EGRESS";
    private static final String SECURITY_GROUP_PROTOCOL_1 = "TCP";
    private static final String SECURITY_GROUP_PROTOCOL_2 = "UDP";

    private static final int SECURITY_GROUP_PORT_RANGE_MIN_1 = 1;
    private static final int SECURITY_GROUP_PORT_RANGE_MIN_2 = 101;
    private static final int SECURITY_GROUP_PORT_RANGE_MAX_1 = 100;
    private static final int SECURITY_GROUP_PORT_RANGE_MAX_2 = 200;

    private static final String SECURITY_GROUP_REMOTE_IP_PREFIX_1 = "1.1.1.0/24";
    private static final String SECURITY_GROUP_REMOTE_IP_PREFIX_2 = "2.2.2.0/24";

    private SecurityGroup securityGroup1;
    private SecurityGroup securityGroup2;

    private SecurityGroupRule securityGroupRule11;
    private SecurityGroupRule securityGroupRule12;
    private SecurityGroupRule securityGroupRule21;
    private SecurityGroupRule securityGroupRule22;

    private List<SecurityGroupRule> securityGroupRules1;
    private List<SecurityGroupRule> securityGroupRules2;

    private OpenstackSecurityGroupManager target;
    private DistributedSecurityGroupStore store;

    private final TestOpenstackSecurityGroupListener
            testOpenstackSecurityGroupListener = new TestOpenstackSecurityGroupListener();

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() throws Exception {

        store = new DistributedSecurityGroupStore();
        TestUtils.setField(store, "coreService", new TestCoreService());
        TestUtils.setField(store, "storageService", new TestStorageService());
        TestUtils.setField(store, "eventExecutor", MoreExecutors.newDirectExecutorService());
        store.activate();

        target = new OpenstackSecurityGroupManager();
        TestUtils.setField(target, "coreService", new TestCoreService());
        target.osSecurityGroupStore = store;
        target.addListener(testOpenstackSecurityGroupListener);
        target.activate();

        securityGroupRule11 = NeutronSecurityGroupRule.builder()
                                            .id(SECURITY_GROUP_RULE_ID_1_1)
                                            .securityGroupId(SECURITY_GROUP_ID_1)
                                            .tenantId(SECURITY_GROUP_TENANT_ID_1)
                                            .remoteGroupId(SECURITY_GROUP_ID_1)
                                            .ethertype(SECURITY_GROUP_ETH_TYPE)
                                            .direction(SECURITY_GROUP_DIRECTION)
                                            .portRangeMin(SECURITY_GROUP_PORT_RANGE_MIN_1)
                                            .portRangeMax(SECURITY_GROUP_PORT_RANGE_MAX_1)
                                            .protocol(SECURITY_GROUP_PROTOCOL_1)
                                            .remoteIpPrefix(SECURITY_GROUP_REMOTE_IP_PREFIX_1)
                                            .build();

        securityGroupRule12 = NeutronSecurityGroupRule.builder()
                                            .id(SECURITY_GROUP_RULE_ID_1_2)
                                            .securityGroupId(SECURITY_GROUP_ID_1)
                                            .tenantId(SECURITY_GROUP_TENANT_ID_1)
                                            .remoteGroupId(SECURITY_GROUP_ID_1)
                                            .ethertype(SECURITY_GROUP_ETH_TYPE)
                                            .direction(SECURITY_GROUP_DIRECTION)
                                            .portRangeMin(SECURITY_GROUP_PORT_RANGE_MIN_1)
                                            .portRangeMax(SECURITY_GROUP_PORT_RANGE_MAX_1)
                                            .protocol(SECURITY_GROUP_PROTOCOL_1)
                                            .remoteIpPrefix(SECURITY_GROUP_REMOTE_IP_PREFIX_1)
                                            .build();

        securityGroupRule21 = NeutronSecurityGroupRule.builder()
                                            .id(SECURITY_GROUP_RULE_ID_2_1)
                                            .securityGroupId(SECURITY_GROUP_ID_2)
                                            .tenantId(SECURITY_GROUP_TENANT_ID_2)
                                            .remoteGroupId(SECURITY_GROUP_ID_2)
                                            .ethertype(SECURITY_GROUP_ETH_TYPE)
                                            .direction(SECURITY_GROUP_DIRECTION)
                                            .portRangeMin(SECURITY_GROUP_PORT_RANGE_MIN_2)
                                            .portRangeMax(SECURITY_GROUP_PORT_RANGE_MAX_2)
                                            .protocol(SECURITY_GROUP_PROTOCOL_2)
                                            .remoteIpPrefix(SECURITY_GROUP_REMOTE_IP_PREFIX_2)
                                            .build();

        securityGroupRule22 = NeutronSecurityGroupRule.builder()
                                            .id(SECURITY_GROUP_RULE_ID_2_2)
                                            .securityGroupId(SECURITY_GROUP_ID_2)
                                            .tenantId(SECURITY_GROUP_TENANT_ID_2)
                                            .remoteGroupId(SECURITY_GROUP_ID_2)
                                            .ethertype(SECURITY_GROUP_ETH_TYPE)
                                            .direction(SECURITY_GROUP_DIRECTION)
                                            .portRangeMin(SECURITY_GROUP_PORT_RANGE_MIN_2)
                                            .portRangeMax(SECURITY_GROUP_PORT_RANGE_MAX_2)
                                            .protocol(SECURITY_GROUP_PROTOCOL_2)
                                            .remoteIpPrefix(SECURITY_GROUP_REMOTE_IP_PREFIX_2)
                                            .build();

        securityGroupRules1 = ImmutableList.of(securityGroupRule11, securityGroupRule12);
        securityGroupRules2 = ImmutableList.of(securityGroupRule21, securityGroupRule22);

        securityGroup1 = NeutronSecurityGroup.builder()
                                            .id(SECURITY_GROUP_ID_1)
                                            .name(SECURITY_GROUP_NAME_1)
                                            .tenantId(SECURITY_GROUP_TENANT_ID_1)
                                            .description(SECURITY_GROUP_DESCRIPTION_1)
                                            .build();

        securityGroup2 = NeutronSecurityGroup.builder()
                                            .id(SECURITY_GROUP_ID_2)
                                            .name(SECURITY_GROUP_NAME_2)
                                            .tenantId(SECURITY_GROUP_TENANT_ID_2)
                                            .description(SECURITY_GROUP_DESCRIPTION_2)
                                            .build();

    }

    /**
     * Tears down all of this unit test.
     */
    @After
    public void tearDown() {
        target.removeListener(testOpenstackSecurityGroupListener);
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
        assertNotNull("Instance port did not match", target.securityGroup(SECURITY_GROUP_ID_1));
        assertNotNull("Instance port did not match", target.securityGroup(SECURITY_GROUP_ID_2));
        assertNull("Instance port did not match", target.securityGroup(UNKNOWN_ID));
    }

    /**
     * Tests creating and removing a security group, and checks if it triggers proper events.
     */
    @Test
    public void testCreateAndRemoveSecurityGroup() {
        target.createSecurityGroup(securityGroup1);
        assertEquals("Number of security group did not match",
                1, target.securityGroups().size());
        assertNotNull("Security group did not match",
                target.securityGroup(SECURITY_GROUP_ID_1));

        target.removeSecurityGroup(SECURITY_GROUP_ID_1);
        assertEquals("Number of security group did not match",
                0, target.securityGroups().size());
        assertNull("Security group did not match",
                target.securityGroup(SECURITY_GROUP_ID_1));

        validateEvents(OPENSTACK_SECURITY_GROUP_CREATED, OPENSTACK_SECURITY_GROUP_REMOVED);
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
        target.createSecurityGroup(securityGroup1);
        target.createSecurityGroup(securityGroup1);
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
        target.updateSecurityGroup(securityGroup1);
    }

    private void createBasicSecurityGroups() {
        target.createSecurityGroup(securityGroup1);
        target.createSecurityGroup(securityGroup2);
    }

    private static class TestCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return TEST_APP_ID;
        }
    }

    private static class TestOpenstackSecurityGroupListener
                                    implements OpenstackSecurityGroupListener {
        private List<OpenstackSecurityGroupEvent> events = Lists.newArrayList();

        @Override
        public void event(OpenstackSecurityGroupEvent event) {
            events.add(event);
        }
    }

    private void validateEvents(Enum... types) {
        int i = 0;
        assertEquals("Number of events did not match", types.length,
                testOpenstackSecurityGroupListener.events.size());
        for (Event event : testOpenstackSecurityGroupListener.events) {
            assertEquals("Incorrect event received", types[i], event.type());
            i++;
        }
        testOpenstackSecurityGroupListener.events.clear();
    }
}
