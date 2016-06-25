/*
 * Copyright 2014-present Open Networking Laboratory
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
import com.google.common.collect.Maps;
import org.apache.commons.lang.ArrayUtils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.onlab.packet.LLDPOrganizationalTLV.OUI_LENGTH;
import static org.onlab.packet.LLDPOrganizationalTLV.SUBTYPE_LENGTH;

/**
 *  ONOS LLDP containing organizational TLV for ONOS device discovery.
 */
public class ONOSLLDP extends LLDP {

    public static final byte[] ONLAB_OUI = {(byte) 0xa4, 0x23, 0x05};
    public static final String DEFAULT_DEVICE = "INVALID";
    public static final String DEFAULT_NAME = "ONOS Discovery";

    // ON.Lab OUI (a42305) with multicast bit set
    public static final byte[] LLDP_ONLAB = {(byte) 0xa5, 0x23, 0x05, 0x00, 0x00, 0x01};
    public static final byte[] BDDP_MULTICAST = {(byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};

    protected static final byte NAME_SUBTYPE = 1;
    protected static final byte DEVICE_SUBTYPE = 2;
    protected static final byte DOMAIN_SUBTYPE = 3;

    private static final short NAME_LENGTH = OUI_LENGTH + SUBTYPE_LENGTH;
    private static final short DEVICE_LENGTH = OUI_LENGTH + SUBTYPE_LENGTH;
    private static final short DOMAIN_LENGTH = OUI_LENGTH + SUBTYPE_LENGTH;

    private final HashMap<Byte, LLDPOrganizationalTLV> opttlvs = Maps.newHashMap();

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

    // Only needs to be accessed from LinkProbeFactory.
    public ONOSLLDP(byte ... subtype) {
        super();
        for (byte st : subtype) {
            opttlvs.put(st, new LLDPOrganizationalTLV());
        }
        // guarantee the following (name and device) TLVs exist
        opttlvs.putIfAbsent(NAME_SUBTYPE, new LLDPOrganizationalTLV());
        opttlvs.putIfAbsent(DEVICE_SUBTYPE, new LLDPOrganizationalTLV());
        setName(DEFAULT_NAME);
        setDevice(DEFAULT_DEVICE);

        setOptionalTLVList(Lists.newArrayList(opttlvs.values()));
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
        LLDPOrganizationalTLV nametlv = opttlvs.get(NAME_SUBTYPE);
        nametlv.setLength((short) (name.length() + NAME_LENGTH));
        nametlv.setInfoString(name);
        nametlv.setSubType(NAME_SUBTYPE);
        nametlv.setOUI(ONLAB_OUI);
    }

    public void setDevice(String device) {
        LLDPOrganizationalTLV devicetlv = opttlvs.get(DEVICE_SUBTYPE);
        devicetlv.setInfoString(device);
        devicetlv.setLength((short) (device.length() + DEVICE_LENGTH));
        devicetlv.setSubType(DEVICE_SUBTYPE);
        devicetlv.setOUI(ONLAB_OUI);
    }

    public void setDomainInfo(String domainId) {
        LLDPOrganizationalTLV domaintlv = opttlvs.get(DOMAIN_SUBTYPE);
        if (domaintlv == null) {
            // maybe warn people not to set this if remote probes aren't.
            return;
        }
        domaintlv.setInfoString(domainId);
        domaintlv.setLength((short) (domainId.length() + DOMAIN_LENGTH));
        domaintlv.setSubType(DOMAIN_SUBTYPE);
        domaintlv.setOUI(ONLAB_OUI);
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

    /**
     * Gets the TLV associated with remote probing. This TLV will be null if
     * remote probing is disabled.
     *
     * @return A TLV containing domain ID, or null.
     */
    public LLDPOrganizationalTLV getDomainTLV() {
        for (LLDPTLV tlv : this.getOptionalTLVList()) {
            if (tlv.getType() == LLDPOrganizationalTLV.ORGANIZATIONAL_TLV_TYPE) {
                LLDPOrganizationalTLV orgTLV =  (LLDPOrganizationalTLV) tlv;
                if (orgTLV.getSubType() == DOMAIN_SUBTYPE) {
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

    public String getDomainString() {
        LLDPOrganizationalTLV tlv = getDomainTLV();
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
           ONOSLLDP onosLldp = new ONOSLLDP((LLDP) eth.getPayload());
           if (ONOSLLDP.DEFAULT_NAME.equals(onosLldp.getNameString())) {
               return onosLldp;
           }
        }
        return null;
    }

    /**
     * Creates a link probe for link discovery/verification.
     *
     * @param deviceId The device ID as a String
     * @param chassisId The chassis ID of the device
     * @param portNum Port number of port to send probe out of
     * @return ONOSLLDP probe message
     */
    public static ONOSLLDP onosLLDP(String deviceId, ChassisId chassisId, int portNum) {
        ONOSLLDP probe = new ONOSLLDP(NAME_SUBTYPE, DEVICE_SUBTYPE);
        probe.setPortId(portNum);
        probe.setDevice(deviceId);
        probe.setChassisId(chassisId);
        return probe;
    }
}
