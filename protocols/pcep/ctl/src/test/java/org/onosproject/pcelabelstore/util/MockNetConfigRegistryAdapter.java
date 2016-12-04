/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.pcelabelstore.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.SubjectFactory;
import org.onosproject.pcep.api.DeviceCapability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/* Mock test for network config registry. */
public class MockNetConfigRegistryAdapter implements NetworkConfigService, NetworkConfigRegistry {
    private ConfigFactory cfgFactory;
    private Map<DeviceId, DeviceCapability> classConfig = new HashMap<>();

    @Override
    public void registerConfigFactory(ConfigFactory configFactory) {
        cfgFactory = configFactory;
    }

    @Override
    public void unregisterConfigFactory(ConfigFactory configFactory) {
        cfgFactory = null;
    }

    @Override
    public <S, C extends Config<S>> C addConfig(S subject, Class<C> configClass) {
        if (configClass == DeviceCapability.class) {
            DeviceCapability devCap = new DeviceCapability();
            classConfig.put((DeviceId) subject, devCap);

            JsonNode node = new ObjectNode(new MockJsonNode());
            ObjectMapper mapper = new ObjectMapper();
            ConfigApplyDelegate delegate = new InternalApplyDelegate();
            devCap.init((DeviceId) subject, null, node, mapper, delegate);
            return (C) devCap;
        }

        return null;
    }

    @Override
    public <S, C extends Config<S>> void removeConfig(S subject, Class<C> configClass) {
        classConfig.remove(subject);
    }

    @Override
    public <S, C extends Config<S>> C getConfig(S subject, Class<C> configClass) {
        if (configClass == DeviceCapability.class) {
            return (C) classConfig.get(subject);
        }
        return null;
    }

    private class MockJsonNode extends JsonNodeFactory {
    }

    // Auxiliary delegate to receive notifications about changes applied to
    // the network configuration - by the apps.
    private class InternalApplyDelegate implements ConfigApplyDelegate {
        @Override
        public void onApply(Config config) {
            //configs.put(config.subject(), config.node());
        }
    }

    @Override
    public void addListener(NetworkConfigListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeListener(NetworkConfigListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<ConfigFactory> getConfigFactories() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S, C extends Config<S>> Set<ConfigFactory<S, C>> getConfigFactories(Class<S> subjectClass) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S, C extends Config<S>> ConfigFactory<S, C> getConfigFactory(Class<C> configClass) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<Class> getSubjectClasses() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SubjectFactory getSubjectFactory(String subjectClassKey) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SubjectFactory getSubjectFactory(Class subjectClass) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Class<? extends Config> getConfigClass(String subjectClassKey, String configKey) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S> Set<S> getSubjects(Class<S> subjectClass) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S, C extends Config<S>> Set<S> getSubjects(Class<S> subjectClass, Class<C> configClass) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S> Set<? extends Config<S>> getConfigs(S subject) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S, C extends Config<S>> C applyConfig(S subject, Class<C> configClass, JsonNode json) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S, C extends Config<S>> C applyConfig(String subjectClassKey, S subject, String configKey, JsonNode json) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S> void removeConfig(String subjectClassKey, S subject, String configKey) {
        // TODO Auto-generated method stub

    }

    @Override
    public <S> void removeConfig(S subject) {
        // TODO Auto-generated method stub

    }

    @Override
    public <S> void removeConfig() {
        // TODO Auto-generated method stub

    }
}
