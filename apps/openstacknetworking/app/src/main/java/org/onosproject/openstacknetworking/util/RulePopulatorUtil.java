/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.util;

import com.google.common.collect.Maps;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.ExtensionSelectorResolver;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.ExtensionSelectorType;
import org.onosproject.net.flow.instructions.ExtensionPropertyException;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupDescription;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_LOAD;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ARP_SHA_TO_THA;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ARP_SPA_TO_TPA;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ETH_SRC_TO_DST;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_IP_SRC_TO_DST;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_POP_NSH;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_PUSH_NSH;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_TUNNEL_DST;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides common methods to help populating flow rules for SONA applications.
 */
public final class RulePopulatorUtil {

    private static final Logger log = getLogger(RulePopulatorUtil.class);

    private static final String TUNNEL_DST = "tunnelDst";
    private static final String CT_FLAGS = "flags";
    private static final String CT_ZONE = "zone";
    private static final String CT_TABLE = "recircTable";
    private static final String CT_STATE = "ctState";
    private static final String CT_STATE_MASK = "ctStateMask";
    private static final String CT_PRESENT_FLAGS = "presentFlags";
    private static final String CT_IPADDRESS_MIN = "ipAddressMin";
    private static final String CT_IPADDRESS_MAX = "ipAddressMax";
    private static final String CT_PORT_MIN = "portMin";
    private static final String CT_PORT_MAX = "portMax";
    private static final String CT_NESTED_ACTIONS = "nestedActions";

    public static final int CT_NAT_SRC_FLAG = 0;
    public static final int CT_NAT_DST_FLAG = 1;
    public static final int CT_NAT_PERSISTENT_FLAG = 2;
    public static final int CT_NAT_PROTO_HASH_FLAG = 3;
    public static final int CT_NAT_PROTO_RANDOM_FLAG = 4;

    private static final int ADDRESS_V4_MIN_FLAG = 0;
    private static final int ADDRESS_V4_MAX_FLAG = 1;
    private static final int ADDRESS_V6_MIN_FLAG = 2;
    private static final int ADDRESS_V6_MAX_FLAG = 3;
    private static final int PORT_MIN_FLAG = 4;
    private static final int PORT_MAX_FLAG = 5;

    private static final String STR_ZERO = "0";
    private static final String STR_ONE = "1";
    private static final String STR_PADDING = "0000000000000000";
    private static final int MASK_BEGIN_IDX = 0;
    private static final int MASK_MAX_IDX = 16;
    private static final int MASK_RADIX = 2;
    private static final int PORT_RADIX = 16;

    // Refer to http://openvswitch.org/support/dist-docs/ovs-fields.7.txt for the values
    public static final long CT_STATE_NONE = 0;
    public static final long CT_STATE_NEW = 0x01;
    public static final long CT_STATE_EST = 0x02;
    public static final long CT_STATE_NOT_TRK = 0x20;
    public static final long CT_STATE_TRK = 0x20;

    private static final String OFF_SET_N_BITS = "ofsNbits";
    private static final String DESTINATION = "dst";
    private static final String VALUE = "value";

    private static final int OFF_SET_BIT = 0;
    private static final int REMAINDER_BIT = 8;

    // layer 3 nicira fields
    public static final int NXM_OF_IP_SRC = 0x00000e04;
    public static final int NXM_OF_IP_DST = 0x00001004;
    public static final int NXM_OF_IP_PROT = 0x00000c01;

    public static final int NXM_NX_IP_TTL = 0x00013a01;
    public static final int NXM_NX_IP_FRAG = 0x00013401;
    public static final int NXM_OF_ARP_OP = 0x00001e02;
    public static final int NXM_OF_ARP_SPA = 0x00002004;
    public static final int NXM_OF_ARP_TPA = 0x00002204;
    public static final int NXM_NX_ARP_SHA = 0x00012206;
    public static final int NXM_NX_ARP_THA = 0x00012406;

    // layer 4 nicira fields
    public static final int NXM_OF_TCP_SRC = 0x00001202;
    public static final int NXM_OF_TCP_DST = 0x00001402;
    public static final int NXM_NX_TCP_FLAGS = 0x00014402;
    public static final int NXM_OF_UDP_SRC = 0x00001602;
    public static final int NXM_OF_UDP_DST = 0x00001802;

    public static final int NXM_OF_ICMP_TYPE = 0x00001a01;
    public static final int NXM_OF_ICMP_CODE = 0x00001c01;

    private RulePopulatorUtil() {
    }

