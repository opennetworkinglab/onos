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
package org.onosproject.artemis.impl;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onlab.packet.IpPrefix;
import org.onosproject.artemis.impl.monitors.ExaBgpMonitor;
import org.onosproject.artemis.impl.monitors.Monitor;
import org.onosproject.artemis.impl.monitors.RipeMonitor;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Handler of monitoring step for each different prefix.
 * This class contains all the running monitors of the specified prefix.
 */
class PrefixHandler {

    private IpPrefix prefix;
    private Set<Monitor> prefixMonitors = Sets.newHashSet();

    /**
     * Constructor that takes a CIDR-notation string and a list of monitors.
     *
     * @param prefix   A CIDR-notation string, e.g. "192.168.0.1/24"
     * @param monitors A map of strings to a set of string for monitors, e.g. "ripe", ["host1","host2",..]
     */
    PrefixHandler(IpPrefix prefix, Map<String, Set<String>> monitors) {
        this.prefix = prefix;

        monitors.forEach((type, values) -> {
            if (type.equals(Monitor.Types.RIPE.toString())) {
                values.forEach(host -> prefixMonitors.add(new RipeMonitor(prefix, host)));
            } else if (type.equals(Monitor.Types.EXABGP.toString())) {
                values.forEach(host -> prefixMonitors.add(new ExaBgpMonitor(prefix, host)));
            }
        });
    }

    /**
     * Start all monitors for this prefix.
     */
    void startPrefixMonitors() {
        prefixMonitors.forEach(Monitor::startMonitor);
    }

    /**
     * Stop all monitors for this prefix.
     */
    void stopPrefixMonitors() {
        prefixMonitors.forEach(Monitor::stopMonitor);
    }

    /**
     * Return a CIDR-notation string of prefix.
     *
     * @return the prefix in CIDR-notation
     */
    IpPrefix getPrefix() {
        return prefix;
    }

    /**
     * Changes the monitors based on the new list given.
     *
     * @param newMonitors monitors to be added
     */
    void changeMonitors(Map<String, Set<String>> newMonitors) {
        Set<String> newTypes = newMonitors.keySet();
        Set<Monitor> monToRemove = Sets.newHashSet();
        Map<String, Set<String>> monToAdd = Maps.newHashMap(newMonitors);

        prefixMonitors.forEach(monitor -> {
            String oldType = monitor.getType().toString();
            if (newTypes.contains(oldType)) {
                Set<String> newHosts = newMonitors.get(oldType);
                String oldHost = monitor.getHost();
                if (newHosts.contains(oldHost)) {
                    monToAdd.remove(oldHost, oldHost);
                } else {
                    monToRemove.add(monitor);
                }
            } else {
                monToRemove.add(monitor);
            }
        });

        monToRemove.forEach(Monitor::stopMonitor);
        prefixMonitors.removeAll(monToRemove);

        //TODO
        monToAdd.forEach((type, values) -> {
            if (type.equals(Monitor.Types.RIPE.toString())) {
                values.forEach(host -> prefixMonitors.add(new RipeMonitor(prefix, host)));
            } else if (type.equals(Monitor.Types.EXABGP.toString())) {
                values.forEach(host -> prefixMonitors.add(new ExaBgpMonitor(prefix, host)));
            }
        });

        startPrefixMonitors();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(prefix);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PrefixHandler) {
            final PrefixHandler that = (PrefixHandler) obj;
            return Objects.equals(this.prefix, that.prefix);
        }
        return false;
    }

}
