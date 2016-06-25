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
package org.onosproject.rest;

import com.google.common.annotations.Beta;

import java.util.Set;

/**
 * Service for registering REST API documentation resources.
 */
@Beta
public interface ApiDocService {

    /**
     * Registers the specified REST API documentation provider.
     *
     * @param provider REST API documentation provider
     */
    void register(ApiDocProvider provider);

    /**
     * Unregisters the specified REST API documentation provider.
     *
     * @param provider REST API documentation provider
     */
    void unregister(ApiDocProvider provider);

    /**
     * Returns the set of all registered REST API documentation providers.
     *
     * @return set of registered documentation providers
     */
    Set<ApiDocProvider> getDocProviders();

    /**
     * Returns the specified REST API documentation provider with the specified
     * key.
     *
     * @param key REST API key
     * @return documentation provider
     */
    ApiDocProvider getDocProvider(String key);

}
