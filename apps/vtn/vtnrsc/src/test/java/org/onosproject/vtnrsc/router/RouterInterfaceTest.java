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
package org.onosproject.vtnrsc.router;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

import org.junit.Test;
import org.onosproject.vtnrsc.RouterId;
import org.onosproject.vtnrsc.RouterInterface;
import org.onosproject.vtnrsc.SubnetId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.VirtualPortId;

import com.google.common.testing.EqualsTester;

/**
 * Unit tests for RouterInterface class.
 */
public class RouterInterfaceTest {
    private String tenantIdStr = "123";
    private String virtualPortId = "1212";
    private String routeIdStr1 = "1";
    private String routeIdStr2 = "2";
    private String subnetIdStr = "1234567";

    /**
     * Checks that the RouterInterface class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(RouterInterface.class);
    }

    /**
     * Checks the operation of equals().
     */
    @Test
    public void testEquals() {
        final TenantId tenantId = TenantId.tenantId(tenantIdStr);
        final VirtualPortId portId = VirtualPortId.portId(virtualPortId);
        final RouterId routerId1 = RouterId.valueOf(routeIdStr1);
        final RouterId routerId2 = RouterId.valueOf(routeIdStr2);
        final SubnetId subnet = SubnetId.subnetId(subnetIdStr);

        RouterInterface ri1 = RouterInterface.routerInterface(subnet, portId,
                                                              routerId1,
                                                              tenantId);
        RouterInterface ri2 = RouterInterface.routerInterface(subnet, portId,
                                                              routerId1,
                                                              tenantId);
        RouterInterface ri3 = RouterInterface.routerInterface(subnet, portId,
                                                              routerId2,
                                                              tenantId);

        new EqualsTester().addEqualityGroup(ri1, ri2).addEqualityGroup(ri3)
                .testEquals();
    }

    /**
     * Checks the construction of a RouterInterface object.
     */
    @Test
    public void testConstruction() {
        final TenantId tenantId = TenantId.tenantId(tenantIdStr);
        final VirtualPortId portId = VirtualPortId.portId(virtualPortId);
        final RouterId routerId1 = RouterId.valueOf(routeIdStr1);
        final SubnetId subnet = SubnetId.subnetId(subnetIdStr);

        RouterInterface ri1 = RouterInterface.routerInterface(subnet, portId,
                                                              routerId1,
                                                              tenantId);
        assertThat(portId, is(notNullValue()));
        assertThat(portId, is(ri1.portId()));
        assertThat(tenantId, is(notNullValue()));
        assertThat(tenantId, is(ri1.tenantId()));
        assertThat(routerId1, is(notNullValue()));
        assertThat(routerId1, is(ri1.routerId()));
        assertThat(subnet, is(notNullValue()));
        assertThat(subnet, is(ri1.subnetId()));
    }
}
