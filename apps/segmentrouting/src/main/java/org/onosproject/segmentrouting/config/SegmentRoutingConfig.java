/*
 * Copyright 2014-2015 Open Networking Laboratory
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

package org.onosproject.segmentrouting.config;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.basics.BasicElementConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Configuration object for Segment Routing Application.
 */
public class SegmentRoutingConfig extends Config<DeviceId> {
    private static final String NAME = "name";
    private static final String IP = "routerIp";
    private static final String MAC = "routerMac";
    private static final String SID = "nodeSid";
    private static final String EDGE = "isEdgeRouter";
    private static final String ADJSID = "adjacencySids";

    public Optional<String> getName() {
        String name = get(NAME, null);
        return name != null ? Optional.of(name) : Optional.empty();
    }

    public BasicElementConfig setName(String name) {
        return (BasicElementConfig) setOrClear(NAME, name);
    }

    public Ip4Address getIp() {
        String ip = get(IP, null);
        return ip != null ? Ip4Address.valueOf(ip) : null;
    }

    public BasicElementConfig setIp(String ip) {
        return (BasicElementConfig) setOrClear(IP, ip);
    }

    public MacAddress getMac() {
        String mac = get(MAC, null);
        return mac != null ? MacAddress.valueOf(mac) : null;
    }

    public BasicElementConfig setMac(String mac) {
        return (BasicElementConfig) setOrClear(MAC, mac);
    }

    public int getSid() {
        return get(SID, -1);
    }

    public BasicElementConfig setSid(int sid) {
        return (BasicElementConfig) setOrClear(SID, sid);
    }

    public boolean isEdgeRouter() {
        return get(EDGE, false);
    }

    public BasicElementConfig setEdgeRouter(boolean isEdgeRouter) {
        return (BasicElementConfig) setOrClear(EDGE, isEdgeRouter);
    }

    public List<AdjacencySid> getAdjacencySids() {
        ArrayList<AdjacencySid> adjacencySids = new ArrayList<>();

        if (!object.has(ADJSID)) {
            return adjacencySids;
        }

        ArrayNode adjacencySidNodes = (ArrayNode) object.path(ADJSID);
        adjacencySidNodes.forEach(adjacencySidNode -> {
            int asid = adjacencySidNode.path(AdjacencySid.ASID).asInt();

            ArrayList<Integer> ports = new ArrayList<Integer>();
            ArrayNode portsNodes = (ArrayNode) adjacencySidNode.path(AdjacencySid.PORTS);
            portsNodes.forEach(portNode -> {
                ports.add(portNode.asInt());
            });

            AdjacencySid adjacencySid = new AdjacencySid(asid, ports);
            adjacencySids.add(adjacencySid);
        });

        return adjacencySids;
    }

    public class AdjacencySid {
        private static final String ASID = "adjSid";
        private static final String PORTS = "ports";

        int asid;
        List<Integer> ports;

        public AdjacencySid(int asid, List<Integer> ports) {
            this.asid = asid;
            this.ports = ports;
        }

        public int getAsid() {
            return asid;
        }

        public List<Integer> getPorts() {
            return ports;
        }
    }
}