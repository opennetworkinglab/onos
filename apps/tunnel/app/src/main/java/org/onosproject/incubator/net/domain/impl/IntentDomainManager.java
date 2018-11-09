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
package org.onosproject.incubator.net.domain.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.onosproject.incubator.net.domain.IntentDomain;
import org.onosproject.incubator.net.domain.IntentDomainEvent;
import org.onosproject.incubator.net.domain.IntentDomainId;
import org.onosproject.incubator.net.domain.IntentDomainListener;
import org.onosproject.incubator.net.domain.IntentDomainProvider;
import org.onosproject.incubator.net.domain.IntentDomainProviderRegistry;
import org.onosproject.incubator.net.domain.IntentDomainProviderService;
import org.onosproject.incubator.net.domain.IntentDomainService;
import org.onosproject.incubator.net.domain.IntentPrimitive;
import org.onosproject.incubator.net.domain.IntentResource;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.AbstractListenerProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Implementation of the intent domain service.
 */
@Component(immediate = true, service = {IntentDomainService.class, IntentDomainProviderRegistry.class})
public class IntentDomainManager
        extends AbstractListenerProviderRegistry<IntentDomainEvent, IntentDomainListener,
                    IntentDomainProvider, IntentDomainProviderService>
        implements IntentDomainService, IntentDomainProviderRegistry {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ConcurrentMap<IntentDomainId, IntentDomain> domains = Maps.newConcurrentMap();

    @Activate
    protected void activate() {
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public IntentDomain getDomain(IntentDomainId id) {
        return domains.get(id);
    }

    @Override
    public Set<IntentDomain> getDomains() {
        return ImmutableSet.copyOf(domains.values());
    }

    @Override
    public Set<IntentDomain> getDomains(DeviceId deviceId) {
        return domains.values().stream()
                .filter(domain ->
                                domain.internalDevices().contains(deviceId) ||
                                        domain.edgePorts().stream()
                                                .map(ConnectPoint::deviceId)
                                                .anyMatch(d -> d.equals(deviceId)))
                .collect(Collectors.toSet());
    }

    @Override
    public List<IntentResource> request(IntentDomainId domainId, IntentPrimitive primitive) {
        IntentDomain domain = getDomain(domainId);
        return domain.provider().request(domain, primitive);
    }

    @Override
    public void submit(IntentDomainId domainId, IntentResource resource) {
        getDomain(domainId).provider().apply(resource);
    }

    @Override
    protected IntentDomainProviderService createProviderService(IntentDomainProvider provider) {
        return new InternalDomainProviderService(provider);
    }

    private class InternalDomainProviderService
            extends AbstractProviderService<IntentDomainProvider>
            implements IntentDomainProviderService {

        InternalDomainProviderService(IntentDomainProvider provider) {
            super(provider);
        }
    }
}
