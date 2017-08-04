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
package org.onosproject.net.config;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * Test adapter for network configuration service registry.
 */
public class NetworkConfigRegistryAdapter extends NetworkConfigServiceAdapter implements NetworkConfigRegistry {

    public void registerConfigFactory(ConfigFactory configFactory) {
    }

    public void unregisterConfigFactory(ConfigFactory configFactory) {
    }

    public Set<ConfigFactory> getConfigFactories() {
        return ImmutableSet.of();
    }

    public <S, C extends Config<S>> Set<ConfigFactory<S, C>> getConfigFactories(Class<S> subjectClass) {
        return ImmutableSet.of();
    }

    public <S, C extends Config<S>> ConfigFactory<S, C> getConfigFactory(Class<C> configClass) {
        return null;
    }
}
