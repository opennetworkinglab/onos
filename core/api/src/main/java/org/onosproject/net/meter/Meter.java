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
package org.onosproject.net.meter;

import org.onosproject.core.ApplicationId;

import java.util.Collection;

/**
 * Represents a generalized meter to be deployed on a device.
 */
public interface Meter {

    enum Unit {
        /**
         * Packets per second.
         */
        PKTS_PER_SEC,

        /**
         * Kilo bits per second.
         */
        KB_PER_SEC
    }

    /**
     * This meters id.
     *
     * @return a meter id
     */
    MeterId id();

    /**
     * The id of the application which created this meter.
     *
     * @return an application id
     */
    ApplicationId appId();

    /**
     * The unit used within this meter.
     *
     * @return
     */
    Unit unit();

    /**
     * Signals whether this meter applies to bursts only.
     *
     * @return a boolean
     */
    boolean isBurst();

    /**
     * The collection of bands to apply on the dataplane.
     *
     * @return a collection of bands.
     */
    Collection<Band> bands();

}
