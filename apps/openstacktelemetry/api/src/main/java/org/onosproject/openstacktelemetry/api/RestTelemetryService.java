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
package org.onosproject.openstacktelemetry.api;

import javax.ws.rs.core.Response;
import java.util.Set;

/**
 * Service API for publishing openstack telemetry through REST producer.
 */
public interface RestTelemetryService extends TelemetryService {

    /**
     * Publishes openstack telemetry to REST server.
     * By default, the client sends record using the HTTP method configured in
     * RestTelemetryConfig.
     *
     * @param record network metrics
     * @return response from REST server
     */
    Set<Response> publish(String record);

    /**
     * Returns REST telemetry service type.
     *
     * @return REST telemetry service type
     */
    @Override
    default ServiceType type() {
        return ServiceType.REST;
    }
}
