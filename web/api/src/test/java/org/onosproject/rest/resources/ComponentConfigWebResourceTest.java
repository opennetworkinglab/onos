/*
 * Copyright 2016-present Open Networking Foundation
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

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cfg.ConfigProperty;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.onosproject.cfg.ConfigProperty.Type.INTEGER;
import static org.onosproject.cfg.ConfigProperty.Type.STRING;
import static org.onosproject.cfg.ConfigProperty.defineProperty;

/**
 * Test of the component config REST API.
 */
public class ComponentConfigWebResourceTest extends ResourceTest {

    private TestConfigManager service;

    @Before
    public void setUpMock() {
        service = new TestConfigManager();
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(ComponentConfigService.class, service);
        setServiceDirectory(testDirectory);
    }

    @Test
    public void getAllConfigs() {
        WebTarget wt = target();
        String response = wt.path("configuration").request().get(String.class);
        assertThat(response, containsString("\"foo\":"));
        assertThat(response, containsString("\"bar\":"));
    }

    @Test
    public void getConfigs() {
        WebTarget wt = target();
        String response = wt.path("configuration/foo").request().get(String.class);
        assertThat(response, containsString("{\"foo\":"));
        assertThat(response, not(containsString("{\"bar\":")));
    }

    @Test
    public void setConfigs() {
        WebTarget wt = target();
        try {
            wt.path("configuration/foo").request().post(
                    Entity.json("{ \"k\" : \"v\" }"), String.class);
        } catch (BadRequestException e) {
            assertEquals("incorrect key", "foo", service.component);
            assertEquals("incorrect key", "k", service.name);
            assertEquals("incorrect value", "v", service.value);
        }
    }

    @Test
    public void unsetConfigs() {
        WebTarget wt = target();
        try {
            // TODO: this needs to be revised later. Do you really need to
            // contain any entry inside delete request? Why not just use put then?
            wt.path("configuration/foo").request().delete();
        } catch (BadRequestException e) {
            assertEquals("incorrect key", "foo", service.component);
            assertEquals("incorrect key", "k", service.name);
            assertEquals("incorrect value", null, service.value);
        }
    }


    private class TestConfigManager extends ComponentConfigAdapter {

        private String component;
        private String name;
        private String value;

        @Override
        public Set<String> getComponentNames() {
            return ImmutableSet.of("foo", "bar");
        }

        @Override
        public void setProperty(String componentName, String name, String value) {
            this.component = componentName;
            this.name = name;
            this.value = value;
        }

        @Override
        public void unsetProperty(String componentName, String name) {
            this.component = componentName;
            this.name = name;
            this.value = null;
        }

        @Override
        public Set<ConfigProperty> getProperties(String componentName) {
            return ImmutableSet.of(defineProperty("k1", STRING, "d1", "dv1"),
                                   defineProperty("k2", INTEGER, "d2", "321"));
        }
    }
}