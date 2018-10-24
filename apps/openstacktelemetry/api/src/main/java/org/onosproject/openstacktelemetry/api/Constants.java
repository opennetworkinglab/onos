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
package org.onosproject.openstacktelemetry.api;

/**
 * Provides constants used in OpenstackTelemetry.
 */
public final class Constants {

    private Constants() {
    }

    public static final String OPENSTACK_TELEMETRY_APP_ID = "org.onosproject.openstacktelemetry";

    private static final String DEFAULT_SERVER_IP = "localhost";

    // default configuration variables for InfluxDB
    public static final String DEFAULT_INFLUXDB_MEASUREMENT = "sonaflow";

    public static final String VXLAN = "VXLAN";
    public static final String VLAN = "VLAN";
    public static final String FLAT = "FLAT";

    // default configuration variables for ONOS GUI
    public static final int DEFAULT_DATA_POINT_SIZE = 17280;
}
