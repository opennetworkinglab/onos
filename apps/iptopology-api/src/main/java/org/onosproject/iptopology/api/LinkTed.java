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
import org.onlab.util.Bandwidth;

/**
 * Represents Link Traffic engineering parameters.
 */
public class LinkTed {
    private final Bandwidth maximumLink;
    private final Bandwidth maxReserved;
    private final List<Bandwidth> maxUnResBandwidth;
    private final Metric teMetric;
    private final Metric igpMetric;
    private final List<Ip4Address> ipv4LocRouterId;
    private final List<Ip6Address> ipv6LocRouterId;
    private final List<Ip4Address> ipv4RemRouterId;
    private final List<Ip6Address> ipv6RemRouterId;
    private final Color color;
    private final Signalling signalType;
    private final List<Srlg> srlgGroup;
    private final ProtectionType protectType;

    /**
     * Constructor to initialize its parameter.
     *
     * @param maximumLink maximum bandwidth can be used
     * @param maxReserved max bandwidth that can be reserved
     * @param maxUnResBandwidth amount of bandwidth reservable
     * @param teMetric Traffic engineering metric
     * @param igpMetric IGP metric
     * @param color information on administrative group assigned to the interface
     * @param signalType MPLS signaling protocols
     * @param srlgGroup Shared Risk Link Group information
     * @param protectType protection capabilities of the link
     * @param ipv4LocRouterId IPv4 router-Id of local node
     * @param ipv6LocRouterId IPv6 router-Id of local node
     * @param ipv4RemRouterId IPv4 router-Id of remote node
     * @param ipv6RemRouterId IPv6 router-Id of remote node
     */
    public LinkTed(Bandwidth maximumLink, Bandwidth maxReserved, List<Bandwidth> maxUnResBandwidth,
                   Metric teMetric, Metric igpMetric, Color color, Signalling signalType, List<Srlg> srlgGroup,
                   ProtectionType protectType, List<Ip4Address> ipv4LocRouterId, List<Ip6Address> ipv6LocRouterId,
                   List<Ip4Address> ipv4RemRouterId, List<Ip6Address> ipv6RemRouterId) {
        this.maximumLink = maximumLink;
        this.maxReserved = maxReserved;
        this.maxUnResBandwidth = maxUnResBandwidth;
        this.teMetric = teMetric;
        this.igpMetric = igpMetric;
        this.color = color;
        this.signalType = signalType;
        this.srlgGroup = srlgGroup;
        this.protectType = protectType;
        this.ipv4LocRouterId = ipv4LocRouterId;
        this.ipv6LocRouterId = ipv6LocRouterId;
        this.ipv4RemRouterId = ipv4RemRouterId;
        this.ipv6RemRouterId = ipv6RemRouterId;
    }

    /**
     * Provides maximum bandwidth can be used on the link.
     *
     * @return maximum bandwidth
     */
    public Bandwidth maximumLink() {
        return maximumLink;
    }

    /**
     * Amount of bandwidth reservable on the link.
     *
     * @return unreserved bandwidth
     */
    public List<Bandwidth> maxUnResBandwidth() {
        return maxUnResBandwidth;
    }

    /**
     * Provides max bandwidth that can be reserved on the link.
     *
     * @return max bandwidth reserved
     */
    public Bandwidth maxReserved() {
        return maxReserved;
    }

    /**
     * Provides Traffic engineering metric for the link.
     *
     * @return Traffic engineering metric
     */
    public Metric teMetric() {
        return teMetric;
    }

    /**
     * Provides IGP metric for the link.
     *
     * @return IGP metric
     */
    public Metric igpMetric() {
        return igpMetric;
    }

    /**
     * Provides protection capabilities of the link.
     *
     * @return link protection type
     */
    public ProtectionType protectType() {
        return protectType;
    }

    /**
     * Provides Shared Risk Link Group information.
     *
     * @return Shared Risk Link Group value
     */
    public List<Srlg> srlgGroup() {
        return srlgGroup;
    }

    /**
     * Provides which MPLS signaling protocols are enabled.
     *
     * @return signal type
     */
    public Signalling signalType() {
        return signalType;
    }

