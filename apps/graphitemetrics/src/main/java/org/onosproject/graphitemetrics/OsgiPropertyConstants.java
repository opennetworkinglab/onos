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

package org.onosproject.graphitemetrics;

public final class OsgiPropertyConstants {
    private OsgiPropertyConstants() {
    }

    static final String MONITOR_ALL = "monitorAll";
    static final boolean MONITOR_ALL_DEFAULT = true;

    static final String METRIC_NAMES = "metricNames";
    static final String METRIC_NAMES_DEFAULT = "default";

    static final String ADDRESS = "address";
    static final String ADDRESS_DEFAULT = "localhost";

    static final String PORT = "port";
    static final int PORT_DEFAULT = 2003;

    static final String REPORT_PERIOD = "reportPeriod";
    static final int REPORT_PERIOD_DEFAULT = 1;

    static final String METRIC_NAME_PREFIX = "metricNamePrefix";
    static final String METRIC_NAME_PREFIX_DEFAULT = "onos";
}
