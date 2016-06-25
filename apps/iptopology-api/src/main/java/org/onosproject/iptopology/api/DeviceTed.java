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
package org.onosproject.iptopology.api;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;

/**
 * Represents Device Traffic Engineering parameters.
 */
public class DeviceTed {
    private final List<Ip4Address> ipv4RouterIds;
    private final List<Ip6Address> ipv6RouterIds;
    private final List<TopologyId> topologyIds;
    private final Position position;

    /**
     * Constructor to initialize the parameter fields.
     *
     * @param ipv4RouterIds Router ids of Ipv4
     * @param ipv6RouterIds Router ids of Ipv6
     * @param topologyIds list of multi-topology IDs of the node
     * @param position of router whether it is ABR or ASBR
     */
    public DeviceTed(List<Ip4Address> ipv4RouterIds, List<Ip6Address> ipv6RouterIds,
                     List<TopologyId> topologyIds, Position position) {
        this.ipv4RouterIds = ipv4RouterIds;
        this.ipv6RouterIds = ipv6RouterIds;
        this.topologyIds = topologyIds;
        this.position = position;
    }

    /**
     * Obtain list of Ipv4 Router id.
     *
     * @return Ipv4 Router ids
     */
    public List<Ip4Address> ipv4RouterIds() {
        return ipv4RouterIds;
    }

    /**
     * Obtain list of Ipv6 Router id.
     *
     * @return Ipv6 Router ids
     */
    public List<Ip6Address> ipv6RouterIds() {
        return ipv6RouterIds;
    }

    /**
     * Obtain the list of topology ID's.
     *
     * @return list of topology id's
     */
    public List<TopologyId> topologyIds() {
        return topologyIds;
    }


    /**
     * Obtain position of device in the network.
     *
     * @return position of device in the network
     */
    public Position position() {
        return position;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipv4RouterIds, ipv6RouterIds, topologyIds, position);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DeviceTed) {
            int countObjSubTlv = 0;
            int countOtherSubTlv = 0;
            int countObjTopologyId = 0;
            int countOtherTopologyId = 0;
            boolean isCommonSubTlv = true;
            boolean isCommonSubTlv6 = true;
            boolean isCommonTopology = true;
            DeviceTed other = (DeviceTed) obj;
            Iterator<Ip4Address> objListIterator = other.ipv4RouterIds.iterator();
            countOtherSubTlv = other.ipv4RouterIds.size();
            countObjSubTlv = ipv4RouterIds.size();

            Iterator<Ip6Address> objListIteratorIpv6 = other.ipv6RouterIds.iterator();
            int countOtherSubTlv6 = other.ipv6RouterIds.size();
            int countObjSubTlv6 = ipv6RouterIds.size();

            Iterator<TopologyId> topologyId = other.topologyIds.iterator();
            countOtherTopologyId = other.topologyIds.size();
            countObjTopologyId = topologyIds.size();

            if (countObjSubTlv != countOtherSubTlv || countOtherSubTlv6 != countObjSubTlv6
                    || countObjTopologyId != countOtherTopologyId) {
                return false;
            } else {
                while (objListIterator.hasNext() && isCommonSubTlv) {
                    Ip4Address subTlv = objListIterator.next();
                    //find index of that element and then get that from the list and then compare
                    if (ipv4RouterIds.contains(subTlv) && other.ipv4RouterIds.contains(subTlv)) {
                        isCommonSubTlv = Objects.equals(ipv4RouterIds.get(ipv4RouterIds.indexOf(subTlv)),
                                other.ipv4RouterIds.get(other.ipv4RouterIds.indexOf(subTlv)));
                    } else {
                        isCommonSubTlv = false;
                    }
                }
                while (objListIteratorIpv6.hasNext() && isCommonSubTlv6) {
                    Ip6Address subTlv = objListIteratorIpv6.next();
                    //find index of that element and then get that from the list and then compare
                    if (ipv6RouterIds.contains(subTlv) && other.ipv6RouterIds.contains(subTlv)) {
                        isCommonSubTlv6 = Objects.equals(ipv6RouterIds.get(ipv6RouterIds.indexOf(subTlv)),
                                other.ipv6RouterIds.get(other.ipv6RouterIds.indexOf(subTlv)));
                    } else {
                        isCommonSubTlv6 = false;
                    }
                }
                while (topologyId.hasNext() && isCommonTopology) {
                    TopologyId subTlv = topologyId.next();
                    //find index of that element and then get that from the list and then compare
                    if (topologyIds.contains(subTlv) && other.topologyIds.contains(subTlv)) {
                        isCommonTopology = Objects.equals(topologyIds.get(topologyIds.indexOf(subTlv)),
                                other.topologyIds.get(other.topologyIds.indexOf(subTlv)));
                    } else {
                        isCommonTopology = false;
                    }
                }
                return isCommonSubTlv && isCommonSubTlv6 && isCommonTopology
                        && Objects.equals(position, other.position);
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .omitNullValues()
                .add("ipv6RouterIds", ipv6RouterIds)
                .add("ipv4RouterIds", ipv4RouterIds)
                .add("topologyIds", topologyIds)
                .add("position", position)
                .toString();
    }

}