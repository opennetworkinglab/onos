/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.artemis.impl.monitors;

import org.onlab.packet.IpPrefix;

/**
 * Abstract class for Monitors.
 */
public abstract class Monitor {
    /**
     * Match enum type with monitor type inside configuration to map them.
     */
    public enum Types {
        RIPE {
            @Override
            public String toString() {
                return "ripe";
            }
        },
        EXABGP {
            @Override
            public String toString() {
                return "exabgp";
            }
        }
    }

    IpPrefix prefix;
    Monitor(IpPrefix prefix) {
        this.prefix = prefix;
    }

    /**
     * Get prefix of the specific monitor.
     *
     * @return prefix
     */
    public IpPrefix getPrefix() {
        return prefix;
    }

    /**
     * Set prefix for monitor.
     *
     * @param prefix prefix
     */
    public void setPrefix(IpPrefix prefix) {
        this.prefix = prefix;
    }

    /**
     * Start monitor to begin capturing incoming BGP packets.
     */
    public abstract void startMonitor();

    /**
     * Stop monitor from capturing incoming BGP packets.
     */
    public abstract void stopMonitor();

    /**
     * Get type of monitor.
     *
     * @return enum type
     */
    public abstract Types getType();

    /**
     * Check if monitor is running.
     *
     * @return true if running
     */
    public abstract boolean isRunning();

    /**
     * Get host alias e.g. IP address, name.
     *
     * @return host alias
     */
    public abstract String getHost();

    /**
     * Set alias of host.
     *
     * @param host alias
     */
    public abstract void setHost(String host);
}
