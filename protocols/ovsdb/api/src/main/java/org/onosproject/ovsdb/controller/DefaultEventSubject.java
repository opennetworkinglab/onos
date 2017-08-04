/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.ovsdb.controller;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;
import java.util.Set;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;

/**
 * This class is default event subject that implements OvsdbEventSubject.
 */
public class DefaultEventSubject implements OvsdbEventSubject {
    private final MacAddress mac;
    private final Set<IpAddress> ips;
    private final OvsdbPortName portname;
    private final OvsdbPortNumber portnumber;
    private final OvsdbDatapathId dpid;
    private final OvsdbPortType portType;
    private final OvsdbIfaceId ifaceid;

    /**
     * Creates an end-station event subject using the supplied information.
     *
     * @param mac host MAC address
     * @param ips host MAC ips
     * @param portname port name
     * @param portnumber port number
     * @param dpid ovs dpid
     * @param portType port type
     * @param ifaceid  vm ifaceid
     */
    public DefaultEventSubject(MacAddress mac, Set<IpAddress> ips,
                               OvsdbPortName portname, OvsdbPortNumber portnumber, OvsdbDatapathId dpid,
                               OvsdbPortType portType, OvsdbIfaceId ifaceid) {
        super();
        this.mac = mac;
        this.ips = ips;
        this.portname = portname;
        this.portnumber = portnumber;
        this.dpid = dpid;
        this.portType = portType;
        this.ifaceid = ifaceid;
    }

    @Override
    public MacAddress hwAddress() {
        return mac;
    }

    @Override
    public Set<IpAddress> ipAddress() {
        return ips;
    }

    @Override
    public OvsdbPortName portName() {
        return portname;
    }

    @Override
    public OvsdbPortNumber portNumber() {
        return portnumber;
    }

    @Override
    public OvsdbPortType portType() {
        return portType;
    }

    @Override
    public OvsdbDatapathId dpid() {
        return dpid;
    }

    @Override
    public OvsdbIfaceId ifaceid() {
        return ifaceid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mac, portname, portnumber, dpid, portType, ifaceid);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultEventSubject) {
            final DefaultEventSubject other = (DefaultEventSubject) obj;
            return Objects.equals(this.mac, other.mac)
                    && Objects.equals(this.portname, other.portname)
                    && Objects.equals(this.portnumber, other.portnumber)
                    && Objects.equals(this.dpid, other.dpid)
                    && Objects.equals(this.portType, other.portType)
                    && Objects.equals(this.ifaceid, other.ifaceid);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("mac", mac).add("portname", portname)
                .add("portnumber", portnumber).add("portType", portType)
                .add("ipAddresses", ips).add("dpid", dpid).add("ifaceid", ifaceid)
                .toString();
    }
}
