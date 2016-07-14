/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.ui.topo;

import com.google.common.base.Preconditions;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

    private final Map<String, DeviceHighlight> devices = new HashMap<>();
    private final Map<String, HostHighlight> hosts = new HashMap<>();
    private final Map<String, LinkHighlight> links = new HashMap<>();

    private Amount subdueLevel = Amount.ZERO;
    private int delayMs = 0;

    //TODO: Think of a better solution for topology events race conditions
    /**
     * Sets the number of milliseconds to delay processing of highlights
     * events on the client side.
     *
     * @param ms milliseconds to delay
     * @return self, for chaining
     */
    public Highlights delay(int ms) {
        Preconditions.checkArgument(ms >= 0, "Delay cannot be lower than 0");
        delayMs = ms;
        return this;
    }

    /**
     * Return the delay for the highlight event.
     *
     * @return delay in milliseconds
     */
    public int delayMs() {
        return delayMs;
    }

    /**
     * Adds highlighting information for a device.
     *
     * @param dh device highlight
     * @return self, for chaining
     */
    public Highlights add(DeviceHighlight dh) {
        devices.put(dh.elementId(), dh);
        return this;
    }

    /**
     * Adds highlighting information for a host.
     *
     * @param hh host highlight
     * @return self, for chaining
     */
    public Highlights add(HostHighlight hh) {
        hosts.put(hh.elementId(), hh);
        return this;
    }

    /**
     * Adds highlighting information for a link.
     *
     * @param lh link highlight
     * @return self, for chaining
     */
    public Highlights add(LinkHighlight lh) {
        links.put(lh.elementId(), lh);
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
     * Returns the collection of device highlights.
     *
     * @return device highlights
     */
    public Collection<DeviceHighlight> devices() {
        return Collections.unmodifiableCollection(devices.values());
    }

    /**
     * Returns the collection of host highlights.
     *
     * @return host highlights
     */
    public Collection<HostHighlight> hosts() {
        return Collections.unmodifiableCollection(hosts.values());
    }

    /**
     * Returns the collection of link highlights.
     *
     * @return link highlights
     */
    public Collection<LinkHighlight> links() {
        return Collections.unmodifiableCollection(links.values());
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

    /**
     * Returns the node highlight (device or host) for the given element
     * identifier, or null if no match.
     *
     * @param id element identifier
     * @return corresponding node highlight
     */
    public NodeHighlight getNode(String id) {
        NodeHighlight nh = devices.get(id);
        return nh != null ? nh : hosts.get(id);
    }

    /**
     * Returns the device highlight for the given device identifier,
     * or null if no match.
     *
     * @param id device identifier
     * @return corresponding device highlight
     */
    public DeviceHighlight getDevice(String id) {
        return devices.get(id);
    }

    /**
     * Returns the host highlight for the given host identifier,
     * or null if no match.
     *
     * @param id host identifier
     * @return corresponding host highlight
     */
    public HostHighlight getHost(String id) {
        return hosts.get(id);
    }

    /**
     * Returns the link highlight for the given link identifier,
     * or null if no match.
     *
     * @param id link identifier
     * @return corresponding link highlight
     */
    public LinkHighlight getLink(String id) {
        return links.get(id);
    }

}
