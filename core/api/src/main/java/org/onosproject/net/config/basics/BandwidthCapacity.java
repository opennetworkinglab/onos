/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.net.config.basics;

import static com.google.common.base.Preconditions.checkNotNull;

import org.onlab.util.Bandwidth;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;

/**
 * Configuration to specify maximum available bandwidth resource (Capacity) on a port.
 */
@Beta
public class BandwidthCapacity extends Config<ConnectPoint> {

    /**
     * netcfg ConfigKey for {@link BandwidthCapacity}.
     */
    public static final String CONFIG_KEY = "bandwidthCapacity";

    // JSON key
    private static final String CAPACITY = "capacityMbps";

    private static final Logger log = LoggerFactory.getLogger(BandwidthCapacity.class);

    @Override
    public boolean isValid() {
        // Validate the capacity
        capacity();

        // Open for extension (adding fields) in the future,
        // must have CAPACITY field.
        return isNumber(CAPACITY, FieldPresence.MANDATORY);
    }

    /**
     * Sets the Available Bandwidth resource (Capacity).
     *
     * @param bandwidth value to set.
     * @return self
     */
    public BandwidthCapacity capacity(Bandwidth bandwidth) {
        checkNotNull(bandwidth);

        // TODO current Bandwidth API end up value converted to double
        setOrClear(CAPACITY, bandwidth.bps());
        return this;
    }

    /**
     * Available Bandwidth resource (Capacity).
     *
     * @return {@link Bandwidth}
     */
    public Bandwidth capacity() {
        JsonNode v = object.path(CAPACITY);

        if (v.isIntegralNumber()) {

            return Bandwidth.mbps(v.asLong());
        } else if (v.isFloatingPointNumber()) {

            return Bandwidth.mbps(v.asDouble());
        } else {
            log.warn("Unexpected JsonNode for {}: {}", CAPACITY, v);
            return Bandwidth.mbps(v.asDouble());
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("capacity", capacity())
                .toString();
    }
}
