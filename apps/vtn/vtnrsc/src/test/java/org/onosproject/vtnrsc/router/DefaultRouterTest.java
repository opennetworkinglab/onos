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

import java.util.Collections;

import org.junit.Test;
import org.onosproject.vtnrsc.DefaultRouter;
import org.onosproject.vtnrsc.Router;
import org.onosproject.vtnrsc.RouterGateway;
import org.onosproject.vtnrsc.RouterId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantNetworkId;
import org.onosproject.vtnrsc.VirtualPortId;

import com.google.common.testing.EqualsTester;

/**
 * Unit tests for DefaultRouter class.
 */
public class DefaultRouterTest {

    private String tenantIdStr = "123";
    private String virtualPortId = "1212";
    private String routeIdStr1 = "1";
    private String routeIdStr2 = "2";
    private String routerName = "router";
    private String tenantNetworkId = "1234567";

    /**
     * Checks that the DefaultRouter class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultRouter.class);
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
        final TenantNetworkId networkId = TenantNetworkId
                .networkId(tenantNetworkId);
        final RouterGateway routerGateway = RouterGateway.routerGateway(
                                                              networkId,
                                                              true,
                                                              Collections
                                                                      .emptySet());

        Router r1 = new DefaultRouter(routerId1, routerName, false,
                                      Router.Status.ACTIVE, false,
                                      routerGateway, portId, tenantId, null);
        Router r2 = new DefaultRouter(routerId1, routerName, false,
                                      Router.Status.ACTIVE, false,
                                      routerGateway, portId, tenantId, null);
        Router r3 = new DefaultRouter(routerId2, routerName, false,
                                      Router.Status.ACTIVE, false,
                                      routerGateway, portId, tenantId, null);

        new EqualsTester().addEqualityGroup(r1, r2).addEqualityGroup(r3)
                .testEquals();
    }

    /**
     * Checks the construction of a DefaultRouter object.
     */
    @Test
    public void testConstruction() {
        final TenantId tenantId = TenantId.tenantId(tenantIdStr);
        final VirtualPortId portId = VirtualPortId.portId(virtualPortId);
        final RouterId routerId = RouterId.valueOf(routeIdStr1);
        final TenantNetworkId networkId = TenantNetworkId
                .networkId(tenantNetworkId);
        final RouterGateway routerGateway = RouterGateway.routerGateway(
                                                              networkId,
                                                              true,
                                                              Collections
                                                                      .emptySet());

        Router r1 = new DefaultRouter(routerId, routerName, false,
                                      Router.Status.ACTIVE, false,
                                      routerGateway, portId, tenantId, null);
        assertThat(routerId, is(notNullValue()));
        assertThat(routerId, is(r1.id()));
        assertThat(tenantId, is(notNullValue()));
        assertThat(tenantId, is(r1.tenantId()));
        assertThat(routerGateway, is(notNullValue()));
        assertThat(routerGateway, is(r1.externalGatewayInfo()));
    }

}
