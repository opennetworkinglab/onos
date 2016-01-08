/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.store.config.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.SubjectFactory;
import org.onosproject.store.service.TestStorageService;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class DistributedNetworkConfigStoreTest {
    private DistributedNetworkConfigStore configStore;

    /**
     * Sets up the config store and the storage service test harness.
     */
    @Before
    public void setUp() {
        configStore = new DistributedNetworkConfigStore();
        configStore.storageService = new TestStorageService();
        configStore.setDelegate(event -> { });
        configStore.activate();
    }

    /**
     * Tears down the config store.
     */
    @After
    public void tearDown() {
        configStore.deactivate();
    }

    /**
     * Config class for testing.
     */
    public class BasicConfig extends Config<String> { }

    /**
     * Config factory class for testing.
     */
    public class MockConfigFactory extends ConfigFactory<String, BasicConfig> {
        protected MockConfigFactory(Class<BasicConfig> configClass, String configKey) {
            super(new MockSubjectFactory("strings"), configClass, configKey);
        }
        @Override
        public BasicConfig createConfig() {
            return new BasicConfig();
        }
    }

    /**
     * Subject factory class for testing.
     */
    public class MockSubjectFactory extends SubjectFactory<String> {
        protected MockSubjectFactory(String subjectClassKey) {
            super(String.class, subjectClassKey);
        }

        @Override
        public String createSubject(String subjectKey) {
            return subjectKey;
        }
    }

    /**
     * Tests creation, query and removal of a config.
     */
    @Test
    public void testCreateConfig() {
        configStore.addConfigFactory(new MockConfigFactory(BasicConfig.class, "config1"));

        configStore.createConfig("config1", BasicConfig.class);
        assertThat(configStore.getConfigClasses("config1"), hasSize(1));
        assertThat(configStore.getSubjects(String.class, BasicConfig.class), hasSize(1));
        assertThat(configStore.getSubjects(String.class), hasSize(1));

        BasicConfig queried = configStore.getConfig("config1", BasicConfig.class);
        assertThat(queried, notNullValue());

        configStore.clearConfig("config1", BasicConfig.class);
        assertThat(configStore.getConfigClasses("config1"), hasSize(0));
        assertThat(configStore.getSubjects(String.class, BasicConfig.class), hasSize(0));
        assertThat(configStore.getSubjects(String.class), hasSize(0));

        BasicConfig queriedAfterClear = configStore.getConfig("config1", BasicConfig.class);
        assertThat(queriedAfterClear, nullValue());
    }

    /**
     * Tests creation, query and removal of a factory.
     */
    @Test
    public void testCreateFactory() {
        MockConfigFactory mockFactory = new MockConfigFactory(BasicConfig.class, "config1");

        assertThat(configStore.getConfigFactory(BasicConfig.class), nullValue());

        configStore.addConfigFactory(mockFactory);
        assertThat(configStore.getConfigFactory(BasicConfig.class), is(mockFactory));

        configStore.removeConfigFactory(mockFactory);
        assertThat(configStore.getConfigFactory(BasicConfig.class), nullValue());
    }

    /**
     * Tests applying a config.
     */
    @Test
    public void testApplyConfig() {
        configStore.addConfigFactory(new MockConfigFactory(BasicConfig.class, "config1"));

        configStore.applyConfig("config1", BasicConfig.class, new ObjectMapper().createObjectNode());
        assertThat(configStore.getConfigClasses("config1"), hasSize(1));
        assertThat(configStore.getSubjects(String.class, BasicConfig.class), hasSize(1));
        assertThat(configStore.getSubjects(String.class), hasSize(1));
    }
}
