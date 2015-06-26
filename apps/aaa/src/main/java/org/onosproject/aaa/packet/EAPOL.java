/*
 *
 *  * Copyright 2015 AT&T Foundry
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.onosproject.aaa.packet;

import org.onlab.packet.BasePacket;
import org.onlab.packet.Deserializer;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPacket;
import org.onlab.packet.MacAddress;

import java.nio.ByteBuffer;

import static org.onlab.packet.PacketUtils.checkInput;

/**
 *
 */
public class EAPOL extends BasePacket {

    private byte version = 0x01;
    private byte eapolType;
    private short packetLength;

    /* EAPOL Packet Type */
    public static final byte EAPOL_PACKET = 0x0;
    public static final byte EAPOL_START  = 0x1;
    public static final byte EAPOL_LOGOFF = 0x2;
    public static final byte EAPOL_KEY    = 0x3;
    public static final byte EAPOL_ASF    = 0x4;

    public static final MacAddress PAE_GROUP_ADDR = MacAddress.valueOf(new byte[] {
            (byte) 0x01, (byte) 0x80, (byte) 0xc2, (byte) 0x00, (byte) 0x00, (byte) 0x03
    });


    /**
     * Get version.
     * @return version
     */
    public byte getVersion() {
        return this.version;
    }

    /**
     * Set version.
     * @param version EAPOL version
     * @return this
     */
    public EAPOL setVersion(final byte version) {
        this.version = version;
        return this;
    }

    /**
     * Get type.
     * @return EAPOL type
     */
    public byte getEapolType() {
        return this.eapolType;
    }

    /**
     * Set EAPOL type.
     * @param eapolType EAPOL type
     * @return this
     */
    public EAPOL setEapolType(final byte eapolType) {
        this.eapolType = eapolType;
        return this;
    }

    /**
     * Get packet length.
     * @return packet length
     */
    public short getPacketLength() {
        return this.packetLength;
    }

    /**
     * Set packet length.
     * @param packetLen packet length
     * @return this
     */
    public EAPOL setPacketLength(final short packetLen) {
        this.packetLength = packetLen;
        return this;
    }



    /**
     * Serializes the packet, based on the code/type using the payload
     * to compute its length.
     * @return this
     */
    @Override
    public byte[] serialize() {

        byte[] payloadData = null;

        if (this.payload != null) {
            this.payload.setParent(this);
            payloadData = this.payload.serialize();
        }

        //prepare the buffer to hold the version (1), packet type (1), packet length (2) and the eap payload.
        //if there is no payload, packet length is 0
        byte[] data = new byte[4 + this.packetLength];
        final ByteBuffer bb = ByteBuffer.wrap(data);
        bb.put(this.version);
        bb.put(this.eapolType);
        bb.putShort(this.packetLength);

        //put the EAP payload
        if (payloadData != null) {
            bb.put(payloadData);
        }

        return data;
    }



    @Override
    public int hashCode() {
        final int prime = 3889;
        int result = super.hashCode();
        result = prime * result + this.version;
        result = prime * result + this.eapolType;
        result = prime * result + this.packetLength;
        return result;
    }

    /**
     *
     * @param dstMac
     * @param srcMac
     * @param eapolType
     * @param eap
     * @return Ethernet frame
     */
    public static Ethernet buildEapolResponse(MacAddress dstMac, MacAddress srcMac,
                                              short vlan, byte eapolType, EAP eap) {

        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress(dstMac.toBytes());
        eth.setSourceMACAddress(srcMac.toBytes());
        eth.setEtherType(EAPEthernet.TYPE_PAE);
        if (vlan != Ethernet.VLAN_UNTAGGED) {
            eth.setVlanID(vlan);
        }
        //eapol header
        EAPOL eapol = new EAPOL();
        eapol.setEapolType(eapolType);
        eapol.setPacketLength(eap.getLength());

        //eap part
        eapol.setPayload(eap);

        eth.setPayload(eapol);
        eth.setPad(true);
        return eth;
    }

    public static Deserializer<EAPOL> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, 0);

            EAPOL eapol = new EAPOL();
            final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            eapol.setVersion(bb.get());
            eapol.setEapolType(bb.get());
            eapol.setPacketLength(bb.getShort());

            if (eapol.packetLength > 0) {
                //deserialize the EAP Payload
                eapol.payload = new EAP();

                eapol.payload = eapol.payload.deserialize(data, bb.position(), length - 4);
                eapol.payload.setParent(eapol);
            }
            return eapol;
        };
    }

    @Override
    public IPacket deserialize(final byte[] data, final int offset,
                               final int length) {
        final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);


        //deserialize the EAPOL header
        this.version = bb.get();
        this.eapolType = bb.get();
        this.packetLength = bb.getShort();

        if (this.packetLength > 0) {
            //deserialize the EAP Payload
            this.payload = new EAP();

            this.payload = this.payload.deserialize(data, bb.position(), length - 4);
            this.payload.setParent(this);
        }


        return this;
    }


}

