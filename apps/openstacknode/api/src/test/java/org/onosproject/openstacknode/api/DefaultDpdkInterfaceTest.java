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

import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.openstacknode.api.DpdkInterface.Type.DPDK_VHOST_USER;
import static org.onosproject.openstacknode.api.DpdkInterface.Type.DPDK_VHOST_USER_CLIENT;

/**
 * Unit tests for DefaultDpdkInterface.
 */
public class DefaultDpdkInterfaceTest {

    private static final String DEVICE_NAME_1 = "br-int";
    private static final String DEVICE_NAME_2 = "br-tun";

    private static final String INTF_NAME_1 = "dpdk0";
    private static final String INTF_NAME_2 = "dpdk1";

    private static final String PCI_ADDRESS_1 = "0000:85:00.0";
    private static final String PCI_ADDRESS_2 = "0000:85:00.1";

    private static final DpdkInterface.Type TYPE_1 = DPDK_VHOST_USER;
    private static final DpdkInterface.Type TYPE_2 = DPDK_VHOST_USER_CLIENT;

    private static final Long MTU_1 = 1500L;
    private static final Long MTU_2 = 1600L;

    private DpdkInterface dpdkIntf1;
    private DpdkInterface sameAsDpdkIntf1;
    private DpdkInterface dpdkIntf2;

    /**
     * Tests class immutability.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultDpdkInterface.class);
    }

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        dpdkIntf1 = DefaultDpdkInterface.builder()
                            .type(TYPE_1)
                            .pciAddress(PCI_ADDRESS_1)
                            .mtu(MTU_1)
                            .deviceName(DEVICE_NAME_1)
                            .intf(INTF_NAME_1)
                            .build();

        sameAsDpdkIntf1 = DefaultDpdkInterface.builder()
                            .type(TYPE_1)
                            .pciAddress(PCI_ADDRESS_1)
                            .mtu(MTU_1)
                            .deviceName(DEVICE_NAME_1)
                            .intf(INTF_NAME_1)
                            .build();

        dpdkIntf2 = DefaultDpdkInterface.builder()
                            .type(TYPE_2)
                            .pciAddress(PCI_ADDRESS_2)
                            .mtu(MTU_2)
                            .deviceName(DEVICE_NAME_2)
                            .intf(INTF_NAME_2)
                            .build();
    }

    /**
     * Tests object equality.
     */
    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(dpdkIntf1, sameAsDpdkIntf1)
                .addEqualityGroup(dpdkIntf2)
                .testEquals();
    }

    /**
     * Test object construction.
     */
    @Test
    public void testConstruction() {
        DpdkInterface dpdkIntf = dpdkIntf1;

        assertEquals(dpdkIntf.deviceName(), DEVICE_NAME_1);
        assertEquals(dpdkIntf.intf(), INTF_NAME_1);
        assertEquals(dpdkIntf.mtu(), MTU_1);
        assertEquals(dpdkIntf.pciAddress(), PCI_ADDRESS_1);
        assertEquals(dpdkIntf.type(), TYPE_1);
    }
}
