/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.drivers.server.stats;

import java.util.Map;
import java.util.HashMap;

/**
 * Representation of a monitoring unit.
 */
public interface MonitoringUnit {

    /**
     * Throughput-related monitoring units.
     */
    public enum ThroughputUnit implements MonitoringUnit {

        BPS("bps"),
        KBPS("kbps"),
        MBPS("mbps"),
        GBPS("gbps");

        private String throughputUnit;

        // Statically maps throughput monitoring units to enum types
        private static final Map<String, MonitoringUnit> MAP =
            new HashMap<String, MonitoringUnit>();
        static {
            for (ThroughputUnit tu : ThroughputUnit.values()) {
                MAP.put(tu.toString().toLowerCase(), (MonitoringUnit) tu);
            }
        }

        private ThroughputUnit(String throughputUnit) {
            this.throughputUnit = throughputUnit;
        }

        public static MonitoringUnit getByName(String tu) {
            tu = tu.toLowerCase();
            return MAP.get(tu);
        }

        public static float toGbps(float value, ThroughputUnit fromUnit) {
            if (value == 0) {
                return value;
            }

            if (fromUnit == BPS) {
                return value / 1000000000;
            } else if (fromUnit == KBPS) {
                return value / 1000000;
            } else if (fromUnit == MBPS) {
                return value / 1000;
            }

            return value;
        }

        @Override
        public String toString() {
            return this.throughputUnit;
        }

    };

    /**
     * Latency-related monitoring units.
     */
    public enum LatencyUnit implements MonitoringUnit {

        NANO_SECOND("ns"),
        MICRO_SECOND("us"),
        MILLI_SECOND("ms"),
        SECOND("s");

        private String latencyUnit;

        // Statically maps latency monitoring units to enum types
        private static final Map<String, MonitoringUnit> MAP =
            new HashMap<String, MonitoringUnit>();
        static {
            for (LatencyUnit lu : LatencyUnit.values()) {
                MAP.put(lu.toString().toLowerCase(), (MonitoringUnit) lu);
            }
        }

        private LatencyUnit(String latencyUnit) {
            this.latencyUnit = latencyUnit;
        }

        public static MonitoringUnit getByName(String lu) {
            lu = lu.toLowerCase();
            return MAP.get(lu);
        }

        public static float toNano(float value, LatencyUnit fromUnit) {
            if (value == 0) {
                return value;
            }

            if (fromUnit == MICRO_SECOND) {
                return value * 1000;
            } else if (fromUnit == MILLI_SECOND) {
                return value * 1000000;
            } else if (fromUnit == SECOND) {
                return value * 1000000000;
            }

            return value;
        }

        @Override
        public String toString() {
            return this.latencyUnit;
        }

    };

    String toString();

}
