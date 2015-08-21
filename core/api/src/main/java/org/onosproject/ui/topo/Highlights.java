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

/**
 * Encapsulates highlights to be applied to the topology view, such as
 * highlighting links, displaying link labels, perhaps even decorating
 * nodes with badges, etc.
 */
public class Highlights {

    private final Set<DeviceHighlight> devices = new HashSet<>();
    private final Set<HostHighlight> hosts = new HashSet<>();
    private final Set<LinkHighlight> links = new HashSet<>();


    public Highlights add(DeviceHighlight d) {
        devices.add(d);
        return this;
    }

    public Highlights add(HostHighlight h) {
        hosts.add(h);
        return this;
    }

    public Highlights add(LinkHighlight lh) {
        links.add(lh);
        return this;
    }


    public Set<DeviceHighlight> devices() {
        return Collections.unmodifiableSet(devices);
    }

    public Set<HostHighlight> hosts() {
        return Collections.unmodifiableSet(hosts);
    }

    public Set<LinkHighlight> links() {
        return Collections.unmodifiableSet(links);
    }
}