    /**
     * Returns a builder for OVS Connection Tracking feature actions.
     *
     * @param ds DriverService
     * @param id DeviceId
     * @return a builder for OVS Connection Tracking feature actions
     */
    public static NiciraConnTrackTreatmentBuilder
                    niciraConnTrackTreatmentBuilder(DriverService ds, DeviceId id) {
        return new NiciraConnTrackTreatmentBuilder(ds, id);
    }

    /**
     * Returns tunnel destination extension treatment object.
     *
     * @param deviceService driver service
     * @param deviceId device id to apply this treatment
     * @param remoteIp tunnel destination ip address
     * @return extension treatment
     */
    public static ExtensionTreatment buildExtension(DeviceService deviceService,
                                                    DeviceId deviceId,
                                                    Ip4Address remoteIp) {
        Device device = deviceService.getDevice(deviceId);
        if (!checkTreatmentResolver(device)) {
            return null;
        }

        if (device == null) {
            return null;
        }

        ExtensionTreatmentResolver resolver = device.as(ExtensionTreatmentResolver.class);
        ExtensionTreatment treatment =
                resolver.getExtensionInstruction(NICIRA_SET_TUNNEL_DST.type());
        try {
            treatment.setPropertyValue(TUNNEL_DST, remoteIp);
            return treatment;
        } catch (ExtensionPropertyException e) {
            log.warn("Failed to get tunnelDst extension treatment for {} " +
                    "because of {}", deviceId, e);
            return null;
        }
    }

    /**
     * Builds OVS ConnTrack matches.
     *
     * @param driverService driver service
     * @param deviceId device ID
     * @param ctState connection tracking sate masking value
     * @param ctSateMask connection tracking sate masking value
     * @return OVS ConnTrack extension match
     */
    public static ExtensionSelector buildCtExtensionSelector(DriverService driverService,
                                                             DeviceId deviceId,
                                                             long ctState,
                                                             long ctSateMask) {
        DriverHandler handler = driverService.createHandler(deviceId);
        ExtensionSelectorResolver esr = handler.behaviour(ExtensionSelectorResolver.class);

        ExtensionSelector extensionSelector = esr.getExtensionSelector(
                ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_CONNTRACK_STATE.type());
        try {
            extensionSelector.setPropertyValue(CT_STATE, ctState);
            extensionSelector.setPropertyValue(CT_STATE_MASK, ctSateMask);
        } catch (Exception e) {
            log.error("Failed to set nicira match CT state because of {}", e);
            return null;
        }

        return extensionSelector;
    }

    /**
     * Returns the nicira load extension treatment.
     *
     * @param device        device instance
     * @param field         field code
     * @param value         value to load
     * @return load extension treatment
     */
    public static ExtensionTreatment buildLoadExtension(Device device,
                                                        long field,
                                                        long value) {
        if (!checkTreatmentResolver(device)) {
            return null;
        }

        ExtensionTreatmentResolver resolver = device.as(ExtensionTreatmentResolver.class);
        ExtensionTreatment treatment =
                resolver.getExtensionInstruction(NICIRA_LOAD.type());

        int ofsNbits = OFF_SET_BIT << 6 | (REMAINDER_BIT - 1);

        try {
            treatment.setPropertyValue(OFF_SET_N_BITS, ofsNbits);
            treatment.setPropertyValue(DESTINATION, field);
            treatment.setPropertyValue(VALUE, value);
            return treatment;
        } catch (ExtensionPropertyException e) {
            log.error("Failed to set nicira load extension treatment for {}",
                    device.id());
            return null;
        }
    }

    /**
     * Returns the group bucket with given traffic treatment and group type.
     *
     * @param treatment     traffic treatment
     * @param type          group type
     * @param weight        weight (only for select type)
     * @return group bucket
     */
    public static GroupBucket buildGroupBucket(TrafficTreatment treatment,
                                               GroupDescription.Type type, short weight) {
        switch (type) {
            case ALL:
                return DefaultGroupBucket.createAllGroupBucket(treatment);
            case SELECT:
                if (weight == -1) {
                    return DefaultGroupBucket.createSelectGroupBucket(treatment);
                } else {
                    return DefaultGroupBucket.createSelectGroupBucket(treatment, weight);
                }
            case INDIRECT:
                return DefaultGroupBucket.createIndirectGroupBucket(treatment);
            default:
                return null;
        }
    }

    /**
     * Returns the nicira push extension treatment.
     *
     * @param device        device instance
     * @return push extension treatment
     */
    public static ExtensionTreatment buildPushExtension(Device device) {
        if (!checkTreatmentResolver(device)) {
            return null;
        }

        ExtensionTreatmentResolver resolver = device.as(ExtensionTreatmentResolver.class);
        return resolver.getExtensionInstruction(NICIRA_PUSH_NSH.type());
    }

