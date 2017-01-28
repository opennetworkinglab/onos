/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.mapping;

import org.onosproject.net.provider.ProviderId;

import java.util.Set;

/**
 * Adapter for testing against mapping provider registry.
 */
public class MappingProviderRegistryAdapter implements MappingProviderRegistry {
    @Override
    public MappingProviderService register(MappingProvider provider) {
        return null;
    }

    @Override
    public void unregister(MappingProvider provider) {

    }

    @Override
    public Set<ProviderId> getProviders() {
        return null;
    }
}
