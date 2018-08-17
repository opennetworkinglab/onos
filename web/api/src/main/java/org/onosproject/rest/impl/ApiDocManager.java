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
package org.onosproject.rest.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.onosproject.rest.ApiDocProvider;
import org.onosproject.rest.ApiDocService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * Implementation of the REST API documentation tracker.
 */
@Component(immediate = true, service = ApiDocService.class)
public class ApiDocManager implements ApiDocService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // Set of doc providers
    private final Map<String, ApiDocProvider> providers = Maps.newConcurrentMap();

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void register(ApiDocProvider provider) {
        providers.put(provider.key(), provider);
        log.info("{} registered at {}", provider.name(), provider.key());
    }

    @Override
    public void unregister(ApiDocProvider provider) {
        providers.remove(provider.name());
        log.info("{} unregistered", provider.name());
    }

    @Override
    public Set<ApiDocProvider> getDocProviders() {
        return ImmutableSet.copyOf(providers.values());
    }

    @Override
    public ApiDocProvider getDocProvider(String key) {
        return providers.get(key);
    }
}
