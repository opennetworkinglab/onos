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

package org.onosproject.openstacktelemetry.impl;

/**
 * Name/Value constants for properties.
 */
public final class OsgiPropertyConstants {
    private OsgiPropertyConstants() {
    }

    // Stats flow rule manager
    static final String PROP_REVERSE_PATH_STATS = "reversePathStats";
    static final boolean PROP_REVERSE_PATH_STATS_DEFAULT = false;

    static final String PROP_EGRESS_STATS = "egressStats";
    static final boolean PROP_EGRESS_STATS_DEFAULT = false;

    static final String PROP_PORT_STATS = "portStats";
    static final boolean PROP_PORT_STATS_DEFAULT = true;

    static final String PROP_MONITOR_OVERLAY = "monitorOverlay";
    static final boolean PROP_MONITOR_OVERLAY_DEFAULT = true;

    static final String PROP_MONITOR_UNDERLAY = "monitorUnderlay";
    static final boolean PROP_MONITOR_UNDERLAY_DEFAULT = true;
}
