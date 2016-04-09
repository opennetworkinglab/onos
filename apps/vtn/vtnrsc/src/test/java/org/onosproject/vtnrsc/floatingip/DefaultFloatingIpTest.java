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
package org.onosproject.vtnrsc.floatingip;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.vtnrsc.DefaultFloatingIp;
import org.onosproject.vtnrsc.FloatingIp;
import org.onosproject.vtnrsc.FloatingIpId;
import org.onosproject.vtnrsc.RouterId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantNetworkId;
import org.onosproject.vtnrsc.VirtualPortId;

import com.google.common.testing.EqualsTester;

/**
 * Unit tests for DefaultFloatingIp class.
 */
public class DefaultFloatingIpTest {

    private String floatingIpIdStr1 = "5fb63824-4d5c-4b85-9f2f-ebb93c9ce3df";
    private String floatingIpIdStr2 = "fa44f585-fe02-40d3-afe7-d1d7e5782c99";
    private String floatingIpStr = "10.1.1.2";
    private String fixedIpStr = "192.168.1.2";
    private String tenantIdStr = "123";
    private String tenantNetworkId = "1234567";
    private String virtualPortId = "1212";
    private String routerIdStr = "123";

    /**
     * Checks that the DefaultFloatingIp class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultFloatingIp.class);
    }

    /**
     * Checks the operation of equals().
     */
    @Test
    public void testEquals() {
        final TenantId tenantId = TenantId.tenantId(tenantIdStr);
        final TenantNetworkId networkId = TenantNetworkId
                .networkId(tenantNetworkId);
        final VirtualPortId portId = VirtualPortId.portId(virtualPortId);
        final RouterId routerId = RouterId.valueOf(routerIdStr);
        final FloatingIpId id1 = FloatingIpId.of(floatingIpIdStr1);
        final FloatingIpId id2 = FloatingIpId.of(floatingIpIdStr2);
        final IpAddress floatingIpAddress = IpAddress.valueOf(floatingIpStr);
        final IpAddress fixedIpAddress = IpAddress.valueOf(fixedIpStr);

        FloatingIp fip1 = new DefaultFloatingIp(id1, tenantId, networkId,
                                                portId, routerId,
                                                floatingIpAddress,
                                                fixedIpAddress,
                                                FloatingIp.Status.ACTIVE);
        FloatingIp fip2 = new DefaultFloatingIp(id1, tenantId, networkId,
                                                portId, routerId,
                                                floatingIpAddress,
                                                fixedIpAddress,
                                                FloatingIp.Status.ACTIVE);
        FloatingIp fip3 = new DefaultFloatingIp(id2, tenantId, networkId,
                                                portId, routerId,
                                                floatingIpAddress,
                                                fixedIpAddress,
                                                FloatingIp.Status.ACTIVE);

        new EqualsTester().addEqualityGroup(fip1, fip2).addEqualityGroup(fip3)
                .testEquals();
    }

    /**
     * Checks the construction of a DefaultFloatingIp object.
     */
    @Test
    public void testConstruction() {
        final TenantId tenantId = TenantId.tenantId(tenantIdStr);
        final TenantNetworkId networkId = TenantNetworkId
                .networkId(tenantNetworkId);
        final VirtualPortId portId = VirtualPortId.portId(virtualPortId);
        final RouterId routerId = RouterId.valueOf(routerIdStr);
        final FloatingIpId id = FloatingIpId.of(floatingIpIdStr1);
        final IpAddress floatingIpAddress = IpAddress.valueOf(floatingIpStr);
        final IpAddress fixedIpAddress = IpAddress.valueOf(fixedIpStr);

        FloatingIp fip = new DefaultFloatingIp(id, tenantId, networkId, portId,
                                               routerId, floatingIpAddress,
                                               fixedIpAddress,
                                               FloatingIp.Status.ACTIVE);
        assertThat(id, is(notNullValue()));
        assertThat(id, is(fip.id()));
        assertThat(tenantId, is(notNullValue()));
        assertThat(tenantId, is(fip.tenantId()));
        assertThat(networkId, is(notNullValue()));
        assertThat(networkId, is(fip.networkId()));
        assertThat(portId, is(notNullValue()));
        assertThat(portId, is(fip.portId()));
        assertThat(routerId, is(notNullValue()));
        assertThat(routerId, is(fip.routerId()));
        assertThat(floatingIpAddress, is(notNullValue()));
        assertThat(floatingIpAddress, is(fip.floatingIp()));
        assertThat(fixedIpAddress, is(notNullValue()));
        assertThat(fixedIpAddress, is(fip.fixedIp()));
    }
}
