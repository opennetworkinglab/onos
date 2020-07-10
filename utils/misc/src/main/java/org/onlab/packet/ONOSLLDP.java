/*
 * Copyright 2014-present Open Networking Foundation
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
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.onlab.packet.LLDPOrganizationalTLV.OUI_LENGTH;
import static org.onlab.packet.LLDPOrganizationalTLV.SUBTYPE_LENGTH;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *  ONOS LLDP containing organizational TLV for ONOS device discovery.
 */
public class ONOSLLDP extends LLDP {

    private static final Logger log = getLogger(ONOSLLDP.class);

    public static final String DEFAULT_DEVICE = "INVALID";
    public static final String DEFAULT_NAME = "ONOS Discovery";

    protected static final byte NAME_SUBTYPE = 1;
    protected static final byte DEVICE_SUBTYPE = 2;
    protected static final byte DOMAIN_SUBTYPE = 3;
    protected static final byte TIMESTAMP_SUBTYPE = 4;
    protected static final byte SIG_SUBTYPE = 5;

    private static final short NAME_LENGTH = OUI_LENGTH + SUBTYPE_LENGTH;
    private static final short DEVICE_LENGTH = OUI_LENGTH + SUBTYPE_LENGTH;
    private static final short DOMAIN_LENGTH = OUI_LENGTH + SUBTYPE_LENGTH;
    private static final short TIMESTAMP_LENGTH = OUI_LENGTH + SUBTYPE_LENGTH;
    private static final short SIG_LENGTH = OUI_LENGTH + SUBTYPE_LENGTH;

    private final HashMap<Byte, LLDPOrganizationalTLV> opttlvs = Maps.newHashMap();

    // TLV constants: type, size and subtype
    // Organizationally specific TLV also have packet offset and contents of TLV
    // header
    private static final byte CHASSIS_TLV_TYPE = 1;
    private static final byte CHASSIS_TLV_SIZE = 7;
    private static final byte CHASSIS_TLV_SUBTYPE = 4;

    private static final byte TTL_TLV_TYPE = 3;
    private static final byte PORT_DESC_TLV_TYPE = 4;

    private final byte[] ttlValue = new byte[] {0, 0x78};

    // Only needs to be accessed from LinkProbeFactory.
    public ONOSLLDP(byte... subtype) {
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
        nametlv.setOUI(MacAddress.ONOS.oui());
    }

    public void setDevice(String device) {
        LLDPOrganizationalTLV devicetlv = opttlvs.get(DEVICE_SUBTYPE);
        devicetlv.setInfoString(device);
        devicetlv.setLength((short) (device.length() + DEVICE_LENGTH));
        devicetlv.setSubType(DEVICE_SUBTYPE);
        devicetlv.setOUI(MacAddress.ONOS.oui());
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
        domaintlv.setOUI(MacAddress.ONOS.oui());
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
        byte[] port = ArrayUtils.addAll(new byte[] {PORT_TLV_COMPONENT_SUBTYPE},
                String.valueOf(portNumber).getBytes(StandardCharsets.UTF_8));

        LLDPTLV portTLV = new LLDPTLV();
        portTLV.setLength((short) port.length);
        portTLV.setType(PORT_TLV_TYPE);
        portTLV.setValue(port);
        this.setPortId(portTLV);
    }

    public void setPortName(final String portName) {
        byte[] port = ArrayUtils.addAll(new byte[] {PORT_TLV_INTERFACE_NAME_SUBTYPE},
                portName.getBytes(StandardCharsets.UTF_8));

        LLDPTLV portTLV = new LLDPTLV();
        portTLV.setLength((short) port.length);
        portTLV.setType(PORT_TLV_TYPE);
        portTLV.setValue(port);
        this.setPortId(portTLV);
    }

    public void setTimestamp(long timestamp) {
        LLDPOrganizationalTLV tmtlv = opttlvs.get(TIMESTAMP_SUBTYPE);
        if (tmtlv == null) {
            return;
        }
        tmtlv.setInfoString(ByteBuffer.allocate(8).putLong(timestamp).array());
        tmtlv.setLength((short) (8 + TIMESTAMP_LENGTH));
        tmtlv.setSubType(TIMESTAMP_SUBTYPE);
        tmtlv.setOUI(MacAddress.ONOS.oui());
    }

