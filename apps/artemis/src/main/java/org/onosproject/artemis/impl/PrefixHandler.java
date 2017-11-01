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
package org.onosproject.artemis.impl;

import com.google.common.collect.Sets;
import org.onlab.packet.IpPrefix;
import org.onosproject.artemis.ArtemisPacketProcessor;
import org.onosproject.artemis.Monitors;
import org.onosproject.artemis.impl.monitors.ExaBgpMonitors;
import org.onosproject.artemis.impl.monitors.RipeMonitors;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Handler of monitoring step for each different prefix.
 * This class contains all the running monitors of the specified prefix.
 */
class PrefixHandler {

    private IpPrefix prefix;
    private Set<Monitors> prefixMonitors = Sets.newHashSet();

    /**
     * Constructor that takes a CIDR-notation string and a list of monitors.
     *
     * @param prefix          A CIDR-notation string, e.g. "192.168.0.1/24"
     * @param monitors        A map of strings to a set of string for monitors, e.g. "ripe", ["host1","host2",..]
     * @param packetProcessor Packet processor
     */
    PrefixHandler(IpPrefix prefix, Map<String, Set<String>> monitors, ArtemisPacketProcessor packetProcessor) {
        this.prefix = prefix;

        monitors.forEach((type, values) -> {
            if (Monitors.Types.getEnum(type).equals(Monitors.Types.RIPE)) {
                values.forEach(host -> prefixMonitors.add(new RipeMonitors(prefix, host, packetProcessor)));
            } else if (Monitors.Types.getEnum(type).equals(Monitors.Types.EXABGP)) {
                values.forEach(host -> prefixMonitors.add(new ExaBgpMonitors(prefix, host, packetProcessor)));
            }
        });
    }

    /**
     * Start all monitors for this prefix.
     */
    void startPrefixMonitors() {
        prefixMonitors.forEach(Monitors::startMonitor);
    }

    /**
     * Stop all monitors for this prefix.
     */
    void stopPrefixMonitors() {
        prefixMonitors.forEach(Monitors::stopMonitor);
    }

    /**
     * Return a CIDR-notation string of prefix.
     *
     * @return the prefix in CIDR-notation
     */
    IpPrefix getPrefix() {
        return prefix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PrefixHandler that = (PrefixHandler) o;
        return Objects.equals(prefix, that.prefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix);
    }
}
