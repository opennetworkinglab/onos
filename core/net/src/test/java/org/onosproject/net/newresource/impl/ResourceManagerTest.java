/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net.newresource.impl;

import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.LinkKey;
import org.onosproject.net.PortNumber;
import org.onosproject.net.newresource.ResourcePath;

import java.util.function.Predicate;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for ResourceManager.
 */
public class ResourceManagerTest {

    private static final DeviceId D1 = DeviceId.deviceId("of:001");
    private static final DeviceId D2 = DeviceId.deviceId("of:002");
    private static final PortNumber P1 = PortNumber.portNumber(1);
    private static final ConnectPoint CP1_1 = new ConnectPoint(D1, P1);
    private static final ConnectPoint CP2_1 = new ConnectPoint(D2, P1);
    private static final short VLAN_LOWER_LIMIT = 0;
    private static final short VLAN_UPPER_LIMIT = 1024;

    private final Predicate<VlanId> vlanPredicate =
            x -> x.toShort() >= VLAN_LOWER_LIMIT && x.toShort() < VLAN_UPPER_LIMIT;
    private ResourceManager manager;

    @Before
    public void setUp() {
        manager = new ResourceManager();
    }

    /**
     * Tests resource boundaries.
     */
    @Test
    public void testBoundary() {
        manager.defineResourceBoundary(VlanId.class, vlanPredicate);

        LinkKey linkKey = LinkKey.linkKey(CP1_1, CP2_1);

        assertThat(manager.isValid(new ResourcePath(linkKey, VlanId.vlanId((short) (VLAN_LOWER_LIMIT - 1)))),
                is(false));

        assertThat(manager.isValid(new ResourcePath(linkKey, VlanId.vlanId(VLAN_LOWER_LIMIT))),
                is(true));

        assertThat(manager.isValid(new ResourcePath(linkKey, VlanId.vlanId((short) 100))),
                is(true));

        assertThat(manager.isValid(new ResourcePath(linkKey, VlanId.vlanId((short) (VLAN_UPPER_LIMIT - 1)))),
                is(true));

        assertThat(manager.isValid(new ResourcePath(linkKey, VlanId.vlanId(VLAN_UPPER_LIMIT))),
                is(false));
    }

    /**
     * Tests the case that a boundary is not set.
     */
    @Test
    public void testWhenBoundaryNotSet() {
        LinkKey linkKey = LinkKey.linkKey(CP1_1, CP2_1);

        assertThat(manager.isValid(new ResourcePath(linkKey, VlanId.vlanId((short) (VLAN_LOWER_LIMIT - 1)))),
                is(true));

        assertThat(manager.isValid(new ResourcePath(linkKey, VlanId.vlanId(VLAN_LOWER_LIMIT))),
                is(true));

        assertThat(manager.isValid(new ResourcePath(linkKey, VlanId.vlanId((short) 100))),
                is(true));

        assertThat(manager.isValid(new ResourcePath(linkKey, VlanId.vlanId((short) (VLAN_UPPER_LIMIT - 1)))),
                is(true));

        assertThat(manager.isValid(new ResourcePath(linkKey, VlanId.vlanId(VLAN_UPPER_LIMIT))),
                is(true));
    }
}
