/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.net.domain;

import org.onosproject.net.DeviceId;

import java.util.Set;

/**
 * Service to access domain topology elements.
 */
public interface DomainService {

    /**
     * Returns the set of domains that have an associated topology.
     *
     * @return set of domain identifiers
     */
    Set<DomainId> getDomainIds();

    /**
     * Returns the set of the device ids of the specified domain.
     *
     * @param domainId domain identifier
     * @return set of device objects
     */
    Set<DeviceId> getDeviceIds(DomainId domainId);

    /**
     * Returns a domain given a device id.
     *
     * @param deviceId the device id
     * @return null or the domain id of a device
     */
    DomainId getDomain(DeviceId deviceId);

}

