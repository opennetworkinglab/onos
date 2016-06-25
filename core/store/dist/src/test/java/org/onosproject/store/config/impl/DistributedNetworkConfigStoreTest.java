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

import java.util.Set;


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
     * Config class for testing.
     */
    public class BasicIntConfig extends Config<Integer> { }

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
     * Config factory class for testing.
     */
    public class MockIntConfigFactory extends ConfigFactory<Integer, BasicIntConfig> {
        protected MockIntConfigFactory(Class<BasicIntConfig> configClass, String configKey) {
            super(new MockIntSubjectFactory("strings"), configClass, configKey);
        }
        @Override
        public BasicIntConfig createConfig() {
            return new BasicIntConfig();
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
     * Subject factory class for testing.
     */
    public class MockIntSubjectFactory extends SubjectFactory<Integer> {
        protected MockIntSubjectFactory(String subjectClassKey) {
            super(Integer.class, subjectClassKey);
        }

        @Override
        public Integer createSubject(String subjectKey) {
            return Integer.parseInt(subjectKey);
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

        configStore.applyConfig("subject", BasicConfig.class, new ObjectMapper().createObjectNode());
        assertThat(configStore.getConfigClasses("subject"), hasSize(1));
        assertThat(configStore.getSubjects(String.class, BasicConfig.class), hasSize(1));
        assertThat(configStore.getSubjects(String.class), hasSize(1));
    }

    /**
     * Tests inserting a pending configuration.
     */
    @Test
    public void testPendingConfig() {
        configStore.queueConfig("subject", "config1", new ObjectMapper().createObjectNode());
        configStore.addConfigFactory(new MockConfigFactory(BasicConfig.class, "config1"));

        assertThat(configStore.getConfigClasses("subject"), hasSize(1));
        assertThat(configStore.getSubjects(String.class, BasicConfig.class), hasSize(1));
        assertThat(configStore.getSubjects(String.class), hasSize(1));
    }

    /**
     * Tests inserting a pending configuration for the same key, different subject.
     */
    @Test
    public void testPendingConfigSameKey() {
        configStore.queueConfig("subject", "config1", new ObjectMapper().createObjectNode());
        configStore.queueConfig(123, "config1", new ObjectMapper().createObjectNode());
        configStore.addConfigFactory(new MockConfigFactory(BasicConfig.class, "config1"));

        assertThat(configStore.getConfigClasses("subject"), hasSize(1));
        assertThat(configStore.getConfigClasses(123), hasSize(0));
        assertThat(configStore.getSubjects(String.class, BasicConfig.class), hasSize(1));
        assertThat(configStore.getSubjects(String.class), hasSize(1));

        configStore.addConfigFactory(new MockIntConfigFactory(BasicIntConfig.class, "config1"));

        assertThat(configStore.getConfigClasses("subject"), hasSize(1));
        assertThat(configStore.getConfigClasses(123), hasSize(1));
        assertThat(configStore.getSubjects(Integer.class, BasicIntConfig.class), hasSize(1));
        assertThat(configStore.getSubjects(Integer.class), hasSize(1));
    }

    /**
     * Tests  removal of config including queued.
     */
    @Test
    public void testRemoveConfig() {

        configStore.addConfigFactory(new MockConfigFactory(BasicConfig.class, "config1"));
        configStore.queueConfig("subject", "config2", new ObjectMapper().createObjectNode());
        configStore.queueConfig(123, "config2", new ObjectMapper().createObjectNode());
        configStore.applyConfig("subject1", BasicConfig.class, new ObjectMapper().createObjectNode());

        configStore.clearConfig();

        Set<String> subjects = configStore.getSubjects(String.class);
        assertThat(subjects.size(), is(0));

        configStore.addConfigFactory(new MockConfigFactory(BasicConfig.class, "config1"));
        configStore.queueConfig("subject", "config3", new ObjectMapper().createObjectNode());
        configStore.queueConfig(123, "config3", new ObjectMapper().createObjectNode());
        configStore.applyConfig("subject1", BasicConfig.class, new ObjectMapper().createObjectNode());

        Set<String> configs = configStore.getSubjects(String.class);

        configs.forEach(c -> configStore.clearConfig(c));
        Set<String> newConfig1 = configStore.getSubjects(String.class);

        assertThat(newConfig1, notNullValue());
    }
}