    /**
     * Returns the nicira pop extension treatment.
     *
     * @param device        device instance
     * @return pop extension treatment
     */
    public static ExtensionTreatment buildPopExtension(Device device) {
        if (!checkTreatmentResolver(device)) {
            return null;
        }

        ExtensionTreatmentResolver resolver = device.as(ExtensionTreatmentResolver.class);
        return resolver.getExtensionInstruction(NICIRA_POP_NSH.type());
    }

    /**
     * Returns the nicira move source MAC to destination MAC extension treatment.
     *
     * @param device        device instance
     * @return move extension treatment
     */
    public static ExtensionTreatment buildMoveEthSrcToDstExtension(Device device) {
        if (!checkTreatmentResolver(device)) {
            return null;
        }

        ExtensionTreatmentResolver resolver = device.as(ExtensionTreatmentResolver.class);
        return resolver.getExtensionInstruction(NICIRA_MOV_ETH_SRC_TO_DST.type());
    }

    /**
     * Returns the nicira move source IP to destination IP extension treatment.
     *
     * @param device        device instance
     * @return move extension treatment
     */
    public static ExtensionTreatment buildMoveIpSrcToDstExtension(Device device) {
        if (!checkTreatmentResolver(device)) {
            return null;
        }

        ExtensionTreatmentResolver resolver = device.as(ExtensionTreatmentResolver.class);
        return resolver.getExtensionInstruction(NICIRA_MOV_IP_SRC_TO_DST.type());
    }

    /**
     * Returns the nicira move ARP SHA to THA extension treatment.
     *
     * @param device        device instance
     * @return move extension treatment
     */
    public static ExtensionTreatment buildMoveArpShaToThaExtension(Device device) {
        if (!checkTreatmentResolver(device)) {
            return null;
        }

        ExtensionTreatmentResolver resolver = device.as(ExtensionTreatmentResolver.class);
        return resolver.getExtensionInstruction(NICIRA_MOV_ARP_SHA_TO_THA.type());
    }

    /**
     * Returns the nicira move ARP SPA to TPA extension treatment.
     *
     * @param device        device instance
     * @return move extension treatment
     */
    public static ExtensionTreatment buildMoveArpSpaToTpaExtension(Device device) {
        if (!checkTreatmentResolver(device)) {
            return null;
        }

        ExtensionTreatmentResolver resolver = device.as(ExtensionTreatmentResolver.class);
        return resolver.getExtensionInstruction(NICIRA_MOV_ARP_SPA_TO_TPA.type());
    }

    /**
     * Computes ConnTack State flag values.
     *
     * @param isTracking true for +trk, false for -trk
     * @param isNew true for +new, false for nothing
     * @param isEstablished true for +est, false for nothing
     * @return ConnTrack State flags
     */
    public static long computeCtStateFlag(boolean isTracking,
                                          boolean isNew,
                                          boolean isEstablished) {
        long ctMaskFlag = 0x00;

        if (isTracking) {
            ctMaskFlag = ctMaskFlag | CT_STATE_TRK;
        }

        if (isNew) {
            ctMaskFlag = ctMaskFlag | CT_STATE_TRK;
            ctMaskFlag = ctMaskFlag | CT_STATE_NEW;
        }

        if (isEstablished) {
            ctMaskFlag = ctMaskFlag | CT_STATE_TRK;
            ctMaskFlag = ctMaskFlag | CT_STATE_EST;
        }

        return ctMaskFlag;
    }

    /**
     * Computes ConnTrack State mask values.
     *
     * @param isTracking true for setting +trk/-trk value, false for otherwise
     * @param isNew true for setting +new value, false for otherwise
     * @param isEstablished true for setting +est value, false for otherwise
     * @return ConnTrack State Mask value
     */
    public static long computeCtMaskFlag(boolean isTracking,
                                         boolean isNew,
                                         boolean isEstablished) {
        long ctMaskFlag = 0x00;

        if (isTracking) {
            ctMaskFlag = ctMaskFlag | CT_STATE_TRK;
        }

        if (isNew) {
            ctMaskFlag = ctMaskFlag | CT_STATE_TRK;
            ctMaskFlag = ctMaskFlag | CT_STATE_NEW;
        }

        if (isEstablished) {
            ctMaskFlag = ctMaskFlag | CT_STATE_TRK;
            ctMaskFlag = ctMaskFlag | CT_STATE_EST;
        }

        return ctMaskFlag;
    }

