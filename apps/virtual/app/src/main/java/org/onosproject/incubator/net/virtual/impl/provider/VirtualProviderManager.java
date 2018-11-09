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

package org.onosproject.incubator.net.virtual.impl.provider;

import com.google.common.collect.ImmutableSet;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.provider.VirtualProvider;
import org.onosproject.incubator.net.virtual.provider.VirtualProviderRegistryService;
import org.onosproject.incubator.net.virtual.provider.VirtualProviderService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.ProviderId;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Implementation of the virtual provider registry and providerService registry service.
 */
@Component(service = VirtualProviderRegistryService.class)
public class VirtualProviderManager implements VirtualProviderRegistryService {

    private final Map<ProviderId, VirtualProvider> providers = new HashMap<>();
    private final Map<ProviderId, VirtualProviderService> servicesWithProvider = new HashMap<>();
    private final Map<String, VirtualProvider> providersByScheme = new HashMap<>();
    private final Map<NetworkId, Set<VirtualProviderService>> servicesByNetwork = new HashMap<>();
    private static Logger log = LoggerFactory.getLogger(VirtualProviderManager.class);

    @Override
    public synchronized void registerProvider(VirtualProvider virtualProvider) {
        checkNotNull(virtualProvider, "Provider cannot be null");
        checkState(!providers.containsKey(virtualProvider.id()),
                   "Provider %s already registered", virtualProvider.id());

        // If the provider is a primary one, check for a conflict.
        ProviderId pid = virtualProvider.id();
        checkState(pid.isAncillary() || !providersByScheme.containsKey(pid.scheme()),
                   "A primary provider with id %s is already registered",
                   providersByScheme.get(pid.scheme()));

        providers.put(virtualProvider.id(), virtualProvider);

        // Register the provider by URI scheme only if it is not ancillary.
        if (!pid.isAncillary()) {
            providersByScheme.put(pid.scheme(), virtualProvider);
        }
    }

    @Override
    public synchronized void unregisterProvider(VirtualProvider virtualProvider) {
        checkNotNull(virtualProvider, "Provider cannot be null");

        //TODO: invalidate provider services which subscribe the provider
        providers.remove(virtualProvider.id());

        if (!virtualProvider.id().isAncillary()) {
            providersByScheme.remove(virtualProvider.id().scheme());
        }
    }

    @Override
    public synchronized void
    registerProviderService(NetworkId networkId,
                            VirtualProviderService virtualProviderService) {
        Set<VirtualProviderService> services =
                servicesByNetwork.computeIfAbsent(networkId, k -> new HashSet<>());

        services.add(virtualProviderService);
    }

    @Override
    public synchronized void
    unregisterProviderService(NetworkId networkId,
                              VirtualProviderService virtualProviderService) {
        Set<VirtualProviderService> services = servicesByNetwork.get(networkId);

        if (services != null) {
            services.remove(virtualProviderService);
        }
    }

    @Override
    public synchronized Set<ProviderId> getProviders() {
        return ImmutableSet.copyOf(providers.keySet());
    }

    @Override
    public Set<ProviderId> getProvidersByService(VirtualProviderService
                                                             virtualProviderService) {
        Class clazz = getProviderClass(virtualProviderService);

        return ImmutableSet.copyOf(providers.values().stream()
                                           .filter(clazz::isInstance)
                                           .map(VirtualProvider::id)
                                           .collect(Collectors.toSet()));
    }

    @Override
    public synchronized VirtualProvider getProvider(ProviderId providerId) {
        return providers.get(providerId);
    }

    @Override
    public synchronized VirtualProvider getProvider(DeviceId deviceId) {
        return providersByScheme.get(deviceId.uri().getScheme());
    }

    @Override
    public synchronized VirtualProvider getProvider(String scheme) {
        return providersByScheme.get(scheme);
    }

    @Override
    public synchronized VirtualProviderService
    getProviderService(NetworkId networkId, Class<? extends VirtualProvider> providerClass) {
        Set<VirtualProviderService> services = servicesByNetwork.get(networkId);

        if (services == null) {
            return null;
        }

        return services.stream()
                .filter(s -> getProviderClass(s).equals(providerClass))
                .findFirst().orElse(null);
    }

    /**
     * Returns the class type of parameter type.
     * More specifically, it returns the class type of provider service's provider type.
     *
     * @param service a virtual provider service
     * @return the class type of provider service of the service
     */
    private Class getProviderClass(VirtualProviderService service) {
       String className = service.getClass().getGenericSuperclass().getTypeName();
       String pramType = className.split("<")[1].split(">")[0];

        try {
            return Class.forName(pramType);
        } catch (ClassNotFoundException e) {
            log.warn("getProviderClass()", e);
        }

        return null;
    }
}
