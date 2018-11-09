/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.incubator.net.domain;

import com.google.common.annotations.Beta;
import org.onosproject.event.ListenerService;
import org.onosproject.net.DeviceId;

import java.util.List;
import java.util.Set;

/**
 * Service for that maintains a graph of intent domains and a registry of intent
 * domain providers.
 */
@Beta
public interface IntentDomainService
        extends ListenerService<IntentDomainEvent, IntentDomainListener> {

    /**
     * Returns the intent domain for the given id.
     *
     * @param id id to look up
     * @return the intent domain; null if none found
     */
    IntentDomain getDomain(IntentDomainId id);

    /**
     * Returns a set of all intent domains.
     *
     * @return set of intent domains
     */
    Set<IntentDomain> getDomains();

    /**
     * Returns any network domains associated with the given device id.
     *
     * @param deviceId device id to look up
     * @return set of intent domain
     */
    Set<IntentDomain> getDomains(DeviceId deviceId);

    /**
     * Requests an intent primitive from the intent domain.
     *
     * @param domainId id of target domain
     * @param primitive intent primitive
     * @return set of intent resources that satisfy the primitive
     */
    List<IntentResource> request(IntentDomainId domainId, IntentPrimitive primitive);

    /**
     * Submits an intent resource to the intent domain for installation.
     *
     * @param domainId id of target domain
     * @param resource intent resource
     */
    void submit(IntentDomainId domainId, IntentResource resource);
}





