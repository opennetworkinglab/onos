/*
 * Copyright 2015 Open Networking Laboratory
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
import org.onosproject.core.ApplicationId;

/**
 * Administrative interface for the intent domain service.
 */
@Beta
public interface IntentDomainAdminService extends IntentDomainService {

    /**
     * Register an application that provides intent domain service.
     *
     * @param applicationId application id
     * @param provider intent domain provider
     */
    void registerApplication(ApplicationId applicationId, IntentDomainProvider provider);

    /**
     * Unregisters an application that provides intent domain service.
     *
     * @param applicationId application id
     */
    void unregisterApplication(ApplicationId applicationId);

    /* TODO we may be able to accomplish the following through network config:
    void createDomain(String domainId);
    void removeDomain(String domainId);

    void addInternalDeviceToDomain(IntentDomain domain, DeviceId deviceId);
    void addPortToDomain(IntentDomain domain, ConnectPoint port);

    void bindApplicationToDomain(String domain, IntentDomain implementation);
    void unbindApplicationToDomain(String domain, IntentDomain implementation);
    */
}

