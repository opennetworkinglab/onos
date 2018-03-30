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

import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.ExtensionSelectorResolver;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.ExtensionSelectorType;
import org.onosproject.net.flow.instructions.ExtensionPropertyException;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

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
    private static final String CT = "niciraCt";
    private static final String CT_STATE = "ctState";
    private static final String CT_STATE_MASK = "ctStateMask";
    private static final String CT_PRESENT_FLAGS = "presentFlags";
    private static final String CT_IPADDRESS_MIN = "ipAddressMin";
    private static final String CT_IPADDRESS_MAX = "ipAddressMax";

    private static final int ADDRESS_MIN_FLAG = 0;
    private static final int ADDRESS_MAX_FLAG = 1;
    private static final int PORT_MIN_FLAG = 2;
    private static final int PORT_MAX_FLAG = 3;

    // Refer to http://openvswitch.org/support/dist-docs/ovs-fields.7.txt for the values
    public static final long CT_STATE_NONE = 0;
    public static final long CT_STATE_NEW = 0x01;
    public static final long CT_STATE_EST = 0x02;
    public static final long CT_STATE_NOT_TRK = 0x20;
    public static final long CT_STATE_TRK = 0x20;

    private RulePopulatorUtil() {
    }

    /**
     * Returns a builder for OVS Connection Tracking feature actions.
     *
     * @param ds DriverService
     * @param id DeviceId
     * @return a builder for OVS Connection Tracking feature actions
     */
    public static NiriraConnTrackTreatmentBuilder niciraConnTrackTreatmentBuilder(DriverService ds, DeviceId id) {
        return new NiriraConnTrackTreatmentBuilder(ds, id);
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
        if (device != null && !device.is(ExtensionTreatmentResolver.class)) {
            log.error("The extension treatment is not supported");
            return null;
        }

        if (device == null) {
            return null;
        }

        ExtensionTreatmentResolver resolver = device.as(ExtensionTreatmentResolver.class);
        ExtensionTreatment treatment = resolver.getExtensionInstruction(NICIRA_SET_TUNNEL_DST.type());
        try {
            treatment.setPropertyValue(TUNNEL_DST, remoteIp);
            return treatment;
        } catch (ExtensionPropertyException e) {
            log.warn("Failed to get tunnelDst extension treatment for {}", deviceId);
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
    public static ExtensionSelector buildCtExtensionSelector(DriverService driverService, DeviceId deviceId,
                                                             long ctState, long ctSateMask) {
        DriverHandler handler = driverService.createHandler(deviceId);
        ExtensionSelectorResolver esr = handler.behaviour(ExtensionSelectorResolver.class);

        ExtensionSelector extensionSelector = esr.getExtensionSelector(
                ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_CONNTRACK_STATE.type());
        try {
            extensionSelector.setPropertyValue(CT_STATE, ctState);
            extensionSelector.setPropertyValue(CT_STATE_MASK, ctSateMask);
        } catch (Exception e) {
            log.error("Failed to set nicira match CT state");
            return null;
        }

        return extensionSelector;
    }

    /**
     * Computes ConnTack State flag values.
     *
     * @param isTracking true for +trk, false for -trk
     * @param isNew true for +new, false for nothing
     * @param isEstablished true for +est, false for nothing
     * @return ConnTrack State flags
     */
    public static long computeCtStateFlag(boolean isTracking, boolean isNew, boolean isEstablished) {
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
    public static long computeCtMaskFlag(boolean isTracking, boolean isNew, boolean isEstablished) {
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
     * Builder class for OVS Connection Tracking feature actions.
     */
    public static final class NiriraConnTrackTreatmentBuilder {

        private DriverService driverService;
        private DeviceId deviceId;
        private IpAddress natAddress = null;
        private int zone;
        private boolean commit;
        private short table = -1;
        private boolean natAction;


        private NiriraConnTrackTreatmentBuilder(DriverService driverService, DeviceId deviceId) {
            this.driverService = driverService;
            this.deviceId = deviceId;
        }

        /**
         * Sets commit flag.
         *
         * @param c true if commit, false if not.
         * @return NiriraConnTrackTreatmentBuilder object
         */
        public NiriraConnTrackTreatmentBuilder commit(boolean c) {
            this.commit = c;
            return this;
        }

        /**
         * Sets zone number.
         *
         * @param z zone number
         * @return NiriraConnTrackTreatmentBuilder object
         */
        public NiriraConnTrackTreatmentBuilder zone(int z) {
            this.zone = z;
            return this;
        }

        /**
         * Sets recirculation table number.
         *
         * @param t table number to restart
         * @return NiriraConnTrackTreatmentBuilder object
         */
        public NiriraConnTrackTreatmentBuilder table(short t) {
            this.table = t;
            return this;
        }

        /**
         * Sets IP address for NAT.
         *
         * @param ip NAT IP address
         * @return NiriraConnTrackTreatmentBuilder object
         */
        public NiriraConnTrackTreatmentBuilder natIp(IpAddress ip) {
            this.natAddress = ip;
            return this;
        }

        /**
         * Sets the flag for NAT action.
         *
         * @param nat nat action is included if true, no nat action otherwise
         * @return NiriraConnTrackTreatmentBuilder object
         */
        public NiriraConnTrackTreatmentBuilder natAction(boolean nat) {
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
            ExtensionTreatmentResolver etr = handler.behaviour(ExtensionTreatmentResolver.class);

            ExtensionTreatment natTreatment
                    = etr.getExtensionInstruction(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_NAT.type());
            try {
                if (natAddress != null) {
                    natTreatment.setPropertyValue(CT_FLAGS, 1);
                    natTreatment.setPropertyValue(CT_PRESENT_FLAGS, buildPresentFlag(false, true));
                    natTreatment.setPropertyValue(CT_IPADDRESS_MIN, natAddress);
                    natTreatment.setPropertyValue(CT_IPADDRESS_MAX, natAddress);
                } else {
                    natTreatment.setPropertyValue(CT_FLAGS, 0);
                    natTreatment.setPropertyValue(CT_PRESENT_FLAGS, 0);
                }
            } catch (Exception e) {
                log.error("Failed to set NAT due to error : {}", e.getMessage());
                return null;
            }

            ExtensionTreatment ctTreatment
                    = etr.getExtensionInstruction(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_CT.type());
            try {
                List<ExtensionTreatment> nat = new ArrayList<>();
                if (natAction) {
                    nat.add(natTreatment);
                }
                ctTreatment.setPropertyValue(CT_FLAGS, commit ? 1 : 0);
                ctTreatment.setPropertyValue(CT_ZONE, zone);
                ctTreatment.setPropertyValue(CT_TABLE, table > -1 ? table : 0xff);
                ctTreatment.setPropertyValue("nestedActions", nat);
            } catch (Exception e) {
                log.error("Failed to set CT due to error : {}", e.getMessage());
                return null;
            }

            return ctTreatment;
        }

        private int buildPresentFlag(boolean isPortPresent, boolean isAddressPresent) {

            int presentFlag = 0;

            if (isPortPresent) {
                presentFlag = presentFlag | 1 << PORT_MIN_FLAG | 1 << PORT_MAX_FLAG;
            }

            if (isAddressPresent) {
                presentFlag =  1 << ADDRESS_MIN_FLAG | 1 << ADDRESS_MAX_FLAG;
            }

            return presentFlag;
        }
    }
}
