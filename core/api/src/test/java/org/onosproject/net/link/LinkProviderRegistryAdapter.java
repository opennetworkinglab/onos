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
package org.onosproject.net.link;

import java.util.Set;

import org.onosproject.net.provider.ProviderId;

import com.google.common.collect.ImmutableSet;

/**
 * Testing adapter for the LinkProviderRegistry API.
 */
public class LinkProviderRegistryAdapter implements LinkProviderRegistry {

    LinkProviderServiceAdapter providerService = null;

    @Override
    public LinkProviderService register(LinkProvider provider) {
        providerService = new LinkProviderServiceAdapter(provider);
        return providerService;
    }

    @Override
    public void unregister(LinkProvider provider) {
        if (providerService != null && provider.id().equals(providerService.provider().id())) {
            providerService = null;
        }
    }

    @Override
    public Set<ProviderId> getProviders() {
        if (providerService != null) {
            return ImmutableSet.of(providerService.provider().id());
        } else {
            return ImmutableSet.of();
        }
    }

    public LinkProviderServiceAdapter registeredProvider() {
        return providerService;
    }
}
