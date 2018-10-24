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

package org.onosproject.routing.fpm;

/**
 * Name/Value constants for properties.
 */
public final class OsgiPropertyConstants {
    private OsgiPropertyConstants() {
    }

    public static final String CLEAR_ROUTES = "clearRoutes";
    public static final boolean CLEAR_ROUTES_DEFAULT = true;

    public static final String PD_PUSH_ENABLED = "pdPushEnabled";
    public static final boolean PD_PUSH_ENABLED_DEFAULT = false;

    public static final String PD_PUSH_NEXT_HOP_IPV4 = "pdPushNextHopIPv4";
    public static final String PD_PUSH_NEXT_HOP_IPV4_DEFAULT = "";

    public static final String PD_PUSH_NEXT_HOP_IPV6 = "pdPushNextHopIPv6";
    public static final String PD_PUSH_NEXT_HOP_IPV6_DEFAULT = "";
}
