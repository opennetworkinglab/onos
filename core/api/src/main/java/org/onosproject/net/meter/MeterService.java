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

import org.onosproject.event.ListenerService;

import java.util.Collection;

/**
 * Service for add/updating and removing meters. Meters are
 * are assigned to flow to rate limit them and provide a certain
 * quality of service.
 */
public interface MeterService
        extends ListenerService<MeterEvent, MeterListener> {

    /**
     * Adds a meter to the system and performs it installation.
     *
     * @param meter a meter.
     */
    void addMeter(MeterOperation meter);

    /**
     * Updates a meter by adding statistic information to the meter.
     *
     * @param meter an updated meter
     */
    void updateMeter(MeterOperation meter);

    /**
     * Remove a meter from the system and the dataplane.
     *
     * @param meter a meter to remove
     */
    void removeMeter(MeterOperation meter);

    /**
     * Fetch the meter by the meter id.
     *
     * @param id a meter id
     * @return a meter
     */
    Meter getMeter(MeterId id);

    /**
     * Fetches all the meters.
     *
     * @return a collection of meters
     */
    Collection<Meter> getAllMeters();

    /**
     * Allocate a meter id which must be used to create the meter.
     *
     * @return a meter id
     */
    MeterId allocateMeterId();
}