    /**
     * Provides information on administrative group assigned to the interface.
     *
     * @return 4-octect bit mask assigned by network administrator
     */
    public Color color() {
        return color;
    }

    /**
     * Provides IPv4 router-Id of local node.
     *
     * @return IPv4 router-Id of local node
     */
    public List<Ip4Address> ipv4LocRouterId() {
        return ipv4LocRouterId;
    }

    /**
     * Provides IPv6 router-Id of local node.
     *
     * @return IPv6 router-Id of local node
     */
    public List<Ip6Address> ipv6LocRouterId() {
        return ipv6LocRouterId;
    }

    /**
     * Provides IPv4 router-Id of remote node.
     *
     * @return IPv4 router-Id of remote node
     */
    public List<Ip4Address> ipv4RemRouterId() {
        return ipv4RemRouterId;
    }

    /**
     * Provides IPv6 router-Id of remote node.
     *
     * @return IPv6 router-Id of remote node
     */
    public List<Ip6Address> ipv6RemRouterId() {
        return ipv6RemRouterId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(maximumLink, maxReserved, maxUnResBandwidth, teMetric, igpMetric,
                ipv4LocRouterId, ipv6LocRouterId, ipv4RemRouterId, ipv6RemRouterId,
                color, signalType, srlgGroup, protectType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LinkTed) {
            int countCommonBandwidth = 0;
            int countOtherCommonBandwidth = 0;
            int countOther4LocRouterId = 0;
            int countCommon4LocRouterId = 0;
            int countOther6RemRouterId = 0;
            int countCommon6RemRouterId = 0;
            int countOther4RemRouterId = 0;
            int countCommon4RemRouterId = 0;
            int countCommon6LocRouterId = 0;
            int countOther6LocRouterId = 0;
            int countCommonSrlg = 0;
            int countOtherSrlg = 0;
            boolean isCommonBandwidth = true;
            boolean isCommonIp4Loc = true;
            boolean isCommonIp4Rem = true;
            boolean isCommonIp6Loc = true;
            boolean isCommonIp6Rem = true;
            boolean isCommonSrlg = true;
            LinkTed other = (LinkTed) obj;
            Iterator<Bandwidth> objListIterator = other.maxUnResBandwidth.iterator();
            countOtherCommonBandwidth = other.maxUnResBandwidth.size();
            countCommonBandwidth = maxUnResBandwidth.size();

            Iterator<Ip4Address> ipv4local = other.ipv4LocRouterId.iterator();
            countOther4LocRouterId = other.ipv4LocRouterId.size();
            countCommon4LocRouterId = ipv4LocRouterId.size();

            Iterator<Ip4Address> ipv4remote = other.ipv4RemRouterId.iterator();
            countOther4RemRouterId = other.ipv4RemRouterId.size();
            countCommon4RemRouterId = ipv4RemRouterId.size();

            Iterator<Ip6Address> ipv6local = other.ipv6LocRouterId.iterator();
            countOther6LocRouterId = other.ipv6LocRouterId.size();
            countCommon6LocRouterId = ipv6LocRouterId.size();

            Iterator<Ip6Address> ipv6remote = other.ipv6RemRouterId.iterator();
            countOther6RemRouterId = other.ipv6RemRouterId.size();
            countCommon6RemRouterId = ipv6RemRouterId.size();

            Iterator<Srlg> srlg = other.srlgGroup.iterator();
            countOtherSrlg = other.srlgGroup.size();
            countCommonSrlg = srlgGroup.size();

            if (countOtherCommonBandwidth != countCommonBandwidth
                    || countOther4LocRouterId != countCommon4LocRouterId
                    || countOther4RemRouterId != countCommon4RemRouterId
                    || countOther6LocRouterId != countCommon6LocRouterId
                    || countOther6RemRouterId != countCommon6RemRouterId
                    || countOtherSrlg != countCommonSrlg) {
                return false;
            } else {
                while (objListIterator.hasNext() && isCommonBandwidth) {
                    Bandwidth subTlv = objListIterator.next();
                    if (maxUnResBandwidth.contains(subTlv) && other.maxUnResBandwidth.contains(subTlv)) {
                        isCommonBandwidth = Objects.equals(maxUnResBandwidth.get(maxUnResBandwidth.indexOf(subTlv)),
                                other.maxUnResBandwidth.get(other.maxUnResBandwidth.indexOf(subTlv)));
                    } else {
                        isCommonBandwidth = false;
                    }
                }
                while (ipv4local.hasNext() && isCommonIp4Loc) {
                    Ip4Address subTlv = ipv4local.next();
                    if (ipv4LocRouterId.contains(subTlv) && other.ipv4LocRouterId.contains(subTlv)) {
                        isCommonIp4Loc = Objects.equals(ipv4LocRouterId.get(ipv4LocRouterId.indexOf(subTlv)),
                                other.ipv4LocRouterId.get(other.ipv4LocRouterId.indexOf(subTlv)));
                    } else {
                        isCommonIp4Loc = false;
                    }
                }
                while (ipv4remote.hasNext() && isCommonIp4Rem) {
                    Ip4Address subTlv = ipv4remote.next();
                    if (ipv4RemRouterId.contains(subTlv) && other.ipv4RemRouterId.contains(subTlv)) {
                        isCommonIp4Rem = Objects.equals(ipv4RemRouterId.get(ipv4RemRouterId.indexOf(subTlv)),
                                other.ipv4RemRouterId.get(other.ipv4RemRouterId.indexOf(subTlv)));
                    } else {
                        isCommonIp4Rem = false;
                    }
                }
                while (ipv6remote.hasNext() && isCommonIp6Rem) {
                    Ip6Address subTlv = ipv6remote.next();
                    if (ipv6RemRouterId.contains(subTlv) && other.ipv6RemRouterId.contains(subTlv)) {
                        isCommonIp6Rem = Objects.equals(ipv6RemRouterId.get(ipv6RemRouterId.indexOf(subTlv)),
                                other.ipv6RemRouterId.get(other.ipv6RemRouterId.indexOf(subTlv)));
                    } else {
                        isCommonIp6Rem = false;
                    }
                }
                while (ipv6local.hasNext() && isCommonIp6Loc) {
                    Ip6Address subTlv = ipv6local.next();
                    if (ipv6LocRouterId.contains(subTlv) && other.ipv6LocRouterId.contains(subTlv)) {
                        isCommonIp6Loc = Objects.equals(ipv6LocRouterId.get(ipv6LocRouterId.indexOf(subTlv)),
                                other.ipv6LocRouterId.get(other.ipv6LocRouterId.indexOf(subTlv)));
                    } else {
                        isCommonIp6Loc = false;
                    }
                }
                while (srlg.hasNext() && isCommonIp6Loc) {
                    Srlg subTlv = srlg.next();
                    if (srlgGroup.contains(subTlv) && other.srlgGroup.contains(subTlv)) {
                        isCommonSrlg = Objects.equals(srlgGroup.get(srlgGroup.indexOf(subTlv)),
                                other.srlgGroup.get(other.srlgGroup.indexOf(subTlv)));
                    } else {
                        isCommonSrlg = false;
                    }
                }
                return isCommonBandwidth && isCommonIp4Loc && isCommonIp4Rem && isCommonIp6Rem && isCommonIp6Loc
                        && isCommonSrlg
                        && Objects.equals(igpMetric, other.igpMetric)
                        && Objects.equals(teMetric, other.teMetric)
                        && Objects.equals(maximumLink, other.maximumLink)
                        && Objects.equals(protectType, other.protectType)
                        && Objects.equals(color, other.color)
                        && Objects.equals(signalType, other.signalType);
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("igpMetric", igpMetric)
                .add("teMetric", teMetric)
                .add("maximumLink", maximumLink)
                .add("maxReserved", maxReserved)
                .add("maxUnResBandwidth", maxUnResBandwidth)
                .add("ipv4LocRouterId", ipv4LocRouterId)
                .add("ipv4RemRouterId", ipv4RemRouterId)
                .add("ipv6LocRouterId", ipv6LocRouterId)
                .add("ipv6RemRouterId", ipv6RemRouterId)
                .add("protectType", protectType)
                .add("color", color)
                .add("srlgGroup", srlgGroup)
                .add("signalType", signalType)
                .toString();
    }
}