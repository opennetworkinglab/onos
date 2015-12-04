/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onlab.packet;

import com.google.common.collect.Lists;
import org.apache.commons.lang.ArrayUtils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 *  ONOS LLDP containing organizational TLV for ONOS device dicovery.
 */
public class ONOSLLDP extends LLDP {

    public static final byte[] ONLAB_OUI = {(byte) 0xa4, 0x23, 0x05};
    public static final String DEFAULT_DEVICE = "INVALID";
    public static final String DEFAULT_NAME = "ONOS Discovery";

    public static final byte[] LLDP_NICIRA = {0x01, 0x23, 0x20, 0x00, 0x00,
            0x01};
    public static final byte[] LLDP_MULTICAST = {0x01, (byte) 0x80,
            (byte) 0xc2, 0x00, 0x00, 0x0e};
    public static final byte[] BDDP_MULTICAST = {(byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};

    private static final byte NAME_SUBTYPE = 1;
    private static final byte DEVICE_SUBTYPE = 2;
    private static final short NAME_LENGTH = 4; //1 for subtype + 3 for OUI
    private static final short DEVICE_LENGTH = 4; //1 for subtype + 3 for OUI
    private final LLDPOrganizationalTLV nameTLV = new LLDPOrganizationalTLV();
    private final LLDPOrganizationalTLV deviceTLV =  new LLDPOrganizationalTLV();

    // TLV constants: type, size and subtype
    // Organizationally specific TLV also have packet offset and contents of TLV
    // header
    private static final byte CHASSIS_TLV_TYPE = 1;
    private static final byte CHASSIS_TLV_SIZE = 7;
    private static final byte CHASSIS_TLV_SUBTYPE = 4;

    private static final byte PORT_TLV_TYPE = 2;
    private static final byte PORT_TLV_SIZE = 5;
    private static final byte PORT_TLV_SUBTYPE = 2;

    private static final byte TTL_TLV_TYPE = 3;


    private final byte[] ttlValue = new byte[] {0, 0x78};

    public ONOSLLDP() {
        super();
        setName(DEFAULT_NAME);
        setDevice(DEFAULT_DEVICE);
        setOptionalTLVList(Lists.<LLDPTLV>newArrayList(nameTLV, deviceTLV));
        setTtl(new LLDPTLV().setType(TTL_TLV_TYPE)
                       .setLength((short) ttlValue.length)
                       .setValue(ttlValue));

    }

    private ONOSLLDP(LLDP lldp) {
        this.portId = lldp.getPortId();
        this.chassisId = lldp.getChassisId();
        this.ttl = lldp.getTtl();
        this.optionalTLVList = lldp.getOptionalTLVList();
    }

    public void setName(String name) {
        nameTLV.setLength((short) (name.length() + NAME_LENGTH));
        nameTLV.setInfoString(name);
        nameTLV.setSubType(NAME_SUBTYPE);
        nameTLV.setOUI(ONLAB_OUI);
    }

    public void setDevice(String device) {
        deviceTLV.setInfoString(device);
        deviceTLV.setLength((short) (device.length() + DEVICE_LENGTH));
        deviceTLV.setSubType(DEVICE_SUBTYPE);
        deviceTLV.setOUI(ONLAB_OUI);
    }

    public void setChassisId(final ChassisId chassisId) {
        MacAddress chassisMac = MacAddress.valueOf(chassisId.value());
        byte[] chassis = ArrayUtils.addAll(new byte[] {CHASSIS_TLV_SUBTYPE},
                                           chassisMac.toBytes());

        LLDPTLV chassisTLV = new LLDPTLV();
        chassisTLV.setLength(CHASSIS_TLV_SIZE);
        chassisTLV.setType(CHASSIS_TLV_TYPE);
        chassisTLV.setValue(chassis);
        this.setChassisId(chassisTLV);
    }

    public void setPortId(final int portNumber) {
        byte[] port = ArrayUtils.addAll(new byte[] {PORT_TLV_SUBTYPE},
                                        ByteBuffer.allocate(4).putInt(portNumber).array());

        LLDPTLV portTLV = new LLDPTLV();
        portTLV.setLength(PORT_TLV_SIZE);
        portTLV.setType(PORT_TLV_TYPE);
        portTLV.setValue(port);
        this.setPortId(portTLV);
    }

    public LLDPOrganizationalTLV getNameTLV() {
        for (LLDPTLV tlv : this.getOptionalTLVList()) {
            if (tlv.getType() == LLDPOrganizationalTLV.ORGANIZATIONAL_TLV_TYPE) {
                LLDPOrganizationalTLV orgTLV =  (LLDPOrganizationalTLV) tlv;
                if (orgTLV.getSubType() == NAME_SUBTYPE) {
                    return orgTLV;
                }
            }
        }
        return null;
    }

    public LLDPOrganizationalTLV getDeviceTLV() {
        for (LLDPTLV tlv : this.getOptionalTLVList()) {
            if (tlv.getType() == LLDPOrganizationalTLV.ORGANIZATIONAL_TLV_TYPE) {
                LLDPOrganizationalTLV orgTLV =  (LLDPOrganizationalTLV) tlv;
                if (orgTLV.getSubType() == DEVICE_SUBTYPE) {
                    return orgTLV;
                }
            }
        }
        return null;
    }

    public String getNameString() {
        LLDPOrganizationalTLV tlv = getNameTLV();
        if (tlv != null) {
            return new String(tlv.getInfoString(), StandardCharsets.UTF_8);
        }
        return null;
    }

    public String getDeviceString() {
        LLDPOrganizationalTLV tlv = getDeviceTLV();
        if (tlv != null) {
            return new String(tlv.getInfoString(), StandardCharsets.UTF_8);
        }
        return null;
    }

    public Integer getPort() {
        ByteBuffer portBB = ByteBuffer.wrap(this.getPortId().getValue());
        portBB.position(1);
        return portBB.getInt();
    }

    /**
     * Given an ethernet packet, determines if this is an LLDP from
     * ONOS and returns the device the LLDP came from.
     * @param eth an ethernet packet
     * @return a the lldp packet or null
     */
    public static ONOSLLDP parseONOSLLDP(Ethernet eth) {
        if (eth.getEtherType() == Ethernet.TYPE_LLDP ||
                eth.getEtherType() == Ethernet.TYPE_BSN) {
           ONOSLLDP onosLldp = new ONOSLLDP((LLDP) eth.getPayload()); //(ONOSLLDP) eth.getPayload();
           if (ONOSLLDP.DEFAULT_NAME.equals(onosLldp.getNameString())) {
               return onosLldp;
           }
        }
        return null;
    }
}
