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
package org.onosproject.rest.resources;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.rest.BaseResource;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.Link;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.NetworkConfigServiceAdapter;
import org.onosproject.net.config.SubjectFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;

/**
 * Unit tests for network config web resource.
 */
public class NetworkConfigWebResourceTest extends ResourceTest {

    private MockNetworkConfigService mockNetworkConfigService;

    public class MockDeviceConfig extends Config<Device> {

        final String field1Value;
        final String field2Value;

        MockDeviceConfig(String value1, String value2) {
            field1Value = value1;
            field2Value = value2;
        }

        @Override
        public String key() {
            return "basic";
        }

        @Override
        public JsonNode node() {
            return new ObjectMapper()
                    .createObjectNode()
                    .put("field1", field1Value)
                    .put("field2", field2Value);
        }
    }

    /**
     * Mock config factory for devices.
     */
    private final SubjectFactory<Device> mockDevicesSubjectFactory =
            new SubjectFactory<Device>(Device.class, "devices") {
                @Override
                public Device createSubject(String subjectKey) {
                    DefaultDevice device = createMock(DefaultDevice.class);
                    replay(device);
                    return device;
                }

                @Override
                public Class<Device> subjectClass() {
                    return Device.class;
                }
            };

    /**
     * Mock config factory for links.
     */
    private final SubjectFactory<Link> mockLinksSubjectFactory =
            new SubjectFactory<Link>(Link.class, "links") {
                @Override
                public Link createSubject(String subjectKey) {
                    return null;
                }

                @Override
                public Class<Link> subjectClass() {
                    return Link.class;
                }
            };

    /**
     * Mocked config service.
     */
    class MockNetworkConfigService extends NetworkConfigServiceAdapter {

        Set devicesSubjects = new HashSet<>();
        Set devicesConfigs = new HashSet<>();
        Set linksSubjects = new HashSet();
        Set linksConfigs = new HashSet<>();

        @Override
        public Set<Class> getSubjectClasses() {
            return ImmutableSet.of(Device.class, Link.class);
        }

        @Override
        public SubjectFactory getSubjectFactory(Class subjectClass) {
            if (subjectClass == Device.class) {
                return mockDevicesSubjectFactory;
            } else if (subjectClass == Link.class) {
                return mockLinksSubjectFactory;
            }
            return null;
        }

