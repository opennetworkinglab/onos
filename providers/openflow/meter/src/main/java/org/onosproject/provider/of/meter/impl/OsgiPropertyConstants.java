/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onosproject.provider.of.meter.impl;

/**
 * Name/Value constants for properties.
 */

public final class OsgiPropertyConstants {

    private OsgiPropertyConstants() {
    }
    // Meter stats are used to reconcile the controller internal MeterStore with the information available
    // from the switch. This property defines the poll interval for the meter stats request.
    public static final String METER_STATS_POLL_INTERVAL = "meterStatsPollInterval";
    public static final int METER_STATS_POLL_INTERVAL_DEFAULT = 10;

    // Defines if the controller should force request the meter stats after a meter has been removed to
    // sync the MeterStore as soon as possible with the switch meters while avoid waiting for the next
    // meter stats poll cycle.
    public static final String FORCE_STATS_AFTER_METER_REMOVAL = "forceStatsAfterMeterRemoval";
    public static final boolean FORCE_STATS_AFTER_METER_REMOVAL_ENABLED_DEFAULT = true;



}
