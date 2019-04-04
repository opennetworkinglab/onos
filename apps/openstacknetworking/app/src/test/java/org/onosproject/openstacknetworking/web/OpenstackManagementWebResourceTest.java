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
package org.onosproject.openstacknetworking.web;

import com.google.common.collect.Sets;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onosproject.core.CoreService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkAdminService;
import org.onosproject.openstacknetworking.api.OpenstackRouterAdminService;
import org.onosproject.openstacknetworking.api.OpenstackSecurityGroupAdminService;
import org.onosproject.openstacknode.api.OpenstackNodeAdminService;
import org.onosproject.rest.resources.ResourceTest;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.openstack.networking.domain.NeutronFloatingIP;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.Set;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for openstack management web resource REST API.
 */
public class OpenstackManagementWebResourceTest extends ResourceTest {

    final OpenstackRouterAdminService mockOpenstackRouterAdminService =
            createMock(OpenstackRouterAdminService.class);
    final OpenstackSecurityGroupAdminService mockOpenstackSecurityGroupAdminService =
            createMock(OpenstackSecurityGroupAdminService.class);
    final OpenstackNetworkAdminService mockOpenstackNetworkAdminService =
            createMock(OpenstackNetworkAdminService.class);
    final OpenstackNodeAdminService mockOpenstackNodeAdminService =
            createMock(OpenstackNodeAdminService.class);
    final CoreService mockCoreService = createMock(CoreService.class);
    final FlowRuleService mockFlowRuleService = createMock(FlowRuleService.class);

    private static final String PATH = "management";

    /**
     * Constructs an openstack management REST API test.
     */
    public OpenstackManagementWebResourceTest() {
        super(ResourceConfig.forApplicationClass(OpenstackNetworkingWebApplication.class));
    }

    /**
     * Sets up the global values for all tests.
     */
    @Before
    public void setUpTest() {
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                .add(OpenstackRouterAdminService.class, mockOpenstackRouterAdminService)
                .add(OpenstackSecurityGroupAdminService.class, mockOpenstackSecurityGroupAdminService)
                .add(OpenstackNetworkAdminService.class, mockOpenstackNetworkAdminService)
                .add(OpenstackNodeAdminService.class, mockOpenstackNodeAdminService);
        setServiceDirectory(testDirectory);
    }

    /**
     * Tests the get all floating IPs method.
     */
    @Test
    public void testGetAllFloatingIps() {
        final Set<NetFloatingIP> floatignIps = Sets.newConcurrentHashSet();
        NetFloatingIP ip1 = NeutronFloatingIP.builder()
                .portId("port-id-1")
                .floatingNetworkId("network-id-1")
                .build();
        floatignIps.add(ip1);
        expect(mockOpenstackRouterAdminService.floatingIps()).andReturn(floatignIps).anyTimes();
        replay(mockOpenstackRouterAdminService);

        final WebTarget wt = target();

        Response response = wt.path(PATH + "/floatingips/all")
                .request()
                .get();
        final int status = response.getStatus();

        assertThat(status, is(200));
    }

    /**
     * Tests the get mapped floating IPs method.
     */
    @Test
    public void testGetMappedFloatingIps() {
        final Set<NetFloatingIP> floatignIps = Sets.newConcurrentHashSet();
        NetFloatingIP ip1 = NeutronFloatingIP.builder()
                .portId("port-id-1")
                .floatingNetworkId("network-id-1")
                .build();
        floatignIps.add(ip1);
        expect(mockOpenstackRouterAdminService.floatingIps()).andReturn(floatignIps).anyTimes();
        replay(mockOpenstackRouterAdminService);

        final WebTarget wt = target();

        Response response = wt.path(PATH + "/floatingips/mapped")
                .request()
                .get();
        final int status = response.getStatus();

        assertThat(status, is(200));
    }
}
