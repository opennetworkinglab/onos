/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.segmentrouting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.NetworkConfigRegistryAdapter;
import org.onosproject.net.config.basics.BasicDeviceConfig;

import java.util.Objects;
import java.util.Set;

/**
 * Mock Network Config Registry.
 */
class MockNetworkConfigRegistry extends NetworkConfigRegistryAdapter {
    private Set<Config> configs = Sets.newHashSet();

    void applyConfig(Config config) {
        configs.add(config);
    }

    @Override
    public <S, C extends Config<S>> C getConfig(S subject, Class<C> configClass) {
        Config c = configs.stream()
                .filter(config -> subject.equals(config.subject()))
                .filter(config -> configClass.equals(config.getClass()))
                .findFirst().orElse(null);
        return (C) c;
    }

    @Override
    public <S, C extends Config<S>> C addConfig(S subject, Class<C> configClass) {
        Config c = configs.stream()
                .filter(config -> subject.equals(config.subject()))
                .filter(config -> configClass.equals(config.getClass()))
                .findFirst().orElseGet(() -> {
                    if (configClass.equals(BasicDeviceConfig.class)) {
                        BasicDeviceConfig deviceConfig = new BasicDeviceConfig();
                        ObjectMapper mapper = new ObjectMapper();
                        deviceConfig.init((DeviceId) subject, ((DeviceId) subject).toString(),
                                          JsonNodeFactory.instance.objectNode(), mapper, config -> {
                                });
                        return deviceConfig;
                    }
                    return null;
                });
        return (C) c;
    }

    @Override
    public <S, C extends Config<S>> Set<S> getSubjects(Class<S> subject, Class<C> configClass) {
        ImmutableSet.Builder<S> builder = ImmutableSet.builder();
        String cName = configClass.getName();
        configs.forEach(k -> {
            if (subject.isInstance(k.subject()) && Objects.equals(cName, k.getClass().getName())) {
                builder.add((S) k.subject());
            }
        });
        return builder.build();
    }
}