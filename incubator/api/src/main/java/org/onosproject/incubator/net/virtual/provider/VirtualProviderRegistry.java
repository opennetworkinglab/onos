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

package org.onosproject.incubator.net.virtual.provider;

import java.util.Set;

/** Registry for tracking information virtual providers with the core.
 * @param <P> type of the information virtual provider
 * @param <S> type of the provider virtual service
 */

public interface VirtualProviderRegistry<P extends VirtualProvider,
            S extends VirtualProviderService<P>> {

    /**
     * Registers the supplied virtual provider with the virtual core.
     *
     * @param provider virtual provider to be registered
     * @return service for injecting information into core
     * @throws java.lang.IllegalArgumentException if the provider is registered already
     */
    S register(P provider);

    /**
     * Unregisters the supplied virtual provider.
     * As a result the previously issued virtual provider service
     * will be invalidated and any subsequent invocations
     * of its methods may throw {@link java.lang.IllegalStateException}.
     * Unregistering a virtual provider that has not been previously registered
     * result in a no-op.
     *
     * @param provider provider to be unregistered
     */
    void unregister(P provider);

    /**
     * Returns a set of currently registered virtual providers.
     *
     * @return set of virtual providers
     */
    Set<P> getProviders();
}
