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
package org.onosproject.openstacknode.api;

import com.google.common.collect.Lists;
import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.openstacknode.api.DpdkConfig.DatapathType.NETDEV;
import static org.onosproject.openstacknode.api.DpdkConfig.DatapathType.NORMAL;
import static org.onosproject.openstacknode.api.DpdkInterface.Type.DPDK_VHOST_USER;
import static org.onosproject.openstacknode.api.DpdkInterface.Type.DPDK_VHOST_USER_CLIENT;

/**
 * Unit tests for DefaultDpdkConfig.
 */
public class DefaultDpdkConfigTest {

    private static final DpdkConfig.DatapathType DATAPATH_TYPE_1 = NETDEV;
    private static final DpdkConfig.DatapathType DATAPATH_TYPE_2 = NORMAL;

    private static final String SOCKET_DIR_1 = "/var/lib/libvirt/qemu";
    private static final String SOCKET_DIR_2 = "/var/lib/libvirt/kvm";

    private static final List<DpdkInterface> DPDK_INTFS_1 = Lists.newArrayList();
    private static final List<DpdkInterface> DPDK_INTFS_2 = Lists.newArrayList();

    private DpdkConfig config1;
    private DpdkConfig sameAsConfig1;
    private DpdkConfig config2;

    /**
     * Tests class immutability.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultDpdkConfig.class);
    }

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        DpdkInterface dpdkIntf1 = DefaultDpdkInterface.builder()
                                        .intf("dpdk1")
                                        .deviceName("br-int")
                                        .mtu(1500L)
                                        .pciAddress("0000:85:00.0")
                                        .type(DPDK_VHOST_USER)
                                        .build();

        DpdkInterface dpdkIntf2 = DefaultDpdkInterface.builder()
                                        .intf("dpdk2")
                                        .deviceName("br-int")
                                        .mtu(1500L)
                                        .pciAddress("0000:85:00.0")
                                        .type(DPDK_VHOST_USER_CLIENT)
                                        .build();

        DPDK_INTFS_1.add(dpdkIntf1);
        DPDK_INTFS_2.add(dpdkIntf2);

        config1 = DefaultDpdkConfig.builder()
                                        .datapathType(DATAPATH_TYPE_1)
                                        .socketDir(SOCKET_DIR_1)
                                        .dpdkIntfs(DPDK_INTFS_1)
                                        .build();

        sameAsConfig1 = DefaultDpdkConfig.builder()
                                        .datapathType(DATAPATH_TYPE_1)
                                        .socketDir(SOCKET_DIR_1)
                                        .dpdkIntfs(DPDK_INTFS_1)
                                        .build();

        config2 = DefaultDpdkConfig.builder()
                                        .datapathType(DATAPATH_TYPE_2)
                                        .socketDir(SOCKET_DIR_2)
                                        .dpdkIntfs(DPDK_INTFS_2)
                                        .build();
    }

    /**
     * Checks equals method works as expected.
     */
    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(config1, sameAsConfig1)
                .addEqualityGroup(config2)
                .testEquals();
    }

    /**
     * Test object construction.
     */
    @Test
    public void testConstruction() {
        DpdkConfig config = config1;

        assertEquals(config.datapathType(), DATAPATH_TYPE_1);
        assertEquals(config.socketDir(), SOCKET_DIR_1);
        assertEquals(config.dpdkIntfs(), DPDK_INTFS_1);
    }
}
