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

/**
 * Admin service API for publishing openstack telemetry.
 */
public interface TelemetryAdminService extends TelemetryService {

    /**
     * Prepares and launches the telemetry producer.
     *
     * @param name telemetry service name
     * @return true if the service is successfully started, false otherwise
     */
    boolean start(String name);

    /**
     * Terminates the telemetry producer.
     *
     * @param name telemetry service name
     */
    void stop(String name);

    /**
     * Restarts the telemetry producer.
     *
     * @param name telemetry service name
     * @return true if the service is successfully restarted, false otherwise
     */
    boolean restart(String name);

    /**
     * Launches all telemetry services.
     */
    void startAll();

    /**
     * Terminates all telemetry services.
     */
    void stopAll();

    /**
     * Restarts all telemetry services.
     */
    void restartAll();
}
