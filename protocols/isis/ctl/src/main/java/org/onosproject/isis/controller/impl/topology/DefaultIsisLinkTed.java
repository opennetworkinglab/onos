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
package org.onosproject.isis.controller.impl.topology;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.onlab.packet.Ip4Address;
import org.onlab.util.Bandwidth;
import org.onosproject.isis.controller.topology.IsisLinkTed;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of an ISIS device information.
 */
public class DefaultIsisLinkTed implements IsisLinkTed {
    private int administrativeGroup;
    private Ip4Address ipv4InterfaceAddress;
    private Ip4Address ipv4NeighborAddress;
    private Bandwidth maximumLinkBandwidth;
    private Bandwidth maximumReservableLinkBandwidth;
    private List<Bandwidth> unreservedBandwidth = new ArrayList<>();
    private long teDefaultMetric;

    @Override
    public int administrativeGroup() {
        return administrativeGroup;
    }

    @Override
    public void setAdministrativeGroup(int administrativeGroup) {
        this.administrativeGroup = administrativeGroup;
    }

    @Override
    public Ip4Address ipv4InterfaceAddress() {
        return ipv4InterfaceAddress;
    }

    @Override
    public void setIpv4InterfaceAddress(Ip4Address interfaceAddress) {
        this.ipv4InterfaceAddress = interfaceAddress;
    }

    @Override
    public Ip4Address ipv4NeighborAddress() {
        return ipv4NeighborAddress;
    }

    @Override
    public void setIpv4NeighborAddress(Ip4Address neighborAddress) {
        this.ipv4NeighborAddress = neighborAddress;
    }

    @Override
    public Bandwidth maximumLinkBandwidth() {
        return maximumLinkBandwidth;
    }

    @Override
    public void setMaximumLinkBandwidth(Bandwidth bandwidth) {
        this.maximumLinkBandwidth = bandwidth;
    }

    @Override
    public Bandwidth maximumReservableLinkBandwidth() {
        return maximumReservableLinkBandwidth;
    }

    @Override
    public void setMaximumReservableLinkBandwidth(Bandwidth bandwidth) {
        this.maximumReservableLinkBandwidth = bandwidth;
    }

    @Override
    public List<Bandwidth> unreservedBandwidth() {
        return this.unreservedBandwidth;
    }

    @Override
    public void setUnreservedBandwidth(List<Bandwidth> bandwidth) {
        this.unreservedBandwidth.addAll(bandwidth);
    }

    @Override
    public long teDefaultMetric() {
        return teDefaultMetric;
    }

    @Override
    public void setTeDefaultMetric(long teMetric) {
        this.teDefaultMetric = teMetric;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("administrativeGroup", administrativeGroup)
                .add("ipv4InterfaceAddress", ipv4InterfaceAddress)
                .add("ipv4NeighborAddress", ipv4NeighborAddress)
                .add("maximumLinkBandwidth", maximumLinkBandwidth)
                .add("maximumReservableLinkBandwidth", maximumReservableLinkBandwidth)
                .add("teDefaultMetric", teDefaultMetric)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultIsisLinkTed that = (DefaultIsisLinkTed) o;
        return Objects.equal(administrativeGroup, that.administrativeGroup) &&
                Objects.equal(ipv4InterfaceAddress, that.ipv4InterfaceAddress) &&
                Objects.equal(ipv4NeighborAddress, that.ipv4NeighborAddress) &&
                Objects.equal(maximumLinkBandwidth, that.maximumLinkBandwidth) &&
                Objects.equal(maximumReservableLinkBandwidth,
                              that.maximumReservableLinkBandwidth) &&
                Objects.equal(teDefaultMetric, that.teDefaultMetric);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(administrativeGroup, ipv4InterfaceAddress,
                                ipv4NeighborAddress, maximumLinkBandwidth, teDefaultMetric);
    }
}