        @Override
        public SubjectFactory getSubjectFactory(String subjectClassKey) {
            if ("devices".equals(subjectClassKey)) {
                return mockDevicesSubjectFactory;
            } else if ("links".equals(subjectClassKey)) {
                return mockLinksSubjectFactory;
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <S> Set<S> getSubjects(Class<S> subjectClass) {
            if (subjectClass == Device.class) {
                return devicesSubjects;
            } else if (subjectClass == Link.class) {
                return linksSubjects;
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <S> Set<? extends Config<S>> getConfigs(S subject) {
            if (subject instanceof Device || subject.toString().contains("device")) {
                return devicesConfigs;
            } else if (subject.toString().contains("link")) {
                return linksConfigs;
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <S, C extends Config<S>> C getConfig(S subject, Class<C> configClass) {

            if (configClass == MockDeviceConfig.class) {
                return (C) devicesConfigs.toArray()[0];
            }
            return null;
        }

        @Override
        public Class<? extends Config> getConfigClass(String subjectClassKey, String configKey) {
            return MockDeviceConfig.class;
        }
    }

    /**
     * Sets up mocked config service.
     */
    @Before
    public void setUpMocks() {
        mockNetworkConfigService = new MockNetworkConfigService();
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(NetworkConfigService.class, mockNetworkConfigService);
        BaseResource.setServiceDirectory(testDirectory);
    }

    /**
     * Sets up test config data.
     */
    @SuppressWarnings("unchecked")
    private void setUpConfigData() {
        mockNetworkConfigService.devicesSubjects.add("device1");
        mockNetworkConfigService.devicesConfigs.add(new MockDeviceConfig("v1", "v2"));
    }

    /**
     * Tests the result of the rest api GET when there are no configs.
     */
    @Test
    public void testEmptyConfigs() {
        final WebTarget wt = target();
        final String response = wt.path("network/configuration").request().get(String.class);

        assertThat(response, containsString("\"devices\":{}"));
        assertThat(response, containsString("\"links\":{}"));
    }

    /**
     * Tests the result of the rest api GET for a single subject with no configs.
     */
    @Test
    public void testEmptyConfig() {
        final WebTarget wt = target();
        final String response = wt.path("network/configuration/devices").request().get(String.class);

        assertThat(response, is("{}"));
    }

    /**
     * Tests the result of the rest api GET for a single subject that
     * is undefined.
     */
    @Test
    public void testNonExistentConfig() {
        final WebTarget wt = target();

        try {
            final String response = wt.path("network/configuration/nosuchkey").request().get(String.class);
            fail("GET of non-existent key does not produce an exception " + response);
        } catch (NotFoundException e) {
            assertThat(e.getResponse().getStatus(), is(HttpURLConnection.HTTP_NOT_FOUND));
        }
    }

    private void checkBasicAttributes(JsonValue basic) {
        Assert.assertThat(basic.asObject().get("field1").asString(), is("v1"));
        Assert.assertThat(basic.asObject().get("field2").asString(), is("v2"));
    }

    /**
     * Tests the result of the rest api GET when there is a config.
     */
    @Test
    public void testConfigs() {
        setUpConfigData();
        final WebTarget wt = target();
        final String response = wt.path("network/configuration").request().get(String.class);

        final JsonObject result = Json.parse(response).asObject();
        Assert.assertThat(result, notNullValue());

        Assert.assertThat(result.names(), hasSize(2));

        JsonValue devices = result.get("devices");
        Assert.assertThat(devices, notNullValue());

        JsonValue device1 = devices.asObject().get("device1");
        Assert.assertThat(device1, notNullValue());

        JsonValue basic = device1.asObject().get("basic");
        Assert.assertThat(basic, notNullValue());

        checkBasicAttributes(basic);
    }

    /**
     * Tests the result of the rest api single subject key GET when
     * there is a config.
     */
    @Test
    public void testSingleSubjectKeyConfig() {
        setUpConfigData();
        final WebTarget wt = target();
        final String response = wt.path("network/configuration/devices").request().get(String.class);

        final JsonObject result = Json.parse(response).asObject();
        Assert.assertThat(result, notNullValue());

        Assert.assertThat(result.names(), hasSize(1));

        JsonValue device1 = result.asObject().get("device1");
        Assert.assertThat(device1, notNullValue());

        JsonValue basic = device1.asObject().get("basic");
        Assert.assertThat(basic, notNullValue());

        checkBasicAttributes(basic);
    }

    /**
     * Tests the result of the rest api single subject GET when
     * there is a config.
     */
    @Test
    public void testSingleSubjectConfig() {
        setUpConfigData();
        final WebTarget wt = target();
        final String response =
                wt.path("network/configuration/devices/device1")
                        .request()
                        .get(String.class);

        final JsonObject result = Json.parse(response).asObject();
        Assert.assertThat(result, notNullValue());

        Assert.assertThat(result.names(), hasSize(1));

        JsonValue basic = result.asObject().get("basic");
        Assert.assertThat(basic, notNullValue());

        checkBasicAttributes(basic);
    }

    /**
     * Tests the result of the rest api single subject single config GET when
     * there is a config.
     */
    @Test
    public void testSingleSubjectSingleConfig() {
        setUpConfigData();
        final WebTarget wt = target();
        final String response =
                wt.path("network/configuration/devices/device1/basic")
                        .request()
                        .get(String.class);

        final JsonObject result = Json.parse(response).asObject();
        Assert.assertThat(result, notNullValue());

        Assert.assertThat(result.names(), hasSize(2));

        checkBasicAttributes(result);
    }

    /**
     * Tests network configuration with POST and illegal JSON.
     */
    @Test
    public void testBadPost() {
        String json = "this is invalid!";
        WebTarget wt = target();

        Response response = wt.path("network/configuration")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(json));
        Assert.assertThat(response.getStatus(), is(HttpURLConnection.HTTP_BAD_REQUEST));
    }

    /**
     * Tests creating a network config with POST.
     */
    @Test
    public void testPost() {
        InputStream jsonStream = IntentsResourceTest.class
                .getResourceAsStream("post-config.json");
        WebTarget wt = target();

        Response response = wt.path("network/configuration")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        Assert.assertThat(response.getStatus(), is(HttpURLConnection.HTTP_OK));
    }

    /**
     * Tests creating a network config with POST of valid but incorrect JSON.
     */
    @Test
    public void testBadSyntaxPost() {
        InputStream jsonStream = IntentsResourceTest.class
                .getResourceAsStream("post-config-bad-syntax.json");
        WebTarget wt = target();

        Response response = wt.path("network/configuration")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        Assert.assertThat(response.getStatus(), is(HttpStatus.MULTI_STATUS_207));
    }

    // TODO: Add test for DELETE
}
