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

package org.onosproject.incubator.net.virtual.provider;

import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.ProviderId;

import java.util.Set;

/**
 * Registry for tracking information providers with the core.
 */
public interface VirtualProviderRegistryService {

    /**
     * Registers the supplied virtual provider.
     *
     * @param virtualProvider a virtual provider to be registered
     * @throws java.lang.IllegalArgumentException if the provider is registered already
     */
    void registerProvider(VirtualProvider virtualProvider);

    /**
     * Unregisters the supplied virtual provider.
     * As a result the previously issued virtual provider service will be invalidated
     * and any subsequent invocations of its methods may throw
     * {@link java.lang.IllegalStateException}.
     * <p>
     * Unregistering a virtual provider that has not been previously registered results
     * in a no-op.
     * </p>
     *
     * @param virtualProvider a virtual provider to be unregistered
     */
    void unregisterProvider(VirtualProvider virtualProvider);

    /**
     * Registers the supplied virtual provider.
     *
     * @param networkId a virtual network identifier
     * @param virtualProviderService a virtual provider service to be registered
     */
    void registerProviderService(NetworkId networkId,
                                 VirtualProviderService virtualProviderService);

    /**
     * Unregisters the supplied virtual provider service.
     *
     * @param networkId a virtual network identifier
     * @param virtualProviderService a virtual provider service to be unregistered
     */
    void unregisterProviderService(NetworkId networkId,
                                   VirtualProviderService virtualProviderService);

    /**
     * Returns a set of currently registered virtual provider identities.
     *
     * @return set of virtual provider identifiers
     */
    Set<ProviderId> getProviders();

    /**
     * Returns a set of currently registered virtual provider identities
     * corresponding to the requested providerService.
     *
     * @param virtualProviderService a virtual provider service
     * @return set of virtual provider identifiers
     */
    Set<ProviderId> getProvidersByService(VirtualProviderService virtualProviderService);

    /**
     * Returns the virtual provider registered with the specified provider ID or null
     * if none is found for the given provider family and default fall-back is
     * not supported.
     *
     * @param providerId provider identifier
     * @return provider
     */
    VirtualProvider getProvider(ProviderId providerId);

    /**
     * Returns the virtual provider for the specified device ID based on URI scheme.
     *
     * @param deviceId virtual device identifier
     * @return provider bound to the URI scheme
     */
    VirtualProvider getProvider(DeviceId deviceId);

    /**
     * Returns the virtual provider registered with the specified scheme.
     *
     * @param scheme provider scheme
     * @return provider
     */
    VirtualProvider getProvider(String scheme);

    /**
     * Returns a virtual provider service corresponding to
     * the virtual network and provider class type.
     *
     * @param networkId a virtual network identifier
     * @param providerClass a type of virtual provider
     * @return a virtual provider service
     */
    VirtualProviderService getProviderService(NetworkId networkId,
                                              Class<? extends VirtualProvider> providerClass);
}