    public void setSig(byte[] sig) {
        LLDPOrganizationalTLV sigtlv = opttlvs.get(SIG_SUBTYPE);
        if (sigtlv == null) {
            return;
        }
        sigtlv.setInfoString(sig);
        sigtlv.setLength((short) (sig.length + SIG_LENGTH));
        sigtlv.setSubType(SIG_SUBTYPE);
        sigtlv.setOUI(MacAddress.ONOS.oui());
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
                LLDPOrganizationalTLV orgTLV = (LLDPOrganizationalTLV) tlv;
                if (orgTLV.getSubType() == DEVICE_SUBTYPE) {
                    return orgTLV;
                }
            }
        }
        return null;
    }

    public LLDPOrganizationalTLV getTimestampTLV() {
        for (LLDPTLV tlv : this.getOptionalTLVList()) {
            if (tlv.getType() == LLDPOrganizationalTLV.ORGANIZATIONAL_TLV_TYPE) {
                LLDPOrganizationalTLV orgTLV =  (LLDPOrganizationalTLV) tlv;
                if (orgTLV.getSubType() == TIMESTAMP_SUBTYPE) {
                    return orgTLV;
                }
            }
        }
        return null;
    }

    public LLDPOrganizationalTLV getSigTLV() {
        for (LLDPTLV tlv : this.getOptionalTLVList()) {
            if (tlv.getType() == LLDPOrganizationalTLV.ORGANIZATIONAL_TLV_TYPE) {
                LLDPOrganizationalTLV orgTLV = (LLDPOrganizationalTLV) tlv;
                if (orgTLV.getSubType() == SIG_SUBTYPE) {
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

    public LLDPTLV getPortDescTLV() {
        for (LLDPTLV tlv : this.getOptionalTLVList()) {
            if (tlv.getType() == PORT_DESC_TLV_TYPE) {
                return tlv;
            }
        }

        log.debug("Cannot find the port description tlv type.");
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

    public String getPortDescString() {
        LLDPTLV tlv = getPortDescTLV();
        if (tlv != null) {
            return new String(tlv.getValue(), StandardCharsets.UTF_8);
        }
        return null;
    }

    public Integer getPort() {
        ByteBuffer portBB = ByteBuffer.wrap(this.getPortId().getValue());
        byte type = portBB.get();

        if (type == PORT_TLV_COMPONENT_SUBTYPE) {
            return Integer.parseInt(new String(portBB.array(),
                    portBB.position(), portBB.remaining(), StandardCharsets.UTF_8));
        } else {
            return -1;
        }
    }

    public String getPortNameString() {
        ByteBuffer portBB = ByteBuffer.wrap(this.getPortId().getValue());
        byte type = portBB.get();

        if (type == PORT_TLV_INTERFACE_NAME_SUBTYPE) {
            return new String(portBB.array(), portBB.position(), portBB.remaining(), StandardCharsets.UTF_8);
        } else {
            return null;
        }
    }

    public MacAddress getChassisIdByMac() {
        ByteBuffer portBB = ByteBuffer.wrap(this.getChassisId().getValue());
        byte type = portBB.get();

        if (type == CHASSIS_TLV_SUBTYPE) {
            byte[] bytes = new byte[portBB.remaining()];

            System.arraycopy(portBB.array(), portBB.position(), bytes, 0, MacAddress.MAC_ADDRESS_LENGTH);

            return new MacAddress(bytes);
        } else {
            return MacAddress.NONE;
        }
    }

    public short getTtlBySeconds() {
        ByteBuffer portBB = ByteBuffer.wrap(this.getTtl().getValue());

        return portBB.getShort();
    }

    public long getTimestamp() {
        LLDPOrganizationalTLV tlv = getTimestampTLV();
        if (tlv != null) {
            ByteBuffer b = ByteBuffer.allocate(8).put(tlv.getInfoString());
            b.flip();
            return b.getLong();
        }
        return 0;
    }

    public byte[] getSig() {
        LLDPOrganizationalTLV tlv = getSigTLV();
        if (tlv != null) {
            return tlv.getInfoString();
        }
        return null;
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
     * Given an ethernet packet, returns the device the LLDP came from.
     * @param eth an ethernet packet
     * @return a the lldp packet or null
     */
    public static ONOSLLDP parseLLDP(Ethernet eth) {
        if (eth.getEtherType() == Ethernet.TYPE_LLDP ||
                eth.getEtherType() == Ethernet.TYPE_BSN) {

            return new ONOSLLDP((LLDP) eth.getPayload());
        }

        log.error("Packet is not the LLDP or BSN.");
        return null;
    }

    /**
     * Creates a link probe for link discovery/verification.
     * @deprecated since 1.15. Insecure, do not use.
     *
     * @param deviceId The device ID as a String
     * @param chassisId The chassis ID of the device
     * @param portNum Port number of port to send probe out of
     * @return ONOSLLDP probe message
     */
   @Deprecated
    public static ONOSLLDP onosLLDP(String deviceId, ChassisId chassisId, int portNum) {
        ONOSLLDP probe = new ONOSLLDP(NAME_SUBTYPE, DEVICE_SUBTYPE);
        probe.setPortId(portNum);
        probe.setDevice(deviceId);
        probe.setChassisId(chassisId);
        return probe;
    }

    /**
     * Creates a link probe for link discovery/verification.
     *
     * @param deviceId The device ID as a String
     * @param chassisId The chassis ID of the device
     * @param portNum Port number of port to send probe out of
     * @param secret LLDP secret
     * @return ONOSLLDP probe message
     */
    public static ONOSLLDP onosSecureLLDP(String deviceId, ChassisId chassisId, int portNum, String secret) {
        ONOSLLDP probe = null;
        if (secret == null) {
            probe = new ONOSLLDP(NAME_SUBTYPE, DEVICE_SUBTYPE);
        } else {
            probe = new ONOSLLDP(NAME_SUBTYPE, DEVICE_SUBTYPE, TIMESTAMP_SUBTYPE, SIG_SUBTYPE);
        }
        probe.setPortId(portNum);
        probe.setDevice(deviceId);
        probe.setChassisId(chassisId);

        if (secret != null) {
            /* Secure Mode */
            long ts = System.currentTimeMillis();
            probe.setTimestamp(ts);
            byte[] sig = createSig(deviceId, portNum, ts, secret);
            if (sig == null) {
                return null;
            }
            probe.setSig(sig);
            sig = null;
        }
        return probe;
    }

    /**
     * Creates a link probe for link discovery/verification.
     * @deprecated since 1.15. Insecure, do not use.
     *
     * @param deviceId The device ID as a String
     * @param chassisId The chassis ID of the device
     * @param portNum Port number of port to send probe out of
     * @param portDesc Port description of port to send probe out of
     * @return ONOSLLDP probe message
     */
    @Deprecated
    public static ONOSLLDP onosLLDP(String deviceId, ChassisId chassisId, int portNum, String portDesc) {
        ONOSLLDP probe = onosLLDP(deviceId, chassisId, portNum);
        addPortDesc(probe, portDesc);
        return probe;
    }

    /**
     * Creates a link probe for link discovery/verification.
     *
     * @param deviceId  The device ID as a String
     * @param chassisId The chassis ID of the device
     * @param portNum   Port number of port to send probe out of
     * @param portDesc  Port description of port to send probe out of
     * @param secret    LLDP secret
     * @return ONOSLLDP probe message
     */
    public static ONOSLLDP onosSecureLLDP(String deviceId, ChassisId chassisId, int portNum, String portDesc,
                                          String secret) {
        ONOSLLDP probe = onosSecureLLDP(deviceId, chassisId, portNum, secret);
        addPortDesc(probe, portDesc);
        return probe;
    }

    private static void addPortDesc(ONOSLLDP probe, String portDesc) {
        if (portDesc != null && !portDesc.isEmpty()) {
            byte[] bPortDesc = portDesc.getBytes(StandardCharsets.UTF_8);

            if (bPortDesc.length > LLDPTLV.MAX_LENGTH) {
                bPortDesc = Arrays.copyOf(bPortDesc, LLDPTLV.MAX_LENGTH);
            }
            LLDPTLV portDescTlv = new LLDPTLV()
                    .setType(PORT_DESC_TLV_TYPE)
                    .setLength((short) bPortDesc.length)
                    .setValue(bPortDesc);
            probe.addOptionalTLV(portDescTlv);
        }
    }

    private static byte[] createSig(String deviceId, int portNum, long timestamp, String secret) {
        byte[] pnb = ByteBuffer.allocate(8).putLong(portNum).array();
        byte[] tmb = ByteBuffer.allocate(8).putLong(timestamp).array();

        try {
            SecretKeySpec signingKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            mac.update(deviceId.getBytes());
            mac.update(pnb);
            mac.update(tmb);
            byte[] sig = mac.doFinal();
            return sig;
        } catch (NoSuchAlgorithmException e) {
            return null;
        } catch (InvalidKeyException e) {
            return null;
        }
    }

    private static boolean verifySig(byte[] sig, String deviceId, int portNum, long timestamp, String secret) {
        byte[] nsig = createSig(deviceId, portNum, timestamp, secret);
        if (nsig == null) {
            return false;
        }

        if (!ArrayUtils.isSameLength(nsig, sig)) {
            return false;
        }

        boolean fail = false;
        for (int i = 0; i < nsig.length; i++) {
            if (sig[i] != nsig[i]) {
                fail = true;
            }
        }
        if (fail) {
            return false;
        }
        return true;
    }

    public static boolean verify(ONOSLLDP probe, String secret, long maxDelay) {
        if (secret == null) {
            return true;
        }

        String deviceId = probe.getDeviceString();
        int portNum = probe.getPort();
        long timestamp = probe.getTimestamp();
        byte[] sig = probe.getSig();

        if (deviceId == null || sig == null) {
            return false;
        }

        if (timestamp + maxDelay <= System.currentTimeMillis() ||
                timestamp > System.currentTimeMillis()) {
            return false;
        }

        return verifySig(sig, deviceId, portNum, timestamp, secret);
    }

}
