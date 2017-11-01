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
package org.onosproject.artemis;

import org.onlab.packet.IpPrefix;

import java.util.Arrays;
import java.util.Optional;

/**
 * Interface for Monitors.
 */
public interface Monitors {
    /**
     * Get prefix of the specific monitor.
     *
     * @return prefix
     */
    IpPrefix getPrefix();

    /**
     * Set prefix for monitor.
     *
     * @param prefix prefix
     */
    void setPrefix(IpPrefix prefix);

    /**
     * Start monitor to begin capturing incoming BGP packets.
     */
    void startMonitor();

    /**
     * Stop monitor from capturing incoming BGP packets.
     */
    void stopMonitor();

    /**
     * Check if monitor is running.
     *
     * @return true if running
     */
    boolean isRunning();

    /**
     * Get host alias e.g. IP address, name.
     *
     * @return host alias
     */
    String getHost();

    /**
     * Set alias of host.
     *
     * @param host alias
     */
    void setHost(String host);

    /**
     * Match enum type with monitor type inside configuration to map them.
     */
    enum Types {
        RIPE("ripe") {
            @Override
            public String toString() {
                return "ripe";
            }
        },
        EXABGP("exabgp") {
            @Override
            public String toString() {
                return "exabgp";
            }
        };

        private String name;

        Types(String name) {
            this.name = name;
        }

        public static Types getEnum(String name) {
            Optional<Types> any = Arrays.stream(Types.values()).filter(typeStr -> typeStr.name.equals(name)).findAny();
            if (any.isPresent()) {
                return any.get();
            }
            throw new IllegalArgumentException("No enum defined for string: " + name);
        }
    }
}
