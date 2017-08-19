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

import com.google.common.collect.Sets;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.NetworkConfigRegistryAdapter;

import java.util.Set;

/**
 * Mock Network Config Registry.
 */
class MockNetworkConfigRegistry extends NetworkConfigRegistryAdapter {
    private Set<Config> configs = Sets.newHashSet();

    public void applyConfig(Config config) {
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
}