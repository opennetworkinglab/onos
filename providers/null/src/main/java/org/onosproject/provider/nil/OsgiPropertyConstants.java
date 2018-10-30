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

package org.onosproject.provider.nil;

/**
 * Constants for default values of configurable properties.
 */
public final class OsgiPropertyConstants {

    private OsgiPropertyConstants() {}

    public static final String ENABLED = "enabled";
    public static final boolean ENABLED_DEFAULT = false;

    public static final String TOPO_SHAPE = "topoShape";
    public static final String TOPO_SHAPE_DEFAULT = "configured";

    public static final String DEVICE_COUNT = "deviceCount";
    public static final int DEVICE_COUNT_DEFAULT = 10;

    public static final String HOST_COUNT = "hostCount";
    public static final int HOST_COUNT_DEFAULT = 5;

    public static final String PACKET_RATE = "packetRate";
    public static final int PACKET_RATE_DEFAULT = 0;

    public static final String MUTATION_RATE = "mutationRate";
    public static final double MUTATION_RATE_DEFAULT = 0;

    public static final String MASTERSHIP = "mastership";
    public static final String MASTERSHIP_DEFAULT = "random";

}
