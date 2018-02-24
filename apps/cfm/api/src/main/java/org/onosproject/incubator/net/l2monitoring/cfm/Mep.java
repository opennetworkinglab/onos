/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.incubator.net.l2monitoring.cfm;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.onlab.packet.IpAddress;
import org.onlab.packet.VlanId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.net.DeviceId;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.PortNumber;

/**
 * A model of the Maintenance Association Endpoint.
 *
 * See IEE 802.1Q Section 12.14.7.1.3 for reference
 */
public interface Mep extends NetworkResource {

    /**
     * Get the ID of the MEP.
     *
     * @return The MEPID is an integer in the range 1–8191
     */
    MepId mepId();

    /**
     * Get the Device ID which the MEP is realized on.
     *
     * @return The DeviceID to which this Mep is associated
     */
    DeviceId deviceId();

    /**
     * Get the port to which the MEP is attached.
     *
     * @return An port to which the MEP is attached
     */
    PortNumber port();

    /**
     * Get the MEP direction.
     *
     * @return A value indicating the direction in which the MEP faces on the interface
     */
    MepDirection direction();

    /**
     * Get the Maintenance Domain reference.
     *
     * @return The name of the containing Maintenance Domain
     */
    MdId mdId();

    /**
     * Get the Maintenance Association reference.
     *
     * @return The name of the containing Maintenance Association
     */
    MaIdShort maId();

    /**
     * Get the Primary VID of the MEP.
     * The value 0 indicates that either the Primary VID is that
     * of the MEP's MA or that the MEP's MA is associated with no VID.
     *
     * @return An integer in the range 0-4094
     */
    VlanId primaryVid();

    /**
     * Set the Primary VID of the MEP.
     *
     * @param primaryVid An integer between 0 and 4094
     * @return A new MEP with this value set
     */
    Mep withPrimaryVid(VlanId primaryVid);

    /**
     * Get the administrative state of the MEP.
     *
     * @return The administrative state of the MEP
     */
    boolean administrativeState();

    /**
     * Set the administrative state of the MEP.
     *
     * @param adminState The administrative state of the MEP
     * @return A new MEP with this value set
     */
    Mep withAdministrativeState(boolean adminState);

    /**
     * Get whether the MEP is or is not to generate CCMs.
     *
     * CCMs are Continuity Check Messages
     *
     * @return boolean value indicating whether the MEP is or is not to generate CCMs
     */
    Boolean cciEnabled();

    /**
     * Enable or disable the generation of CCMs by the MEP.
     *
     * @param cciEnabled boolean value dictating whether CCMs are sent or not
     * @return A new MEP with this value set
     */
    Mep withCciEnabled(boolean cciEnabled);

    /**
     * Get the priority parameter for CCMs and LTMs transmitted by the MEP.
     *
     * @return The priority parameter for CCMs and LTMs transmitted by the MEP
     */
    Priority ccmLtmPriority();

    /**
     * Set the priority parameter for CCMs and LTMs transmitted by the MEP.
     *
     * @param priority An integer value between 0 and 7 inclusive
     * @return A new MEP with this value set
     */
    Mep withCcmLtmPriority(Priority priority);

    /**
     * Get the network address to which Fault Alarms are to be transmitted.
     *
     * @return The IP address to which Fault Alarms are to be transmitted
     */
    FngAddress fngAddress();

    /**
     * Set the network address to which Fault Alarms are to be transmitted.
     *
     * If “not specified,” the address used is that from the Maintenance Association managed object
     *
     * @param address Address type or indicator that address is not specified or alarms are not to be transmitted
     * @return A new MEP with this value set
     */
    Mep withFngAddress(FngAddress address);

    /**
     * Get the lowest priority defect that is allowed to generate a Fault Alarm.
     * @return The lowest priority defect that is allowed to generate a Fault Alarm
     */
    LowestFaultDefect lowestFaultPriorityDefect();

    /**
     * Set the lowest priority defect that is allowed to generate a Fault Alarm.
     * @param lowestFdType The lowest priority defect that is allowed to generate a Fault Alarm
     * @return A new MEP with this value set
     */
    Mep withLowestFaultPriorityDefect(LowestFaultDefect lowestFdType);

    /**
     * Get the time that the Fault must be present before it is issued.
     * @return The time that the Fault must be present before it is issued
     */
    Duration defectPresentTime();

    /**
     * Set the time that the Fault must be present before it is issued.
     *
     * The default is 2500ms (2.5 seconds) if not specified
     * @param duration The time that the Fault must be present before it is issued
     * @return A new MEP with this value set
     */
    Mep withDefectPresentTime(Duration duration);

    /**
     * Get the time that the Fault must be absent before it is reset.
     * @return The time that the Fault must be absent before it is reset
     */
    Duration defectAbsentTime();

