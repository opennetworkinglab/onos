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

import java.util.Set;

/**
 * Openstack telemetry service interface.
 */
public interface OpenstackTelemetryService {

    /**
     * Registers a new northbound telemetry service.
     *
     * @param telemetryService telemetry service
     */
    void addTelemetryService(TelemetryAdminService telemetryService);

    /**
     * Unregisters an existing northbound telemetry service.
     *
     * @param telemetryService telemetry service
     */
    void removeTelemetryService(TelemetryAdminService telemetryService);

    /**
     * Publishes new flow information to off-platform application through
     * various northbound interfaces.
     *
     * @param flowInfos virtual flow information
     */
    void publish(Set<FlowInfo> flowInfos);

    /**
     * Obtains a collection of openstack telemetry services.
     *
     * @return telemetry services
     */
    Set<TelemetryAdminService> telemetryServices();

    /**
     * Obtains a specific openstack telemetry service.
     *
     * @param type telemetry type
     * @return telemetry service instance
     */
    TelemetryAdminService telemetryService(String type);
}
