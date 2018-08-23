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

package org.onosproject.simplefabric.api;

/**
 * Provides constants used in simple fabric services.
 */
public final class Constants {

    private Constants() {
    }

    // App symbols
    public static final String APP_ID = "org.onosproject.simplefabric";
    public static final String FORWARDING_APP_ID = "org.onosproject.simplefabric.forwarding";
    public static final String ROUTING_APP_ID = "org.onosproject.simplefabric.routing";

    // Priority for l2NetworkRouting: L2NETWORK_UNICAST or L2NETWORK_BROADCAST
    public static final int PRI_L2NETWORK_UNICAST = 601;
    public static final int PRI_L2NETWORK_BROADCAST = 600;

    // Reactive Routing within Local Subnets
    // ASSUME: local subnets NEVER overlaps each other
    public static final int PRI_REACTIVE_LOCAL_FORWARD = 501;
    public static final int PRI_REACTIVE_LOCAL_INTERCEPT = 500;
    // Reactive Routing for Border Routes with local subnet
    // Priority: REACTIVE_BROUTE_BASE + routeIpPrefix * REACTIVE_BROUTE_STEP
    //           + REACTIVE_BROUTE_FORWARD or REACTIVE_BROUTE_INTERCEPT
    public static final int PRI_REACTIVE_BORDER_BASE = 100;
    public static final int PRI_REACTIVE_BORDER_STEP = 2;
    public static final int PRI_REACTIVE_BORDER_FORWARD = 1;
    public static final int PRI_REACTIVE_BORDER_INTERCEPT = 0;

    // Simple fabric event related timers
    public static final long IDLE_INTERVAL_MSEC = 5000;

    // Feature control parameters
    public static final boolean ALLOW_IPV6 = false;
    public static final boolean ALLOW_ETH_ADDRESS_SELECTOR = true;
    public static final boolean REACTIVE_SINGLE_TO_SINGLE = false;
    public static final boolean REACTIVE_ALLOW_LINK_CP = false;  // MUST BE false (yjlee, 2017-10-18)
    public static final boolean REACTIVE_HASHED_PATH_SELECTION = false;
    public static final boolean REACTIVE_MATCH_IP_PROTO = false;
}
