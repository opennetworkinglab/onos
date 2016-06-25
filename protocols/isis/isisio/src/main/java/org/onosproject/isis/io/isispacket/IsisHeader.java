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
package org.onosproject.isis.io.isispacket;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onlab.packet.MacAddress;
import org.onosproject.isis.controller.IsisMessage;
import org.onosproject.isis.controller.IsisPduType;

/**
 * Representation of ISIS message header.
 */
public class IsisHeader implements IsisMessage {

    private MacAddress sourceMac;
    private int interfaceIndex;
    private MacAddress interfaceMac;
    private int isisPduType;
    private byte irpDiscriminator;
    private byte pduHeaderLength;
    private byte version2;
    private byte idLength;
    private byte version;
    private byte reserved;
    private byte maximumAreaAddresses;

    /**
     * Returns the interface index on which the message received.
     *
     * @return interface index on which the message received
     */
    public int interfaceIndex() {
        return interfaceIndex;
    }

    /**
     * Sets the interface index on which the message received.
     *
     * @param interfaceIndex interface index on which the message received
     */
    public void setInterfaceIndex(int interfaceIndex) {
        this.interfaceIndex = interfaceIndex;
    }

    /**
     * Returns the interface mac address on which the message received.
     *
     * @return interface mac address on which the message received
     */
    public MacAddress interfaceMac() {
        return interfaceMac;
    }

    /**
     * Returns the mac address of the message sender.
     *
     * @return mac address of the message sender
     */
    public MacAddress sourceMac() {
        return sourceMac;
    }

    /**
     * Sets the mac address of the message sender.
     *
     * @param sourceMac mac address of the message sender
     */
    public void setSourceMac(MacAddress sourceMac) {
        this.sourceMac = sourceMac;
    }

    /**
     * Sets the interface mac address on which the message received.
     *
     * @param interfaceMac mac address on which the message received
     */
    public void setInterfaceMac(MacAddress interfaceMac) {
        this.interfaceMac = interfaceMac;
    }

    /**
     * Returns the version of TLV header.
     *
     * @return version version of TLV header
     */
    public byte version2() {
        return version2;
    }

    /**
     * Sets the version of TLV header.
     *
     * @param version2 version of TLV header
     */
    public void setVersion2(byte version2) {
        this.version2 = version2;
    }

    /**
     * Returns maximum area address.
     *
     * @return maximum area address
     */
    public byte maximumAreaAddresses() {
        return maximumAreaAddresses;
    }

    /**
     * Sets maximum area address.
     *
     * @param maximumAreaAddresses maximum area address
     */
    public void setMaximumAreaAddresses(byte maximumAreaAddresses) {
        this.maximumAreaAddresses = maximumAreaAddresses;
    }

    /**
     * Returns reserved field value on which data received.
     *
     * @return reserved
     */
    public byte reserved() {
        return reserved;
    }

    /**
     * Sets reserved.
     *
     * @param reserved reserved
     */
    public void setReserved(byte reserved) {
        this.reserved = reserved;
    }

    /**
     * Returns version.
     *
     * @return version
     */
    public byte version() {
        return version;
    }

    /**
     * Returns ID length.
     *
     * @return ID length
     */
    public byte idLength() {
        return idLength;
    }

    /**
     * Sets ID length.
     *
     * @param idLength ID length
     */
    public void setIdLength(byte idLength) {
        this.idLength = idLength;
    }

    /**
     * Returns the PDU type.
     *
     * @return PDU type
     */
    public int pduType() {

        return this.isisPduType;
    }

    /**
     * Sets PDU type.
     *
     * @param isisPduType PDU type
     */
    public void setIsisPduType(int isisPduType) {
        this.isisPduType = isisPduType;
    }

    /**
     * Sets protocol ID.
     *
     * @param version protocol ID
     */
    public void setVersion(byte version) {
        this.version = version;
    }

    /**
     * Returns length indicator.
     *
     * @return length indicator
     */
    public byte pduHeaderLength() {
        return pduHeaderLength;
    }

    /**
     * Sets length indicator.
     *
     * @param pduHeaderLength length indicator
     */
    public void setPduHeaderLength(byte pduHeaderLength) {
        this.pduHeaderLength = pduHeaderLength;
    }

    /**
     * Returns IRP discriminator.
     *
     * @return IRP discriminator
     */
    public byte irpDiscriminator() {
        return irpDiscriminator;
    }

    /**
     * Sets IRP discriminator.
     *
     * @param irpDiscriminator IRP discriminator
     */
    public void setIrpDiscriminator(byte irpDiscriminator) {

        this.irpDiscriminator = irpDiscriminator;
    }

    @Override
    public IsisPduType isisPduType() {

        return IsisPduType.get(this.isisPduType);
    }

    @Override
    public void readFrom(ChannelBuffer channelBuffer) {
        //implemented in the sub classes
    }

    @Override
    public byte[] asBytes() {
        return null;
    }

    /**
     * Populates ISIS header.
     *
     * @param isisHeader ISIS header
     */
    public void populateHeader(IsisHeader isisHeader) {
        this.setIrpDiscriminator(isisHeader.irpDiscriminator());
        this.setPduHeaderLength(isisHeader.pduHeaderLength());
        this.setVersion(isisHeader.version());
        this.setIdLength(isisHeader.idLength());
        this.setIsisPduType(isisHeader.pduType());
        this.setVersion2(isisHeader.version2());
        this.setReserved(isisHeader.reserved());
        this.setMaximumAreaAddresses(isisHeader.maximumAreaAddresses());
    }
}