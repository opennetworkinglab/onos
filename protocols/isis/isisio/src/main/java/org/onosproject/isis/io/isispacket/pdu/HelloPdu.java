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
package org.onosproject.isis.io.isispacket.pdu;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.isis.controller.IsisInterfaceState;
import org.onosproject.isis.io.isispacket.IsisHeader;
import org.onosproject.isis.io.isispacket.tlv.AdjacencyStateTlv;
import org.onosproject.isis.io.isispacket.tlv.AreaAddressTlv;
import org.onosproject.isis.io.isispacket.tlv.IpInterfaceAddressTlv;
import org.onosproject.isis.io.isispacket.tlv.IsisNeighborTlv;
import org.onosproject.isis.io.isispacket.tlv.IsisTlv;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of ISIS hello PDU.
 */
public abstract class HelloPdu extends IsisHeader {

    protected List<IsisTlv> variableLengths = new ArrayList<>();
    private byte circuitType;
    private String sourceId;
    private int holdingTime;
    private int pduLength;

    public void addTlv(IsisTlv isisTlv) {
        variableLengths.add(isisTlv);
    }

    /**
     * Returns the variable lengths.
     *
     * @return variable lengths
     */
    public List<IsisTlv> tlvs() {
        return variableLengths;
    }

    /**
     * Returns the list of area addresses.
     *
     * @return areaAddresses area addresses
     */
    public List<String> areaAddress() {
        List<String> areaAddresses = null;
        for (IsisTlv tlv : tlvs()) {
            if (tlv instanceof AreaAddressTlv) {
                areaAddresses = ((AreaAddressTlv) tlv).areaAddress();
            }
        }
        return areaAddresses;
    }

    /**
     * Returns the list of interface IP addresses.
     *
     * @return interfaceIpAddresses list of interface IP addresses
     */
    public List<Ip4Address> interfaceIpAddresses() {
        List<Ip4Address> interfaceIpAddresses = null;
        for (IsisTlv tlv : tlvs()) {
            if (tlv instanceof IpInterfaceAddressTlv) {
                interfaceIpAddresses = ((IpInterfaceAddressTlv) tlv).interfaceAddress();
            }
        }
        return interfaceIpAddresses;
    }

    /**
     * Returns the list of neighbor list.
     *
     * @return macAddresses list of neighbor MAC address
     */
    public List<MacAddress> neighborList() {
        List<MacAddress> macAddresses = null;
        for (IsisTlv tlv : tlvs()) {
            if (tlv instanceof IsisNeighborTlv) {
                macAddresses = ((IsisNeighborTlv) tlv).neighbor();
            }
        }
        return macAddresses;
    }

    /**
     * Returns the adjacency state.
     *
     * @return interfaceState adjacency state
     */
    public IsisInterfaceState adjacencyState() {
        IsisInterfaceState interfaceState = null;
        for (IsisTlv tlv : tlvs()) {
            if (tlv instanceof AdjacencyStateTlv) {
                interfaceState = IsisInterfaceState.get(((AdjacencyStateTlv) tlv).adjacencyType());
                break;
            }
        }
        return interfaceState;
    }

    /**
     * Returns the source ID.
     *
     * @return sourceId source ID
     */
    public String sourceId() {
        return sourceId;
    }

    /**
     * Sets source ID.
     *
     * @param sourceId source ID
     */
    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    /**
     * Returns the PDU length.
     *
     * @return pduLength PDU length
     */
    public int pduLength() {
        return pduLength;
    }

    /**
     * Sets the PDU length.
     *
     * @param pduLength PDU lenght
     */
    public void setPduLength(int pduLength) {
        this.pduLength = pduLength;
    }

    /**
     * Returns the holding time.
     *
     * @return holdingTime holding time
     */
    public int holdingTime() {
        return holdingTime;
    }

    /**
     * Sets the holding time.
     *
     * @param holdingTime holding time
     */
    public void setHoldingTime(int holdingTime) {
        this.holdingTime = holdingTime;
    }

    /**
     * Returns the circuit type.
     *
     * @return circuitType circuit type
     */
    public byte circuitType() {
        return circuitType;
    }

    /**
     * Sets the circuit type.
     *
     * @param circuitType circuit type
     */
    public void setCircuitType(byte circuitType) {
        this.circuitType = circuitType;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("circuitType", circuitType)
                .add("sourceId", sourceId)
                .add("holdingTime", holdingTime)
                .add("pduLength", pduLength)
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
        HelloPdu that = (HelloPdu) o;
        return Objects.equal(circuitType, that.circuitType) &&
                Objects.equal(sourceId, that.sourceId) &&
                Objects.equal(holdingTime, that.holdingTime) &&
                Objects.equal(pduLength, that.pduLength);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(circuitType, sourceId, holdingTime, pduLength);
    }
}