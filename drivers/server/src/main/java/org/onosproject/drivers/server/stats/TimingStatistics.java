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

package org.onosproject.drivers.server.stats;

/**
 * Timing statistics API.
 */
public interface TimingStatistics {

    /**
     * Returns the unit of timing statistics.
     *
     * @return timing statistics' unit
     */
    MonitoringUnit unit();

    /**
     * Time (default is ns) to parse the controller's deployment instruction.
     *
     * @return time to parse a 'deploy' command
     */
    long deployCommandParsingTime();

    /**
     * Time (default is ns) to launch a slave process in the dataplane.
     *
     * @return time to launch a 'deploy' command
     */
    long deployCommandLaunchingTime();

    /**
     * Time (default is ns) to parse + launch the controller's deployment instruction.
     * This is the sum of the above two timers.
     *
     * @return time to parse + launch a 'deploy' command
     */
    long totalDeploymentTime();

    /**
     * Time (default is ns) to perform a local reconfiguration.
     * (i.e., the agent auto-scales the number of CPUs).
     *
     * @return time to auto scale
     */
    long autoScaleTime();

}
