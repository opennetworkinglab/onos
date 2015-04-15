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
package org.onosproject.routing.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.NetTools;
import org.onosproject.net.PortNumber;

import java.util.List;
import java.util.Objects;

/**
 * Represents a BGP daemon in SDN network.
 * <p>
 * Each BGP speaker has a attachment point, which includes a switch DPID and a
 * switch port. Each BGP speaker has one MAC address and several IP addresses,
 * which are used to peer with BGP peers outside the SDN network. For each
 * peer outside the SDN network, we configure a different IP address to BGP
 * speaker inside the SDN network.
 * </p>
 * <p>
 * Each BGP speaker has a name, which is a unique identifying String that is
 * used to reference this speaker in the configuration.
 * </p>
 */
public class BgpSpeaker {
    private final String name;
    private final ConnectPoint connectPoint;
    private final MacAddress macAddress;
    private List<InterfaceAddress> interfaceAddresses;

    /**
     * Class constructor used by the JSON library to create an object.
     *
     * @param name the name of the BGP speaker inside SDN network
     * @param attachmentDpid the DPID where the BGP speaker is attached to
     * @param attachmentPort the port where the BGP speaker is attached to
     * @param macAddress the MAC address of the BGP speaker
     */
    @JsonCreator
    public BgpSpeaker(@JsonProperty("name") String name,
            @JsonProperty("attachmentDpid") String attachmentDpid,
            @JsonProperty("attachmentPort") long attachmentPort,
            @JsonProperty("macAddress") String macAddress) {

        this.name = name;
        this.macAddress = MacAddress.valueOf(macAddress);
        this.connectPoint = new ConnectPoint(
                DeviceId.deviceId(NetTools.dpidToUri(attachmentDpid)),
                PortNumber.portNumber(attachmentPort));
    }

    /**
     * Sets the addresses we configured for the BGP speaker on all virtual
     * {@link Interface}s.
     *
     * @param interfaceAddresses a list of IP addresses of the BGP speaker
     * configured on all virtual interfaces
     */
    @JsonProperty("interfaceAddresses")
    public void setInterfaceAddresses(
            List<InterfaceAddress> interfaceAddresses) {
        this.interfaceAddresses = interfaceAddresses;
    }

    /**
     * Gets the BGP speaker name.
     *
     * @return the BGP speaker name
     */
    public String name() {
        return name;
    }

    /**
     * Gets the connect point where the BGP speaker is attached.
     *
     * @return the connect point
     */
    public ConnectPoint connectPoint() {
        return connectPoint;
    }

    /**
     * Gets the MAC address of the BGP speaker.
     *
     * @return the MAC address
     */
    public MacAddress macAddress() {
        return macAddress;
    }

    /**
     * Gets all IP addresses configured on all {@link Interface}s of the
     * BGP speaker.
     *
     * @return a list of IP addresses of the BGP speaker configured on all
     * virtual interfaces
     */
    public List<InterfaceAddress> interfaceAddresses() {
        return interfaceAddresses;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BgpSpeaker)) {
            return false;
        }

        BgpSpeaker otherBgpSpeaker = (BgpSpeaker) other;

        return  name.equals(otherBgpSpeaker.name) &&
                connectPoint.equals(
                        otherBgpSpeaker.connectPoint) &&
                macAddress.equals(otherBgpSpeaker.macAddress) &&
                interfaceAddresses.equals(otherBgpSpeaker.interfaceAddresses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, connectPoint, macAddress,
                interfaceAddresses);

    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("speakerName", name)
                .add("connectPoint", connectPoint)
                .add("macAddress", macAddress)
                .add("interfaceAddresses", interfaceAddresses)
                .toString();
    }
}
