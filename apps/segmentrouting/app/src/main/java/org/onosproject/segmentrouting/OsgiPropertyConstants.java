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

package org.onosproject.segmentrouting;

/**
 * Constants for default values of configurable properties.
 */
public final class OsgiPropertyConstants {

    private OsgiPropertyConstants() {}

    public static final String PROP_ACTIVE_PROBING = "activeProbing";
    public static final boolean ACTIVE_PROBING_DEFAULT = true;

    public static final String PROP_SINGLE_HOMED_DOWN = "singleHomedDown";
    public static final boolean SINGLE_HOMED_DOWN_DEFAULT = false;

    public static final String PROP_RESPOND_TO_UNKNOWN_HOSTS = "respondToUnknownHosts";
    public static final boolean RESPOND_TO_UNKNOWN_HOSTS_DEFAULT = true;

    public static final String PROP_ROUTE_DOUBLE_TAGGED_HOSTS = "routeDoubleTaggedHosts";
    public static final boolean ROUTE_DOUBLE_TAGGED_HOSTS_DEFAULT = false;

    public static final String PROP_DEFAULT_INTERNAL_VLAN = "defaultInternalVlan";
    public static final int DEFAULT_INTERNAL_VLAN_DEFAULT = 4094;

    public static final String PROP_PW_TRANSPORT_VLAN = "pwTransportVlan";
    public static final int PW_TRANSPORT_VLAN_DEFAULT = 4090;

    static final String PROP_SYMMETRIC_PROBING = "symmetricProbing";
    static final boolean SYMMETRIC_PROBING_DEFAULT = false;

    public static final String PROP_ROUTE_SIMPLIFICATION = "routeSimplification";
    public static final boolean ROUTE_SIMPLIFICATION_DEFAULT = false;


}
