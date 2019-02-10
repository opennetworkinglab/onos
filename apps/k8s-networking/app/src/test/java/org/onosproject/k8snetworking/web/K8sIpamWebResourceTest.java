/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.web;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.k8snetworking.api.DefaultK8sNetwork;
import org.onosproject.k8snetworking.api.K8sIpam;
import org.onosproject.k8snetworking.api.K8sIpamAdminService;
import org.onosproject.k8snetworking.api.K8sNetwork;
import org.onosproject.k8snetworking.api.K8sNetworkService;
import org.onosproject.k8snetworking.codec.K8sIpamCodec;
import org.onosproject.rest.resources.ResourceTest;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static junit.framework.TestCase.assertEquals;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * Unit test for kubernetes IPAM REST API.
 */
public class K8sIpamWebResourceTest extends ResourceTest {

    final K8sNetworkService mockNetworkService = createMock(K8sNetworkService.class);
    final K8sIpamAdminService mockIpamService = createMock(K8sIpamAdminService.class);

    private static final String IPAM = "ipam";

    private K8sNetwork k8sNetwork;

    /**
     * Constructs a kubernetes networking resource test instance.
     */
    public K8sIpamWebResourceTest() {
        super(ResourceConfig.forApplicationClass(K8sNetworkingWebApplication.class));
    }

    /**
     * Sets up the global values for all the tests.
     */
    @Before
    public void setUpTest() {
        final CodecManager codecService = new CodecManager();
        codecService.activate();
        codecService.registerCodec(K8sIpam.class, new K8sIpamCodec());
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(K8sNetworkService.class, mockNetworkService)
                        .add(K8sIpamAdminService.class, mockIpamService)
                        .add(CodecService.class, codecService);
        setServiceDirectory(testDirectory);

        k8sNetwork = DefaultK8sNetwork.builder()
                .networkId("sona-network")
                .name("sona-network")
                .segmentId("1")
                .cidr("10.10.10.0/24")
                .gatewayIp(IpAddress.valueOf("10.10.10.1"))
                .type(K8sNetwork.Type.VXLAN)
                .mtu(1500)
                .build();
    }

    /**
     * Tests the IP allocation with incorrect network ID.
     */
    @Test
    public void testAllocateIpWithIncorrectNetId() {
        expect(mockNetworkService.network(anyObject())).andReturn(null);

        replay(mockNetworkService);

        final WebTarget wt = target();
        Response response = wt.path(IPAM + "/sona-network").request().get();
        final int status = response.getStatus();

        assertEquals(404, status);

        verify(mockNetworkService);
    }

    /**
     * Tests the IP allocation with null IP address returned.
     */
    @Test
    public void testAllocateIpWithNullIp() {
        expect(mockNetworkService.network(anyObject())).andReturn(k8sNetwork);
        expect(mockIpamService.allocateIp(anyObject())).andReturn(null);

        replay(mockNetworkService);
        replay(mockIpamService);

        final WebTarget wt = target();
        Response response = wt.path(IPAM + "/sona-network").request().get();
        final int status = response.getStatus();

        assertEquals(404, status);

        verify(mockNetworkService);
        verify(mockIpamService);
    }

    /**
     * Tests the IP allocation with correct IP address returned.
     */
    @Test
    public void testAllocateIp() {
        expect(mockNetworkService.network(anyObject())).andReturn(k8sNetwork);
        expect(mockIpamService.allocateIp(anyObject()))
                .andReturn(IpAddress.valueOf("10.10.10.2"));

        replay(mockNetworkService);
        replay(mockIpamService);

        final WebTarget wt = target();
        Response response = wt.path(IPAM + "/sona-network").request().get();
        final int status = response.getStatus();

        assertEquals(200, status);

        verify(mockNetworkService);
        verify(mockIpamService);
    }

    /**
     * Tests the IP allocation with incorrect network ID.
     */
    @Test
    public void testReleaseIpWithIncorrectNetIdAndIp() {
        expect(mockNetworkService.network(anyObject())).andReturn(null);

        replay(mockNetworkService);

        final WebTarget wt = target();
        Response response = wt.path(IPAM + "/sona-network/10.10.10.2").request().delete();
        final int status = response.getStatus();

        assertEquals(404, status);

        verify(mockNetworkService);
    }

    /**
     * Tests the IP allocation with correct network ID and IP address.
     */
    @Test
    public void testReleaseIpWithCorrectNetIdAndIp() {
        expect(mockNetworkService.network(anyObject())).andReturn(k8sNetwork);
        expect(mockIpamService.releaseIp(anyObject(), anyObject())).andReturn(true);

        replay(mockNetworkService);
        replay(mockIpamService);

        final WebTarget wt = target();
        Response response = wt.path(IPAM + "/sona-network/10.10.10.2").request().delete();
        final int status = response.getStatus();

        assertEquals(204, status);

        verify(mockNetworkService);
        verify(mockIpamService);
    }
}
