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
package org.onosproject.net.host;

import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;

/**
 * Information related to the host being probed.
 */
public interface HostProbe extends Host {
    /**
     * Gets connect point of this entry.
     *
     * @return connect point
     */
    ConnectPoint connectPoint();

    /**
     * Gets retry counter of this entry.
     *
     * @return retry
     */
    int retry();

    /**
     * Decrease retry counter of this entry by one.
     */
    void decreaseRetry();

    /**
     * Gets mode of this entry.
     *
     * @return mode
     */
    ProbeMode mode();

    /**
     * Gets probe MAC of this entry.
     *
     * @return probe mac
     */
    MacAddress probeMac();
}
