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

package org.onosproject.cfg.impl;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import static com.google.common.io.ByteStreams.toByteArray;
import static com.google.common.io.Files.write;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * UnitTest for ComponentLoader.
 */
public class ComponentConfigLoaderTest {

    @ClassRule
    public static TemporaryFolder testFolder = new TemporaryFolder();

    private static final String FOO_COMPONENT = "fooComponent";

    private ComponentConfigLoader loader;

    private TestConfigService service;

    private final Logger log = getLogger(getClass());

    /*
     * Method to SetUp the test environment with test file, a config loader a service,
     * and assign it to the loader.configService for the test.
     */
    @Before
    public void setUp() throws IOException {
        ComponentConfigLoader.cfgFile = new File(testFolder.newFolder(), "test.json");
        loader = new ComponentConfigLoader();
        service = new TestConfigService();
        loader.configService = service;
    }

    /*
     * Tests that the component in the json receives the correct configuration.
     */
    @Test
    public void basics() throws IOException {
        stageTestResource("basic.json");
        loader.activate();
        assertEquals("incorrect component", FOO_COMPONENT, service.component);
    }

    /*
     * Tests that the component is null if the file has a bad configuration format
     * for which it yielded an exception. Can't test the exception because it happens
     * in a different thread.
     */
    @Test
    public void badConfig() throws IOException {
        stageTestResource("badConfig.json");
        loader.activate();
        assertNull("incorrect configuration", service.component);

    }

    /*
     * Writes the necessary file for the tests in the temporary directory
     */
    static void stageTestResource(String name) throws IOException {
        byte[] bytes = toByteArray(ComponentConfigLoaderTest.class.getResourceAsStream(name));
        write(bytes, ComponentConfigLoader.cfgFile);
    }

    /*
     * Mockup class for the config service.
     */
    private class TestConfigService extends ComponentConfigAdapter {

        protected String component;
        protected String name;
        protected String value;

        @Override
        public Set<String> getComponentNames() {
            return ImmutableSet.of(FOO_COMPONENT);
        }

        @Override
        public void preSetProperty(String componentName, String name, String value) {
            log.info("preSet");
            this.component = componentName;
            this.name = name;
            this.value = value;

        }

        @Override
        public void setProperty(String componentName, String name, String value) {
            log.info("Set");
            this.component = componentName;
            this.name = name;
            this.value = value;

        }
    }
}