    /**
     * Set the time that the Fault must be absent before it is reset.
     *
     * The default is 10000ms (10 seconds) if not specified
     * @param duration The time that the Fault must be absent before it is reset
     * @return A new MEP with this value set
     */
    Mep withDefectAbsentTime(Duration duration);

    /**
     * Enumerated options for MEP Directions.
     */
    public enum MepDirection {
        UP_MEP,
        DOWN_MEP
    }

    /**
     * Supported FNG Address Types.
     * See {@link Mep.FngAddress}
     */
    public enum FngAddressType {
        IPV4,
        IPV6,
        NOT_SPECIFIED,
        NOT_TRANSMITTED;
    }

    /**
     * Supported Fault Defect Types.
     */
    public enum FaultDefectType {
        DEF_NONE,
        DEF_RDI_CCM,
        DEF_MAC_STATUS,
        DEF_REMOTE_CCM,
        DEF_ERROR_CCM,
        DEF_XCON_CCM
    }

    /**
     * Options for setting the lowest fault defect.
     * Each comprises a set of {@link Mep.FaultDefectType}
     */
    public enum LowestFaultDefect {
        ALL_DEFECTS(FaultDefectType.DEF_RDI_CCM,
                FaultDefectType.DEF_MAC_STATUS,
                FaultDefectType.DEF_REMOTE_CCM,
                FaultDefectType.DEF_ERROR_CCM,
                FaultDefectType.DEF_XCON_CCM),
        MAC_FD_PLUS(FaultDefectType.DEF_MAC_STATUS,
                FaultDefectType.DEF_REMOTE_CCM,
                FaultDefectType.DEF_ERROR_CCM,
                FaultDefectType.DEF_XCON_CCM),
        REMOTE_FD_PLUS(FaultDefectType.DEF_REMOTE_CCM,
                FaultDefectType.DEF_ERROR_CCM,
                FaultDefectType.DEF_XCON_CCM),
        ERROR_FD_PLUS(FaultDefectType.DEF_ERROR_CCM,
                FaultDefectType.DEF_XCON_CCM),
        XCON_FD_ONLY(FaultDefectType.DEF_XCON_CCM);

        private Set<FaultDefectType> defectTypes = new HashSet<>();

        private LowestFaultDefect(FaultDefectType... defectTypes) {
            for (FaultDefectType defectType:defectTypes) {
                this.defectTypes.add(defectType);
            }
        }

        public Set<FaultDefectType> getDefectTypes() {
            return defectTypes;
        }
    }

    /**
     * An enumerated set of values to represent Priority.
     */
    public enum Priority {
        PRIO0, PRIO1, PRIO2, PRIO3, PRIO4, PRIO5, PRIO6, PRIO7
    }

    /**
     * A simple class to join an FngAddressType and an IpAddress.
     */
    public final class FngAddress {
        private final FngAddressType addressType;
        private final IpAddress ipAddress;

        private FngAddress(FngAddressType addressType, IpAddress ipAddress) {
            this.addressType = addressType;
            this.ipAddress = ipAddress;
        }

        public FngAddressType addressType() {
            return addressType;
        }

        public IpAddress ipAddress() {
            return ipAddress;
        }

        public static FngAddress ipV4Address(IpAddress ipAddress) {
            return new FngAddress(FngAddressType.IPV4, ipAddress);
        }

        public static FngAddress ipV6Address(IpAddress ipAddress) {
            return new FngAddress(FngAddressType.IPV6, ipAddress);
        }

        public static FngAddress notSpecified() {
            return new FngAddress(FngAddressType.NOT_SPECIFIED, null);
        }

        public static FngAddress notTransmitted(IpAddress ipAddress) {
            return new FngAddress(FngAddressType.NOT_TRANSMITTED, ipAddress);
        }

        @Override
        public String toString() {
            return "FngAddress{" +
                    "addressType=" + addressType +
                    ", ipAddress=" + ipAddress +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            FngAddress that = (FngAddress) o;

            if (addressType != that.addressType) {
                return false;
            }
            return ipAddress != null ? ipAddress.equals(that.ipAddress) : that.ipAddress == null;
        }

        @Override
        public int hashCode() {
            int result = addressType.hashCode();
            result = 31 * result + (ipAddress != null ? ipAddress.hashCode() : 0);
            return result;
        }
    }

    /**
     * Builder for {@link Mep}.
     */
    interface MepBuilder {

        MepBuilder primaryVid(VlanId primaryVid);

        MepBuilder administrativeState(boolean administrativeState);

        MepBuilder cciEnabled(boolean cciEnabled);

        MepBuilder ccmLtmPriority(Priority ccmLtmPriority);

        MepBuilder fngAddress(FngAddress fngAddress);

        MepBuilder lowestFaultPriorityDefect(LowestFaultDefect lowestFaultPriorityDefect);

        MepBuilder defectPresentTime(Duration defectPresentTime);

        MepBuilder defectAbsentTime(Duration defectAbsentTime);

        Mep build() throws CfmConfigException;
    }
}
