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
package org.onosproject.openstacktelemetry.web;

import com.google.common.collect.Maps;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onosproject.openstacktelemetry.api.DefaultTelemetryConfig;
import org.onosproject.openstacktelemetry.api.TelemetryConfigAdminService;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.onosproject.rest.resources.ResourceTest;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.ConfigType.GRPC;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.Status.ENABLED;

/**
 * Unit tests for openstack telemetry config REST API.
 */
public class OpenstackTelemetryConfigWebResourceTest extends ResourceTest {

    private static final String NAME = "grpc";

    private static final TelemetryConfig.ConfigType TYPE = GRPC;

    private static final String MANUFACTURER = "grpc.io";

    private static final String SW_VERSION = "1.0";

    private static final Map<String, String> PROP = Maps.newConcurrentMap();

    private static final String PROP_KEY_1 = "key11";
    private static final String PROP_KEY_2 = "key12";
    private static final String PROP_VALUE_1 = "value11";
    private static final String PROP_VALUE_2 = "value12";

    private static final TelemetryConfig.Status STATUS = ENABLED;

    private final TelemetryConfigAdminService mockConfigAdminService =
            createMock(TelemetryConfigAdminService.class);
    private static final String PATH = "config";

    /**
     * Constructs an openstack telemetry config resource test instance.
     */
    public OpenstackTelemetryConfigWebResourceTest() {
        super(ResourceConfig.forApplicationClass(OpenstackTelemetryWebApplication.class));
    }

    private TelemetryConfig telemetryConfig;

    /**
     * Sets up the global values for all the tests.
     */
    @Before
    public void setUpTest() {
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(TelemetryConfigAdminService.class,
                                mockConfigAdminService);
        setServiceDirectory(testDirectory);

        PROP.put(PROP_KEY_1, PROP_VALUE_1);
        PROP.put(PROP_KEY_2, PROP_VALUE_2);

        telemetryConfig = new DefaultTelemetryConfig(NAME, TYPE, null,
                MANUFACTURER, SW_VERSION, STATUS, PROP);
    }

    /**
     * Tests the results of the REST API PUT method by modifying config address.
     */
    @Test
    public void testUpdateConfigAddressWithModifyOperation() {
        expect(mockConfigAdminService.getConfig(anyString()))
                .andReturn(telemetryConfig).once();
        mockConfigAdminService.updateTelemetryConfig(telemetryConfig);
        replay(mockConfigAdminService);

        final WebTarget wt = target();
        Response response = wt.path(PATH + "/address/test1/address1")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(""));
        final int status = response.getStatus();

        assertEquals(200, status);

        verify(mockConfigAdminService);
    }

    /**
     * Tests the results of the REST API PUT method without modifying config address.
     */
    @Test
    public void testUpdateConfigAddressWithoutOperation() {
        expect(mockConfigAdminService.getConfig(anyString())).andReturn(null).once();
        replay(mockConfigAdminService);

        final WebTarget wt = target();
        Response response = wt.path(PATH + "/address/test1/address1")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(""));
        final int status = response.getStatus();

        assertEquals(304, status);

        verify(mockConfigAdminService);
    }

    /**
     * Tests the results of the REST API DELETE method by removing config.
     */
    @Test
    public void testDeleteConfigWithModifyOperation() {
        expect(mockConfigAdminService.getConfig(anyString()))
                .andReturn(telemetryConfig).once();
        mockConfigAdminService.removeTelemetryConfig(anyString());
        replay(mockConfigAdminService);

        final WebTarget wt = target();
        Response response = wt.path(PATH + "/test1")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .delete();
        final int status = response.getStatus();

        assertEquals(204, status);

        verify(mockConfigAdminService);
    }

    /**
     * Tests the results of the REST API DELETE method without removing config.
     */
    @Test
    public void testDeleteConfigWithoutModifyOperation() {
        expect(mockConfigAdminService.getConfig(anyString())).andReturn(null).once();
        replay(mockConfigAdminService);

        final WebTarget wt = target();
        Response response = wt.path(PATH + "/test1")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .delete();
        final int status = response.getStatus();

        assertEquals(304, status);

        verify(mockConfigAdminService);
    }

    /**
     * Tests the results of REST API PUT method with enabling the config.
     */
    @Test
    public void testEnableConfig() {
        expect(mockConfigAdminService.getConfig(anyString()))
                .andReturn(telemetryConfig).once();
        mockConfigAdminService.updateTelemetryConfig(telemetryConfig);
        replay(mockConfigAdminService);

        final WebTarget wt = target();
        Response response = wt.path(PATH + "/enable/test1")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(""));
        final int status = response.getStatus();

        assertEquals(200, status);

        verify(mockConfigAdminService);
    }

    /**
     * Tests the results of REST API PUT method with disabling the config.
     */
    @Test
    public void testDisableConfig() {
        expect(mockConfigAdminService.getConfig(anyString()))
                .andReturn(telemetryConfig).once();
        mockConfigAdminService.updateTelemetryConfig(telemetryConfig);
        replay(mockConfigAdminService);

        final WebTarget wt = target();
        Response response = wt.path(PATH + "/disable/test1")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(""));
        final int status = response.getStatus();

        assertEquals(200, status);

        verify(mockConfigAdminService);
    }
}
