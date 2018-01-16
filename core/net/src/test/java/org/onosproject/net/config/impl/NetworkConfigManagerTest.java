/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.config.impl;

import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ClusterServiceAdapter;
import org.onosproject.event.EventDeliveryServiceAdapter;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.SubjectFactory;
import org.onosproject.net.NetTestTools;
import org.onosproject.store.config.impl.DistributedNetworkConfigStore;
import org.onosproject.store.service.TestStorageService;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.testing.EqualsTester;

/**
 * Unit tests for network config registry.
 */
public class NetworkConfigManagerTest {

    private final ClusterService clusterService = new ClusterServiceAdapter();

    private NetworkConfigManager manager;
    private NetworkConfigRegistry registry;
    private NetworkConfigService configService;
    private DistributedNetworkConfigStore configStore;

    /**
     * Config classes for testing.
     */
    public class BasicConfig1 extends Config<String> { }
    public class BasicConfig2 extends Config<String> { }

    public class MockSubjectFactory extends SubjectFactory<String> {
        protected MockSubjectFactory(Class<String> subjectClass, String subjectKey) {
            super(subjectClass, subjectKey);
        }

        @Override
        public String createSubject(String subjectKey) {
            return subjectKey + "-subject";
        }
    }

    /**
     * Config factory classes for testing.
     */
    public class MockConfigFactory1 extends ConfigFactory<String, BasicConfig1> {
        protected MockConfigFactory1(SubjectFactory<String> subjectFactory,
                                    Class<BasicConfig1> configClass, String configKey) {
            super(subjectFactory, configClass, configKey);
        }
        @Override
        public BasicConfig1 createConfig() {
            return new BasicConfig1();
        }
    }

    public class MockConfigFactory2 extends ConfigFactory<String, BasicConfig2> {
        protected MockConfigFactory2(SubjectFactory<String> subjectFactory,
                                    Class<BasicConfig2> configClass, String configKey) {
            super(subjectFactory, configClass, configKey);
        }
        @Override
        public BasicConfig2 createConfig() {
            return new BasicConfig2();
        }
    }

    MockSubjectFactory factory1 = new MockSubjectFactory(String.class,
            "key1");
    MockSubjectFactory factory2 = new MockSubjectFactory(String.class,
            "key2");

    MockConfigFactory1 config1Factory = new MockConfigFactory1(factory1,
            BasicConfig1.class, "config1");
    MockConfigFactory2 config2Factory = new MockConfigFactory2(factory2,
            BasicConfig2.class, "config2");


    @Before
    public void setUp() throws Exception {
        configStore = new DistributedNetworkConfigStore();
        TestUtils.setField(configStore, "storageService", new TestStorageService());
        configStore.activate();
        manager = new NetworkConfigManager();
        manager.store = configStore;
        NetTestTools.injectEventDispatcher(manager, new EventDeliveryServiceAdapter());
        manager.clusterService = clusterService;
        manager.activate();
        registry = manager;
        configService = manager;
    }

    @After
    public void tearDown() {
        configStore.deactivate();
        manager.deactivate();
    }

    @Test
    public void testRegistry() {
        assertThat(registry.getConfigFactories(), hasSize(0));
        assertThat(registry.getConfigFactories(String.class), hasSize(0));
        assertThat(registry.getConfigFactory(BasicConfig1.class), nullValue());

        registry.registerConfigFactory(config1Factory);
        registry.registerConfigFactory(config2Factory);

        assertThat(registry.getConfigFactories(), hasSize(2));
        assertThat(registry.getConfigFactories(String.class), hasSize(2));

        ConfigFactory queried = registry.getConfigFactory(BasicConfig1.class);
        assertThat(queried, is(config1Factory));

        registry.unregisterConfigFactory(queried);
        //  Factory associations are not removed according to code documentation
        assertThat(registry.getConfigFactories(), hasSize(1));
        assertThat(registry.getConfigFactories(String.class), hasSize(1));
        assertThat(registry.getConfigFactory(BasicConfig1.class), nullValue());
    }

    @Test
    public void configIdEquals() {
        NetworkConfigManager.ConfigIdentifier id1 =
                new NetworkConfigManager.ConfigIdentifier("s1", "c1");
        NetworkConfigManager.ConfigIdentifier likeId1 =
                new NetworkConfigManager.ConfigIdentifier("s1", "c1");
        NetworkConfigManager.ConfigIdentifier id2 =
                new NetworkConfigManager.ConfigIdentifier("s1", "c2");
        NetworkConfigManager.ConfigIdentifier id3 =
                new NetworkConfigManager.ConfigIdentifier("s2", "c1");

        new EqualsTester().addEqualityGroup(id1, likeId1)
                .addEqualityGroup(id2)
                .addEqualityGroup(id3)
                .testEquals();
    }

