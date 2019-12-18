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
package org.onosproject.net.config.basics;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableSet;
import org.onlab.packet.EthType;
import org.onlab.packet.IpAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Basic configuration for network end-station hosts.
 */
public final class BasicHostConfig extends BasicElementConfig<HostId> {

    private static final String IPS = "ips";
    private static final String LOCATIONS = "locations";
    private static final String AUX_LOCATIONS = "auxLocations";
    private static final String INNER_VLAN = "innerVlan";
    private static final String OUTER_TPID = "outerTpid";
    private static final String DASH = "-";

    @Override
    public boolean isValid() {
        // locations is mandatory and must have at least one
        // ipAddresses can be absent, but if present must be valid
        // innerVlan: 0 < innerVlan < VlanId.MAX_VLAN, if present
        // outerTpid: either 0x8100 or 0x88a8, if present
        if (!isIntegralNumber(object, INNER_VLAN, FieldPresence.OPTIONAL, 0, VlanId.MAX_VLAN)) {
            return false;
        }
        checkArgument(!hasField(object, OUTER_TPID) ||
                              (short) (Integer.decode(get(OUTER_TPID, "0")) & 0xFFFF) ==
                              EthType.EtherType.QINQ.ethType().toShort() ||
                              (short) (Integer.decode(get(OUTER_TPID, "0")) & 0xFFFF) ==
                              EthType.EtherType.VLAN.ethType().toShort());
        this.locations();
        this.ipAddresses();
        this.auxLocations();
        return hasOnlyFields(ALLOWED, NAME, LOC_TYPE, LATITUDE, LONGITUDE, ROLES,
                             GRID_X, GRID_Y, UI_TYPE, RACK_ADDRESS, OWNER, IPS, LOCATIONS, AUX_LOCATIONS,
                             INNER_VLAN, OUTER_TPID);
    }

    @Override
    public String name() {
        // NOTE:
        // We don't want to default to host ID if friendly name is not set;
        // (it isn't particularly friendly, e.g. "00:00:00:00:00:01/None").
        // We'd prefer to clear the annotation, but if we pass null, then the
        // value won't get set (see BasicElementOperator). So, instead we will
        // return a DASH to signify "use the default friendly name".
        return get(NAME, DASH);
    }

    /**
     * Returns the location of the host.
     *
     * @return location of the host or null if none specified
     * @throws IllegalArgumentException if locations are set but empty or not
     *                                  specified with correct format
     */
    public Set<HostLocation> locations() {
        if (!object.has(LOCATIONS)) {
            return null; //no locations are specified
        }

        ImmutableSet.Builder<HostLocation> locationsSetBuilder = ImmutableSet.<HostLocation>builder();

        ArrayNode locationNodes = (ArrayNode) object.path(LOCATIONS);
        locationNodes.forEach(n -> {
            ConnectPoint cp = ConnectPoint.deviceConnectPoint((n.asText()));
            locationsSetBuilder.add(new HostLocation(cp, 0));
        });


        Set<HostLocation> locations = locationsSetBuilder.build();
        if (locations.isEmpty()) {
            throw new IllegalArgumentException("Host should have at least one location");
        }

        return locations;
    }

    /**
     * Returns the auxLocations of the host.
     *
     * @return auxLocations of the host or null if none specified
     * @throws IllegalArgumentException if auxLocations are set but empty or not
     *                                  specified with correct format
     */
    public Set<HostLocation> auxLocations() {
        if (!object.has(AUX_LOCATIONS)) {
            return null; //no auxLocations are specified
        }

        ImmutableSet.Builder<HostLocation> auxLocationsSetBuilder = ImmutableSet.<HostLocation>builder();

        ArrayNode auxLocationNodes = (ArrayNode) object.path(AUX_LOCATIONS);
        auxLocationNodes.forEach(n -> {
            ConnectPoint cp = ConnectPoint.deviceConnectPoint((n.asText()));
            auxLocationsSetBuilder.add(new HostLocation(cp, 0));
        });

        return auxLocationsSetBuilder.build();
    }

    /**
     * Sets the location of the host.
     *
     * @param locations location of the host or null to unset
     * @return the config of the host
     */
    public BasicHostConfig setLocations(Set<HostLocation> locations) {
        return (BasicHostConfig) setOrClear(LOCATIONS, locations);
    }

    /**
     * Sets the auxLocations of the host.
     *
     * @param auxLocations auxLocations of the host or null to unset
     * @return the config of the host
     */
    public BasicHostConfig setAuxLocations(Set<HostLocation> auxLocations) {
        return (BasicHostConfig) setOrClear(AUX_LOCATIONS, auxLocations);
    }


    /**
     * Returns IP addresses of the host.
     *
     * @return IP addresses of the host or null if not set
     * @throws IllegalArgumentException if not specified with correct format
     */
    public Set<IpAddress> ipAddresses() {
        HashSet<IpAddress> ipAddresses = new HashSet<>();
        if (object.has(IPS)) {
            ArrayNode ipNodes = (ArrayNode) object.path(IPS);
            ipNodes.forEach(n -> ipAddresses.add(IpAddress.valueOf(n.asText())));
            return ipAddresses;
        }
        return null;
    }

    /**
     * Sets the IP addresses of the host.
     *
     * @param ipAddresses IP addresses of the host or null to unset
     * @return the config of the host
     */
    public BasicHostConfig setIps(Set<IpAddress> ipAddresses) {
        return (BasicHostConfig) setOrClear(IPS, ipAddresses);
    }

    public VlanId innerVlan() {
        String vlan = get(INNER_VLAN, null);
        return vlan == null ? VlanId.NONE : VlanId.vlanId(Short.valueOf(vlan));
    }

    public BasicHostConfig setInnerVlan(VlanId vlanId) {
        return (BasicHostConfig) setOrClear(INNER_VLAN, vlanId.toString());
    }

    public EthType outerTpid() {
        short tpid = (short) (Integer.decode(get(OUTER_TPID, "0x8100")) & 0xFFFF);
        if (!(tpid == EthType.EtherType.VLAN.ethType().toShort() ||
                tpid == EthType.EtherType.QINQ.ethType().toShort())) {
            return EthType.EtherType.UNKNOWN.ethType();
        }
        return EthType.EtherType.lookup(tpid).ethType();
    }

    public BasicHostConfig setOuterTpid(EthType tpid) {
        return (BasicHostConfig) setOrClear(OUTER_TPID, String.format("0x%04X", tpid.toShort()));
    }
}