    /**
     * Computes port and port mask value from port min/max values.
     *
     * @param portMin port min value
     * @param portMax port max value
     * @return Port Mask value
     */
    public static Map<TpPort, TpPort> buildPortRangeMatches(int portMin, int portMax) {

        boolean processing = true;
        int start = portMin;
        Map<TpPort, TpPort> portMaskMap = Maps.newHashMap();
        while (processing) {
            String minStr = Integer.toBinaryString(start);
            String binStrMinPadded = STR_PADDING.substring(minStr.length()) + minStr;

            int mask = testMasks(binStrMinPadded, start, portMax);
            int maskStart = binLower(binStrMinPadded, mask);
            int maskEnd = binHigher(binStrMinPadded, mask);

            log.debug("start : {} port/mask = {} / {} ", start, getMask(mask), maskStart);
            portMaskMap.put(TpPort.tpPort(maskStart), TpPort.tpPort(
                    Integer.parseInt(Objects.requireNonNull(getMask(mask)), PORT_RADIX)));

            start = maskEnd + 1;
            if (start > portMax) {
                processing = false;
            }
        }

        return portMaskMap;
    }

    private static int binLower(String binStr, int bits) {
        StringBuilder outBin = new StringBuilder(
                binStr.substring(MASK_BEGIN_IDX, MASK_MAX_IDX - bits));
        for (int i = 0; i < bits; i++) {
            outBin.append(STR_ZERO);
        }

        return Integer.parseInt(outBin.toString(), MASK_RADIX);
    }

    private static int binHigher(String binStr, int bits) {
        StringBuilder outBin = new StringBuilder(
                binStr.substring(MASK_BEGIN_IDX, MASK_MAX_IDX - bits));
        for (int i = 0; i < bits; i++) {
            outBin.append(STR_ONE);
        }

        return Integer.parseInt(outBin.toString(), MASK_RADIX);
    }

    private static int testMasks(String binStr, int start, int end) {
        int mask = MASK_BEGIN_IDX;
        for (; mask <= MASK_MAX_IDX; mask++) {
            int maskStart = binLower(binStr, mask);
            int maskEnd = binHigher(binStr, mask);
            if (maskStart < start || maskEnd > end) {
                return mask - 1;
            }
        }

        return mask;
    }

    private static String getMask(int bits) {
        switch (bits) {
            case 0:  return "ffff";
            case 1:  return "fffe";
            case 2:  return "fffc";
            case 3:  return "fff8";
            case 4:  return "fff0";
            case 5:  return "ffe0";
            case 6:  return "ffc0";
            case 7:  return "ff80";
            case 8:  return "ff00";
            case 9:  return "fe00";
            case 10: return "fc00";
            case 11: return "f800";
            case 12: return "f000";
            case 13: return "e000";
            case 14: return "c000";
            case 15: return "8000";
            case 16: return "0000";
            default: return null;
        }
    }

    private static boolean checkTreatmentResolver(Device device) {
        if (device == null || !device.is(ExtensionTreatmentResolver.class)) {
            log.warn("Nicira extension treatment is not supported");
            return false;
        }

        return true;
    }

    /**
     * Builder class for OVS Connection Tracking feature actions.
     */
    public static final class NiciraConnTrackTreatmentBuilder {

        private DriverService driverService;
        private DeviceId deviceId;
        private IpAddress natAddress = null;
        private TpPort natPortMin = null;
        private TpPort natPortMax = null;
        private int zone;
        private boolean commit;
        private short table = -1;
        private boolean natAction;
        private int natFlag;

        // private constructor
        private NiciraConnTrackTreatmentBuilder(DriverService driverService,
                                                DeviceId deviceId) {
            this.driverService = driverService;
            this.deviceId = deviceId;
        }

        /**
         * Sets commit flag.
         *
         * @param c true if commit, false if not.
         * @return NiriraConnTrackTreatmentBuilder object
         */
        public NiciraConnTrackTreatmentBuilder commit(boolean c) {
            this.commit = c;
            return this;
        }

        /**
         * Sets zone number.
         *
         * @param z zone number
         * @return NiriraConnTrackTreatmentBuilder object
         */
        public NiciraConnTrackTreatmentBuilder zone(int z) {
            this.zone = z;
            return this;
        }

        /**
         * Sets recirculation table number.
         *
         * @param t table number to restart
         * @return NiriraConnTrackTreatmentBuilder object
         */
        public NiciraConnTrackTreatmentBuilder table(short t) {
            this.table = t;
            return this;
        }

