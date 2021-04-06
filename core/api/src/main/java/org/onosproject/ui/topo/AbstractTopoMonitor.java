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

package org.onosproject.ui.topo;

/**
 * Base class for the business logic of topology overlay "monitors".
 */
public class AbstractTopoMonitor {

    /**
     * Creates a new topo monitor base.
     */
    protected AbstractTopoMonitor() {
    }

    /**
     * Number of milliseconds between invocations of sending traffic data.
     */
    protected static long trafficPeriod = 5000;

    /**
     * Sets the traffic refresh period in milliseconds.
     *
     * @param ms refresh rate in millis
     */
    public static void setTrafficPeriod(long ms) {
        trafficPeriod = ms;
    }

    /**
     * Returns the traffic refresh period in milliseconds.
     *
     * @return refresh rate in millis
     */
    public static long getTrafficPeriod() {
        return trafficPeriod;
    }

    // TODO: pull common code up into this class

    // Note to Andrea:
    //  this class has to be defined in the core.api module, because
    //  external applications may want to extend it.
}
