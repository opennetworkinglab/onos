/*
 * Copyright 2015 Open Networking Laboratory
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
 *
 */

package org.onosproject.ui.topo;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Encapsulates highlights to be applied to the topology view, such as
 * highlighting links, displaying link labels, perhaps even decorating
 * nodes with badges, etc.
 */
public class Highlights {

    private static final String EMPTY = "";
    private static final String MIN = "min";
    private static final String MAX = "max";

    /**
     * A notion of amount.
     */
    public enum Amount {
        ZERO(EMPTY),
        MINIMALLY(MIN),
        MAXIMALLY(MAX);

        private final String s;
        Amount(String str) {
            s = str;
        }

        @Override
        public String toString() {
            return s;
        }
    }

    private final Set<DeviceHighlight> devices = new HashSet<>();
    private final Set<HostHighlight> hosts = new HashSet<>();
    private final Set<LinkHighlight> links = new HashSet<>();

    private Amount subdueLevel = Amount.ZERO;


    /**
     * Adds highlighting information for a device.
     *
     * @param dh device highlight
     * @return self, for chaining
     */
    public Highlights add(DeviceHighlight dh) {
        devices.add(dh);
        return this;
    }

    /**
     * Adds highlighting information for a host.
     *
     * @param hh host highlight
     * @return self, for chaining
     */
    public Highlights add(HostHighlight hh) {
        hosts.add(hh);
        return this;
    }

    /**
     * Adds highlighting information for a link.
     *
     * @param lh link highlight
     * @return self, for chaining
     */
    public Highlights add(LinkHighlight lh) {
        links.add(lh);
        return this;
    }

    /**
     * Marks the amount by which all other elements (devices, hosts, links)
     * not explicitly referenced here will be "subdued" visually.
     *
     * @param amount amount to subdue other elements
     * @return self, for chaining
     */
    public Highlights subdueAllElse(Amount amount) {
        subdueLevel = checkNotNull(amount);
        return this;
    }

    /**
     * Returns the set of device highlights.
     *
     * @return device highlights
     */
    public Set<DeviceHighlight> devices() {
        return Collections.unmodifiableSet(devices);
    }

    /**
     * Returns the set of host highlights.
     *
     * @return host highlights
     */
    public Set<HostHighlight> hosts() {
        return Collections.unmodifiableSet(hosts);
    }

    /**
     * Returns the set of link highlights.
     *
     * @return link highlights
     */
    public Set<LinkHighlight> links() {
        return Collections.unmodifiableSet(links);
    }

    /**
     * Returns the amount by which all other elements not explicitly
     * referenced here should be "subdued".
     *
     * @return amount to subdue other elements
     */
    public Amount subdueLevel() {
        return subdueLevel;
    }
}
