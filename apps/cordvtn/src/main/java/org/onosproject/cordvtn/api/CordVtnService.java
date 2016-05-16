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
package org.onosproject.cordvtn.api;

import org.onosproject.xosclient.api.VtnServiceId;

/**
 * Service for provisioning overlay virtual networks on compute nodes.
 */
public interface CordVtnService {

    String CORDVTN_APP_ID = "org.onosproject.cordvtn";

    /**
     * Creates dependencies for a given tenant service.
     *
     * @param tServiceId id of the service which has a dependency
     * @param pServiceId id of the service which provide dependency
     * @param isBidirectional true to enable bidirectional connectivity between two services
     */
    void createServiceDependency(VtnServiceId tServiceId, VtnServiceId pServiceId,
                                 boolean isBidirectional);

    /**
     * Removes all dependencies from a given tenant service.
     *
     * @param tServiceId id of the service which has a dependency
     * @param pServiceId id of the service which provide dependency
     */
    void removeServiceDependency(VtnServiceId tServiceId, VtnServiceId pServiceId);
}