        /**
         * Sets IP address for NAT.
         *
         * @param ip NAT IP address
         * @return NiriraConnTrackTreatmentBuilder object
         */
        public NiciraConnTrackTreatmentBuilder natIp(IpAddress ip) {
            this.natAddress = ip;
            return this;
        }

        /**
         * Sets min port for NAT.
         *
         * @param port port number
         * @return NiciraConnTrackTreatmentBuilder object
         */
        public NiciraConnTrackTreatmentBuilder natPortMin(TpPort port) {
            this.natPortMin = port;
            return this;
        }

        /**
         * Sets max port for NAT.
         *
         * @param port port number
         * @return NiciraConnTrackTreatmentBuilder object
         */
        public NiciraConnTrackTreatmentBuilder natPortMax(TpPort port) {
            this.natPortMax = port;
            return this;
        }

        /**
         * Sets NAT flags.
         * SRC NAT: 1 << 0
         * DST NAT: 1 << 1
         * PERSISTENT NAT: 1 << 2
         * PROTO_HASH NAT: 1 << 3
         * PROTO_RANDOM NAT : 1 << 4
         *
         * @param flag flag value
         * @return NiciraConnTrackTreatmentBuilder object
         */
        public NiciraConnTrackTreatmentBuilder natFlag(int flag) {
            this.natFlag = 1 << flag;
            return this;
        }

        /**
         * Sets the flag for NAT action.
         *
         * @param nat nat action is included if true, no nat action otherwise
         * @return NiriraConnTrackTreatmentBuilder object
         */
        public NiciraConnTrackTreatmentBuilder natAction(boolean nat) {
            this.natAction = nat;
            return this;
        }

        /**
         * Builds extension treatment for OVS ConnTack and NAT feature.
         *
         * @return ExtensionTreatment object
         */
        public ExtensionTreatment build() {
            DriverHandler handler = driverService.createHandler(deviceId);
            ExtensionTreatmentResolver etr =
                    handler.behaviour(ExtensionTreatmentResolver.class);

            ExtensionTreatment natTreatment = etr.getExtensionInstruction(
                    ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_NAT.type());
            try {

                if (natAddress == null && natPortMin == null && natPortMax == null) {
                    natTreatment.setPropertyValue(CT_FLAGS, 0);
                    natTreatment.setPropertyValue(CT_PRESENT_FLAGS, 0);
                } else {
                    natTreatment.setPropertyValue(CT_FLAGS, this.natFlag);

                    natTreatment.setPropertyValue(CT_PRESENT_FLAGS,
                            buildPresentFlag((natPortMin != null && natPortMax != null),
                                    natAddress != null));
                }

                if (natAddress != null) {
                    natTreatment.setPropertyValue(CT_IPADDRESS_MIN, natAddress);
                    natTreatment.setPropertyValue(CT_IPADDRESS_MAX, natAddress);
                }

                if (natPortMin != null) {
                    natTreatment.setPropertyValue(CT_PORT_MIN, natPortMin.toInt());
                }

                if (natPortMax != null) {
                    natTreatment.setPropertyValue(CT_PORT_MAX, natPortMax.toInt());
                }

            } catch (Exception e) {
                log.error("Failed to set NAT due to error : {}", e);
                return null;
            }

            ExtensionTreatment ctTreatment = etr.getExtensionInstruction(
                    ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_CT.type());
            try {
                List<ExtensionTreatment> nat = new ArrayList<>();
                if (natAction) {
                    nat.add(natTreatment);
                }
                ctTreatment.setPropertyValue(CT_FLAGS, commit ? 1 : 0);
                ctTreatment.setPropertyValue(CT_ZONE, zone);
                ctTreatment.setPropertyValue(CT_TABLE, table > -1 ? table : 0xff);
                ctTreatment.setPropertyValue(CT_NESTED_ACTIONS, nat);
            } catch (Exception e) {
                log.error("Failed to set CT due to error : {}", e);
                return null;
            }

            return ctTreatment;
        }

        private int buildPresentFlag(boolean isPortPresent, boolean isAddressPresent) {

            int presentFlag = 0;

            if (isPortPresent) {
                presentFlag = 1 << PORT_MIN_FLAG | 1 << PORT_MAX_FLAG;
            }

            if (isAddressPresent) {
                // TODO: need to support IPv6 address
                presentFlag =  presentFlag | 1 << ADDRESS_V4_MIN_FLAG | 1 << ADDRESS_V4_MAX_FLAG;
            }

            return presentFlag;
        }
    }
}
