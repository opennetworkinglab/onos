/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.provider;

import java.util.Set;

/**
 * Registry for tracking information providers with the core.
 *
 * @param <P> type of the information provider
 * @param <S> type of the provider service
 */
public interface ProviderRegistry<P extends Provider, S extends ProviderService<P>> {

    /**
     * Registers the supplied provider with the core.
     *
     * @param provider provider to be registered
     * @return provider service for injecting information into core
     * @throws java.lang.IllegalArgumentException if the provider is registered already
     */
    S register(P provider);

    /**
     * Unregisters the supplied provider. As a result the previously issued
     * provider service will be invalidated and any subsequent invocations
     * of its methods may throw {@link java.lang.IllegalStateException}.
     * <p>
     * Unregistering a provider that has not been previously registered results
     * in a no-op.
     * </p>
     *
     * @param provider provider to be unregistered
     */
    void unregister(P provider);

    /**
     * Returns a set of currently registered provider identities.
     *
     * @return set of provider identifiers
     */
    Set<ProviderId> getProviders();

}