    @Test
    public void configKeyEquals() {
        NetworkConfigManager.ConfigKey key1 =
                new NetworkConfigManager.ConfigKey(String.class, String.class);
        NetworkConfigManager.ConfigKey likeKey1 =
                new NetworkConfigManager.ConfigKey(String.class, String.class);
        NetworkConfigManager.ConfigKey key2 =
                new NetworkConfigManager.ConfigKey(String.class, Integer.class);
        NetworkConfigManager.ConfigKey key3 =
                new NetworkConfigManager.ConfigKey(Integer.class, String.class);

        new EqualsTester().addEqualityGroup(key1, likeKey1)
                .addEqualityGroup(key2)
                .addEqualityGroup(key3)
                .testEquals();
    }

    /**
     * Tests creation, query and removal of a factory.
     */
    @Test
    public void testAddConfig() {

        assertThat(configService.getSubjectFactory(String.class), nullValue());
        assertThat(configService.getSubjectFactory("key"), nullValue());

        registry.registerConfigFactory(config1Factory);
        registry.registerConfigFactory(config2Factory);
        configService.addConfig("configKey", BasicConfig1.class);

        Config newConfig = configService.getConfig("configKey", BasicConfig1.class);
        assertThat(newConfig, notNullValue());

        assertThat(configService.getSubjectFactory(String.class), notNullValue());
        assertThat(configService.getSubjectFactory("key1"), notNullValue());

        Set<Class> classes = configService.getSubjectClasses();
        assertThat(classes, hasSize(1));

        Set<String> subjectsForClass =
                configService.getSubjects(String.class);
        assertThat(subjectsForClass, hasSize(1));

        Set<String> subjectsForConfig =
                configService.getSubjects(String.class, BasicConfig1.class);
        assertThat(subjectsForConfig, hasSize(1));

        Class queriedConfigClass = configService.getConfigClass("key1", "config1");
        assertThat(queriedConfigClass == BasicConfig1.class, is(true));

        Set<? extends Config> configs = configService.getConfigs("configKey");
        assertThat(configs.size(), is(1));
        configs.forEach(c -> assertThat(c, instanceOf(BasicConfig1.class)));

        configService.removeConfig("configKey", BasicConfig1.class);
        Config newConfigAfterRemove = configService.getConfig("configKey", BasicConfig1.class);
        assertThat(newConfigAfterRemove, nullValue());
    }

    /**
     * Tests creation, query and removal of a factory.
     */
    @Test
    public void testApplyConfig() {

        assertThat(configService.getSubjectFactory(String.class), nullValue());
        assertThat(configService.getSubjectFactory("key"), nullValue());

        registry.registerConfigFactory(config1Factory);
        registry.registerConfigFactory(config2Factory);
        configService.applyConfig("configKey", BasicConfig1.class, new ObjectMapper().createObjectNode());

        Config newConfig = configService.getConfig("configKey", BasicConfig1.class);
        assertThat(newConfig, notNullValue());

        assertThat(configService.getSubjectFactory(String.class), notNullValue());
        assertThat(configService.getSubjectFactory("key1"), notNullValue());
    }

    /**
     * Tests creation, query and removal of a configuration including queued.
     */
    @Test
    public void testRemoveConfig() {

        assertThat(configService.getSubjectFactory(String.class), nullValue());
        assertThat(configService.getSubjectFactory("key"), nullValue());

        registry.registerConfigFactory(config1Factory);
        registry.registerConfigFactory(config2Factory);
        configService.applyConfig("configKey", BasicConfig1.class, new ObjectMapper().createObjectNode());

        configService.applyConfig("key1", "key", "config1", new ObjectMapper().createObjectNode());
        configService.applyConfig("key1", "keyxx", "config3", new ObjectMapper().createObjectNode());
        configService.applyConfig("key2", "key1", "config4", new ObjectMapper().createObjectNode());

        configService.removeConfig();

        Set<String> subjects = configService.getSubjects(factory1.subjectClass());
        assertThat(subjects.size(), is(0));

        Set<String> subjects2 = configService.getSubjects(factory2.subjectClass());
        assertThat(subjects2.size(), is(0));

        configService.applyConfig("key1", "key", "config1", new ObjectMapper().createObjectNode());
        configService.applyConfig("key1", "keyxx", "config3", new ObjectMapper().createObjectNode());
        configService.applyConfig("key1", "key1", "config4", new ObjectMapper().createObjectNode());

        @SuppressWarnings("unchecked")
        Set<String> configs = configService.getSubjects(
        configService.getSubjectFactory("key1").subjectClass());

        configs.forEach(c -> configService.removeConfig(c));
        Set<String> newConfig1 = configService.getSubjects(factory1.subjectClass());

        assertThat(newConfig1, notNullValue());
    }
}
