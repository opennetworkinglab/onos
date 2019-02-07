/*
 * Copyright 2014-present Open Networking Foundation
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

import com.google.common.collect.ImmutableSet;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Base implementation of provider registry.
 *
 * @param <P> type of the information provider
 * @param <S> type of the provider service
 */
public abstract class AbstractProviderRegistry<P extends Provider, S extends ProviderService<P>>
        implements ProviderRegistry<P, S> {

    private final Map<ProviderId, P> providers = new HashMap<>();
    private final Map<ProviderId, S> services = new HashMap<>();
    private final Map<String, P> providersByScheme = new HashMap<>();

    /**
     * Creates a new provider service bound to the specified provider.
     *
     * @param provider provider
     * @return provider service
     */
    protected abstract S createProviderService(P provider);

    /**
     * Returns the default fall-back provider. Default implementation return null.
     *
     * @return default provider
     */
    protected P defaultProvider() {
        return null;
    }

    private static final Logger log = LoggerFactory
        .getLogger(AbstractProviderRegistry.class);

    @Override
    public synchronized S register(P provider) {

        log.debug("registering provider {} :{}", provider, provider.id());
        checkNotNull(provider, "Provider cannot be null");

        // If the provider is a primary one, check for a conflict.
        ProviderId pid = provider.id();
        checkState(pid.isAncillary() || !providersByScheme.containsKey(pid.scheme()),
            "A primary provider with id %s is already registered",
            providersByScheme.get(pid.scheme()));

        checkState(pid.isAncillary() || !services.containsKey(provider.id()),
            "Provider %s already registered", provider.id());

        // Register the provider by URI scheme only if it is not ancillary.
        S service = null;

        if (!pid.isAncillary()) {
            service = createProviderService(provider);
            services.put(provider.id(), service);
            providers.put(provider.id(), provider);
            providersByScheme.put(pid.scheme(), provider);
        } else {
            ProviderId servicePid = new ProviderId(provider.id().scheme(), provider.id().id(), false);
            service = services.get(servicePid);
            checkState(service != null,
                "Primary provider with id %s not registered yet", pid);
        }

        return service;
    }

    @Override
    public synchronized void unregister(P provider) {
        checkNotNull(provider, "Provider cannot be null");
        S service = services.get(provider.id());
        if (service instanceof AbstractProviderService) {
            ((AbstractProviderService) service).invalidate();
            services.remove(provider.id());
            providers.remove(provider.id());
            if (!provider.id().isAncillary()) {
                providersByScheme.remove(provider.id().scheme());
            }
        }
    }

    @Override
    public synchronized Set<ProviderId> getProviders() {
        return ImmutableSet.copyOf(services.keySet());
    }

    /**
     * Returns the provider registered with the specified provider ID or null
     * if none is found for the given provider family and default fall-back is
     * not supported.
     *
     * @param providerId provider identifier
     * @return provider
     */
    protected synchronized P getProvider(ProviderId providerId) {
        P provider = providers.get(providerId);
        return provider != null ? provider : defaultProvider();
    }

    /**
     * Returns the provider for the specified device ID based on URI scheme.
     *
     * @param deviceId device identifier
     * @return provider bound to the URI scheme
     */
    protected synchronized P getProvider(DeviceId deviceId) {
        P provider = providersByScheme.get(deviceId.uri().getScheme());
        return provider != null ? provider : defaultProvider();
    }

    /**
     * Returns the provider registered with the specified scheme.
     *
     * @param scheme provider scheme
     * @return provider
     */
    protected synchronized P getProvider(String scheme) {
        return providersByScheme.get(scheme);
    }

}
