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
package org.onosproject.xosclient.api;

import java.util.Set;

/**
 * Service for interacting with XOS VTN service and service dependency.
 */
public interface VtnServiceApi {

    // TODO move network type to VtnNetwork later
    enum NetworkType {
        PRIVATE,
        PUBLIC,
        MANAGEMENT_HOSTS,
        MANAGEMENT_LOCAL
    }

    enum ServiceType {
        VSG,
        ACCESS_AGENT,
        MANAGEMENT,
        DEFAULT
    }

    /**
     * Returns all services list.
     *
     * @return service list
     */
    Set<VtnServiceId> services();

    /**
     * Returns VTN service.
     *
     * @param serviceId service id
     * @return vtn service
     */
    VtnService service(VtnServiceId serviceId);

    /**
     * Returns dependent tenant services of a given provider service.
     *
     * @param pServiceId vtn service id
     * @return set of service ids
     */
    Set<VtnServiceId> tenantServices(VtnServiceId pServiceId);

    /**
     * Returns dependent provider services of a given tenant service.
     *
     * @param tServiceId vtn service id
     * @return set of service ids
     */
    Set<VtnServiceId> providerServices(VtnServiceId tServiceId);

    /**
     * Returns VTN service from OpenStack.
     *
     * @param serviceId service id
     * @param osAccess openstack access
     * @return vtn service
     */
    // TODO remove this when XOS provides service information
    VtnService service(VtnServiceId serviceId, OpenStackAccess osAccess);
}
