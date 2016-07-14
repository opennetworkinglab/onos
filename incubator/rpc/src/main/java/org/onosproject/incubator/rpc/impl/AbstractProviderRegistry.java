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
package org.onosproject.incubator.rpc.impl;

import java.util.Map;
import java.util.Set;

import org.onosproject.incubator.rpc.RemoteServiceContextProvider;
import org.onosproject.incubator.rpc.RemoteServiceContextProviderService;
import org.onosproject.incubator.rpc.RemoteServiceProviderRegistry;
import org.onosproject.net.provider.ProviderId;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

// Probably should change name or add missing feature (provider from scheme) to
//  org.onosproject.net.provider.AbstractProviderRegistry<P, S>
@Beta
abstract class AbstractProviderRegistry
    implements RemoteServiceProviderRegistry {


    private final Map<ProviderId, RemoteServiceContextProvider> pidToProvider = Maps.newConcurrentMap();
    private final Map<String, RemoteServiceContextProvider> schemeToProvider = Maps.newConcurrentMap();

    public AbstractProviderRegistry() {
        super();
    }

    protected abstract RemoteServiceContextProviderService createProviderService(RemoteServiceContextProvider provider);

    @Override
    public synchronized RemoteServiceContextProviderService register(RemoteServiceContextProvider provider) {
        // TODO check if it already exists
        pidToProvider.put(provider.id(), provider);
        schemeToProvider.put(provider.id().scheme(), provider);
        return createProviderService(provider);
    }

    @Override
    public synchronized void unregister(RemoteServiceContextProvider provider) {
        pidToProvider.remove(provider.id(), provider);
        schemeToProvider.remove(provider.id().scheme(), provider);
    }

    @Override
    public Set<ProviderId> getProviders() {
        return ImmutableSet.copyOf(pidToProvider.keySet());
    }

    protected RemoteServiceContextProvider getProvider(ProviderId pid) {
        return pidToProvider.get(pid);
    }

    protected RemoteServiceContextProvider getProvider(String scheme) {
        return schemeToProvider.get(scheme);
    }

}
