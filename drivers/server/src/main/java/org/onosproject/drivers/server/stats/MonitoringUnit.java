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

import static org.onosproject.drivers.server.Constants.MSG_CONVERSION_TO_BITS;
import static org.onosproject.drivers.server.Constants.MSG_CONVERSION_TO_BYTES;

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

    /**
     * Capacity-related monitoring units.
     */
    public enum CapacityUnit implements MonitoringUnit {

        BITS("Bits"),
        KILOBITS("kBits"),
        MEGABITS("MBits"),
        GIGABITS("GBits"),
        BYTES("Bytes"),
        KILOBYTES("kBytes"),
        MEGABYTES("MBytes"),
        GIGABYTES("GBytes");

        private String capacityUnit;

        // Statically maps capacity monitoring units to enum types
        private static final Map<String, MonitoringUnit> MAP =
            new HashMap<String, MonitoringUnit>();
        static {
            for (CapacityUnit cu : CapacityUnit.values()) {
                MAP.put(cu.toString().toLowerCase(), (MonitoringUnit) cu);
            }
        }

        private CapacityUnit(String capacityUnit) {
            this.capacityUnit = capacityUnit;
        }

        public static MonitoringUnit getByName(String cu) {
            cu = cu.toLowerCase();
            return MAP.get(cu);
        }

        public static float toBits(float value, CapacityUnit fromUnit) {
            if (value == 0) {
                return value;
            }

            if (fromUnit == BITS) {
                return value;
            } else if (fromUnit == KILOBITS) {
                return (value * 1000);
            } else if (fromUnit == MEGABITS) {
                return (value * 1000000);
            } else if (fromUnit == GIGABITS) {
                return (value * 1000000000);
            } else if (fromUnit == BYTES) {
                return value * 8;
            } else if (fromUnit == KILOBYTES) {
                return (value * 1000) * 8;
            } else if (fromUnit == MEGABYTES) {
                return (value * 1000000) * 8;
            } else if (fromUnit == GIGABYTES) {
                return (value * 1000000000) * 8;
            }

            throw new IllegalArgumentException(MSG_CONVERSION_TO_BITS);
        }

        public static float toKiloBits(float value, CapacityUnit fromUnit) {
            return toBits(value, fromUnit) / 1000;
        }

        public static float toMegaBits(float value, CapacityUnit fromUnit) {
            return toBits(value, fromUnit) / 1000000;
        }

        public static float toGigaBits(float value, CapacityUnit fromUnit) {
            return toBits(value, fromUnit) / 1000000000;
        }

        public static float toBytes(float value, CapacityUnit fromUnit) {
            if (value == 0) {
                return value;
            }

            if (fromUnit == BITS) {
                return value / 8;
            } else if (fromUnit == KILOBITS) {
                return (value * 1000) / 8;
            } else if (fromUnit == MEGABITS) {
                return (value * 1000000) / 8;
            } else if (fromUnit == GIGABITS) {
                return (value * 1000000000) / 8;
            } else if (fromUnit == BYTES) {
                return value;
            } else if (fromUnit == KILOBYTES) {
                return value * 1000;
            } else if (fromUnit == MEGABYTES) {
                return value * 1000000;
            } else if (fromUnit == GIGABYTES) {
                return value * 1000000000;
            }

            throw new IllegalArgumentException(MSG_CONVERSION_TO_BYTES);
        }

        public static float toKiloBytes(float value, CapacityUnit fromUnit) {
            return toBytes(value, fromUnit) / 1000;
        }

        public static float toMegaBytes(float value, CapacityUnit fromUnit) {
            return toBytes(value, fromUnit) / 1000000;
        }

        public static float toGigaBytes(float value, CapacityUnit fromUnit) {
            return toBytes(value, fromUnit) / 1000000000;
        }

        @Override
        public String toString() {
            return this.capacityUnit;
        }

    };

    /**
     * Percentage-related monitoring unit.
     */
    public enum PercentageUnit implements MonitoringUnit {

        PERCENTAGE("percentage");

        private String percentageUnit;

        // Statically maps percentage monitoring units to enum types
        private static final Map<String, MonitoringUnit> MAP =
            new HashMap<String, MonitoringUnit>();
        static {
            MAP.put(PERCENTAGE.toString().toLowerCase(), (MonitoringUnit) PERCENTAGE);
        }

        private PercentageUnit(String percentageUnit) {
            this.percentageUnit = percentageUnit;
        }

        public static MonitoringUnit getByName(String pu) {
            pu = pu.toLowerCase();
            return MAP.get(pu);
        }

        @Override
        public String toString() {
            return this.percentageUnit;
        }

    };

    String toString();

}
