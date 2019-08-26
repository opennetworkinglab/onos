/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.util;

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
import org.onosproject.net.group.GroupDescription.Type;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static org.onosproject.k8snetworking.api.Constants.A_CLASS;
import static org.onosproject.k8snetworking.api.Constants.B_CLASS;
import static org.onosproject.k8snetworking.api.Constants.DST;
import static org.onosproject.k8snetworking.api.Constants.SRC;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_LOAD;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ARP_SHA_TO_THA;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ARP_SPA_TO_TPA;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ETH_SRC_TO_DST;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_RESUBMIT_TABLE;
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

    private static final long CT_STATE_NONE = 0;
    private static final long CT_STATE_NEW = 0x01;
    private static final long CT_STATE_EST = 0x02;
    private static final long CT_STATE_NOT_TRK = 0x20;
    private static final long CT_STATE_TRK = 0x20;

    private static final String TABLE_EXTENSION = "table";

    private static final String OFF_SET_N_BITS = "ofsNbits";
    private static final String DESTINATION = "dst";
    private static final String VALUE = "value";

    private static final int SRC_IP = 0x00000e04;
    private static final int DST_IP = 0x00001004;

    private static final int A_CLASS_OFF_SET_BIT = 8;
    private static final int B_CLASS_OFF_SET_BIT = 16;
    private static final int REMAINDER_BIT = 16;

    // not intended for direct invocation from external
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
            log.error("Failed to set nicira match CT state", e);
            return null;
        }

        return extensionSelector;
    }

    /**
     * Computes ConnTack State flag values.
     *
     * @param isTracking true for +trk, false for -trk
     * @param isNew true for +new, false for -new
     * @param isEstablished true for +est, false for -est
     * @return ConnTrack State flags
     */
    public static long computeCtStateFlag(boolean isTracking,
                                          boolean isNew,
                                          boolean isEstablished) {
        long ctStateFlag = 0x00;

        if (isTracking) {
            ctStateFlag = ctStateFlag | CT_STATE_TRK;
        }

        if (isNew) {
            ctStateFlag = ctStateFlag | CT_STATE_TRK;
            ctStateFlag = ctStateFlag | CT_STATE_NEW;
        }

        if (isEstablished) {
            ctStateFlag = ctStateFlag | CT_STATE_TRK;
            ctStateFlag = ctStateFlag | CT_STATE_EST;
        }

        return ctStateFlag;
    }

    /**
     * Computes ConnTrack State mask values.
     *
     * @param isTracking true for setting +trk/-trk value, false for otherwise
     * @param isNew true for setting +new/-new value, false for otherwise
     * @param isEstablished true for setting +est/-est value, false for otherwise
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
     * Returns the group bucket with given traffic treatment and group type.
     *
     * @param treatment     traffic treatment
     * @param type          group type
     * @param weight        weight (only for select type)
     * @return group bucket
     */
    public static GroupBucket buildGroupBucket(TrafficTreatment treatment,
                                               Type type, short weight) {
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
     * Returns the nicira resubmit extension treatment with given table ID.
     *
     * @param device        device instance
     * @param tableId       table identifier
     * @return resubmit extension treatment
     */
    public static ExtensionTreatment buildResubmitExtension(Device device, int tableId) {
        if (!checkTreatmentResolver(device)) {
            return null;
        }

        ExtensionTreatmentResolver resolver = device.as(ExtensionTreatmentResolver.class);
        ExtensionTreatment treatment =
                resolver.getExtensionInstruction(NICIRA_RESUBMIT_TABLE.type());

        try {
            treatment.setPropertyValue(TABLE_EXTENSION, ((short) tableId));
            return treatment;
        } catch (ExtensionPropertyException e) {
            log.error("Failed to set nicira resubmit extension treatment for {}",
                    device.id());
            return null;
        }
    }

    /**
     * Returns the nicira load extension treatment.
     *
     * @param device        device instance
     * @param cidrClass     CIDR class (a | b)
     * @param ipType        IP type (src|dst)
     * @param shift         shift (e.g., 10.10., 20.20., 10, 20,)
     * @return load extension treatment
     */
    public static ExtensionTreatment buildLoadExtension(Device device,
                                                        String cidrClass,
                                                        String ipType,
                                                        String shift) {
        if (!checkTreatmentResolver(device)) {
            return null;
        }

        ExtensionTreatmentResolver resolver = device.as(ExtensionTreatmentResolver.class);
        ExtensionTreatment treatment =
                resolver.getExtensionInstruction(NICIRA_LOAD.type());

        long dst = 0L;

        if (SRC.equalsIgnoreCase(ipType)) {
            dst = SRC_IP;
        } else if (DST.equals(ipType)) {
            dst = DST_IP;
        }

        long value = calculateUpperBit(cidrClass, shift);

        // we only rewrite the upper x bits with value
        int ofsNbits = 0;

        if (A_CLASS.equals(cidrClass)) {
            ofsNbits = A_CLASS_OFF_SET_BIT << 6 | (REMAINDER_BIT - 1);
        } else if (B_CLASS.equals(cidrClass)) {
            ofsNbits = B_CLASS_OFF_SET_BIT << 6 | (REMAINDER_BIT - 1);
        }

        try {
            treatment.setPropertyValue(OFF_SET_N_BITS, ofsNbits);
            treatment.setPropertyValue(DESTINATION, dst);
            treatment.setPropertyValue(VALUE, value);
            return treatment;
        } catch (ExtensionPropertyException e) {
            log.error("Failed to set nicira load extension treatment for {}",
                    device.id());
            return null;
        }
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
     * Calculate IP address upper string into integer.
     *
     * @param cidrClass CIDR class type
     * @param shift IP address upper two octets with dot
     * @return calculated integer
     */
    private static int calculateUpperBit(String cidrClass, String shift) {

        if (A_CLASS.equals(cidrClass)) {
            return Integer.valueOf(shift);
        }

        if (B_CLASS.equals(cidrClass)) {
            String[] strArray = shift.split("\\.");
            int firstOctet = Integer.valueOf(strArray[0]);
            int secondOctet = Integer.valueOf(strArray[1]);
            return firstOctet << 8 | secondOctet;
        }

        return 0;
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
         * @return NiciraConnTrackTreatmentBuilder object
         */
        public NiciraConnTrackTreatmentBuilder commit(boolean c) {
            this.commit = c;
            return this;
        }

        /**
         * Sets zone number.
         *
         * @param z zone number
         * @return NiciraConnTrackTreatmentBuilder object
         */
        public NiciraConnTrackTreatmentBuilder zone(int z) {
            this.zone = z;
            return this;
        }

        /**
         * Sets recirculation table number.
         *
         * @param t table number to restart
         * @return NiciraConnTrackTreatmentBuilder object
         */
        public NiciraConnTrackTreatmentBuilder table(short t) {
            this.table = t;
            return this;
        }

        /**
         * Sets IP address for NAT.
         *
         * @param ip NAT IP address
         * @return NiciraConnTrackTreatmentBuilder object
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
         * @return NiciraConnTrackTreatmentBuilder object
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
                log.error("Failed to set NAT due to error", e);
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
                log.error("Failed to set CT due to error", e);
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
