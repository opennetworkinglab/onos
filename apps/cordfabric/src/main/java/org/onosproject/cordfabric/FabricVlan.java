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
 */

package org.onosproject.cordfabric;

import com.google.common.collect.ImmutableList;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;

import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Vlan which spans multiple fabric ports.
 */
public class FabricVlan {

    private final VlanId vlan;

    private final List<ConnectPoint> ports;
    private final boolean iptv;

    public FabricVlan(VlanId vlan, Collection<ConnectPoint> ports, boolean iptv) {
        checkNotNull(vlan);
        checkNotNull(ports);
        this.vlan = vlan;
        this.ports = ImmutableList.copyOf(ports);
        this.iptv = iptv;
    }

    public VlanId vlan() {
        return vlan;
    }

    public List<ConnectPoint> ports() {
        return ports;
    }

    public boolean iptv() {
        return iptv;
    }
}
