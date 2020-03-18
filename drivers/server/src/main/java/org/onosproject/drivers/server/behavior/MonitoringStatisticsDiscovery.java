/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.drivers.server.behavior;

import org.onosproject.drivers.server.stats.MonitoringStatistics;

import org.onosproject.net.driver.HandlerBehaviour;

import java.net.URI;

/**
 * Handler behaviour capable of collecting and updating
 * server monitoring statistics.
 */
public interface MonitoringStatisticsDiscovery extends HandlerBehaviour {

    /**
     * Returns global server monitoring statistics.
     * These statistics include all of the traffic that
     * goes through the NICs of the server.
     *
     * @return global monitoring statistics
     */
    MonitoringStatistics discoverGlobalMonitoringStatistics();

    /**
     * Returns monitoring statistics for a specific resource.
     * This resource represents a specific portion of the traffic.
     *
     * @param tcId the ID of the traffic class to be monitored
     * @return resource-specific monitoring statistics
     */
    MonitoringStatistics discoverMonitoringStatistics(URI tcId);

}
