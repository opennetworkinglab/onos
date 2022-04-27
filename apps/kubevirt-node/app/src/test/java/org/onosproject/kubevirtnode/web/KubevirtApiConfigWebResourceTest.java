/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.kubevirtnode.web;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.kubevirtnode.api.DefaultKubevirtApiConfig;
import org.onosproject.kubevirtnode.api.KubevirtApiConfig;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigAdminService;
import org.onosproject.kubevirtnode.codec.KubevirtApiConfigCodec;
import org.onosproject.rest.resources.ResourceTest;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.onosproject.kubevirtnode.api.KubevirtApiConfig.Scheme.HTTPS;
import static org.onosproject.kubevirtnode.api.KubevirtApiConfig.State.DISCONNECTED;

/**
 * Unit test for KubeVirt node REST API.
 */
public class KubevirtApiConfigWebResourceTest extends ResourceTest {

    final KubevirtApiConfigAdminService mockConfigAdminService =
            createMock(KubevirtApiConfigAdminService.class);
    private static final String PATH = "api-config";

    private KubevirtApiConfig kubevirtApiConfig;

    /**
     * Constructs a KubeVirt API config resource test instance.
     */
    public KubevirtApiConfigWebResourceTest() {
        super(ResourceConfig.forApplicationClass(KubevirtNodeWebApplication.class));
    }

    /**
     * Sets up the global values for all the tests.
     */
    @Before
    public void setUpTest() {
        final CodecManager codecService = new CodecManager();
        codecService.activate();
        codecService.registerCodec(KubevirtApiConfig.class, new KubevirtApiConfigCodec());
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                .add(KubevirtApiConfigAdminService.class, mockConfigAdminService)
                .add(CodecService.class, codecService);
        setServiceDirectory(testDirectory);

        kubevirtApiConfig = DefaultKubevirtApiConfig.builder()
                .scheme(HTTPS)
                .ipAddress(IpAddress.valueOf("10.134.34.223"))
                .port(6443)
                .state(DISCONNECTED)
                .token("tokenMod")
                .caCertData("caCertData")
                .clientCertData("clientCertData")
                .clientKeyData("clientKeyData")
                .datacenterId("DB")
                .clusterId("BD-MEH-CT01")
                .build();
    }

    /**
     * Tests the results of the REST API POST method with creating new configs operation.
     */
    @Test
    public void testCreateConfigWithCreateOperation() {
        mockConfigAdminService.createApiConfig(anyObject());
        replay(mockConfigAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = KubevirtApiConfigWebResourceTest.class
                .getResourceAsStream("kubevirt-api-config.json");
        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(201));

        verify(mockConfigAdminService);
    }

    /**
     * Tests the results of the REST API POST method without creating new configs operation.
     */
    @Test
    public void testCreateConfigWithNullConfig() {
        mockConfigAdminService.createApiConfig(null);
        replay(mockConfigAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = KubevirtApiConfigWebResourceTest.class
                .getResourceAsStream("kubevirt-api-config.json");
        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(500));
    }

    /**
     * Tests the results of the REST API DELETE method with deleting the configs.
     */
    @Test
    public void testDeleteConfigsWithDeletionOperation() {
        expect(mockConfigAdminService.apiConfig())
                .andReturn(kubevirtApiConfig).once();
        expect(mockConfigAdminService.removeApiConfig(anyString()))
                .andReturn(kubevirtApiConfig).once();
        replay(mockConfigAdminService);

        String location = PATH + "/https://10.134.34.223:6443";

        final WebTarget wt = target();
        Response response = wt.path(location).request(
                MediaType.APPLICATION_JSON_TYPE).delete();

        final int status = response.getStatus();

        assertThat(status, is(204));

        verify(mockConfigAdminService);
    }

    /**
     * Tests the results of the REST API DELETE method without deleting the configs.
     */
    @Test
    public void testDeleteConfigsWithoutDeletionOperation() {
        expect(mockConfigAdminService.apiConfig()).andReturn(null).once();
        replay(mockConfigAdminService);

        String location = PATH + "/https://test:8663";

        final WebTarget wt = target();
        Response response = wt.path(location).request(
                MediaType.APPLICATION_JSON_TYPE).delete();

        final int status = response.getStatus();

        assertThat(status, is(304));

        verify(mockConfigAdminService);
    }
}
