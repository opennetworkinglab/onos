/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onosproject.net.behaviour.upf;

import com.google.common.annotations.Beta;
import org.onlab.packet.Ip4Address;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A structure representing the UE Termination in the downlink direction on the
 * UPF-programmable device.
 * Provide means to configure the traffic behavior (e.g. set Traffic Class, GTP TEID, or QFI).
 */
@Beta
public final class UpfTerminationDownlink implements UpfEntity {
    // Match Keys
    private final Ip4Address ueSessionId; // UE Session ID, use UE IP address to uniquely identify a session.
    private final byte applicationId; // Application ID, defaults to DEFAULT_APP_ID
    // Action parameters
    private final Integer ctrId;  // Counter ID unique to this UPF Termination Rule
    private final Byte trafficClass;
    private final Integer teid;  // Tunnel Endpoint Identifier
    private final Byte qfi; // QoS Flow Identifier
    private final int appMeterIdx;
    private final boolean dropping;

    private UpfTerminationDownlink(Ip4Address ueSessionId, byte applicationId, Integer ctrId, Byte trafficClass,
                                   Integer teid, Byte qfi, int appMeterIdx, boolean dropping) {
        this.ueSessionId = ueSessionId;
        this.applicationId = applicationId;
        this.ctrId = ctrId;
        this.trafficClass = trafficClass;
        this.teid = teid;
        this.qfi = qfi;
        this.appMeterIdx = appMeterIdx;
        this.dropping = dropping;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        UpfTerminationDownlink that = (UpfTerminationDownlink) obj;

        // Safe comparisons between potentially null objects
        return this.dropping == that.dropping &&
                Objects.equals(this.ueSessionId, that.ueSessionId) &&
                Objects.equals(this.applicationId, that.applicationId) &&
                Objects.equals(this.ctrId, that.ctrId) &&
                Objects.equals(this.trafficClass, that.trafficClass) &&
                Objects.equals(this.teid, that.teid) &&
                this.appMeterIdx == that.appMeterIdx &&
                Objects.equals(this.qfi, that.qfi);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ueSessionId, applicationId, ctrId, trafficClass, teid, qfi, appMeterIdx, dropping);
    }

    /**
     * Get UE Session ID associated with UPF Termination rule.
     *
     * @return UE Session ID
     */
    public Ip4Address ueSessionId() {
        return ueSessionId;
    }

    /**
     * Get the application ID associated with UPF Termination rule.
     *
     * @return the application ID
     */
    public byte applicationId() {
        return applicationId;
    }

    /**
     * Get PDR Counter ID associated with UPF Termination rule.
     *
     * @return PDR counter cell ID
     */
    public Integer counterId() {
        return ctrId;
    }

    /**
     * Get Traffic Class set by this UPF Termination rule.
     *
     * @return Traffic Class
     */
    public Byte trafficClass() {
        return trafficClass;
    }

    /**
     * Get GTP TEID set by this UPF Termination rule.
     *
     * @return GTP tunnel ID
     */
    public Integer teid() {
        return teid;
    }

    /**
     * Get QoS Flow Identifier set by this UPF Termination rule.
     *
     * @return QoS Flow Identifier
     */
    public Byte qfi() {
        return qfi;
    }

    /**
     * True if this UPF Termination needs to drop traffic.
     *
     * @return true if the UPF Termination needs dropping.
     */
    public boolean needsDropping() {
        return dropping;
    }

    /**
     * Get the app meter index set by this UPF Termination rule.
     *
     * @return App meter index
     */
    public int appMeterIdx() {
        return appMeterIdx;
    }

    @Override
    public UpfEntityType type() {
        return UpfEntityType.TERMINATION_DOWNLINK;
    }

    @Override
    public String toString() {
        return "UpfTerminationDL(" + matchString() + " -> " + actionString() + ")";
    }

    private String matchString() {
        return "Match(ue_addr=" + this.ueSessionId() + ", app_id=" + this.applicationId + ")";
    }

    private String actionString() {
        String fwd = "FWD";
        if (this.needsDropping()) {
            fwd = "DROP";
        }
        return "Action(" + fwd +
                ", teid=" + this.teid() +
                ", ctr_id=" + this.counterId() +
                ", qfi=" + this.qfi() +
                ", tc=" + this.trafficClass() +
                ", app_meter_idx=" + this.appMeterIdx() +
                ")";
    }

    public static class Builder {
        private Ip4Address ueSessionId = null;
        private Byte applicationId = null;
        private Integer ctrId = null;
        private Byte trafficClass = null;
        private Integer teid = null;
        private Byte qfi = null;
        private int appMeterIdx = DEFAULT_APP_INDEX;
        private boolean drop = false;

        public Builder() {

        }

        /**
         * Set the ID of the UE session.
         *
         * @param ueSessionId UE session ID
         * @return This builder object
         */
        public Builder withUeSessionId(Ip4Address ueSessionId) {
            this.ueSessionId = ueSessionId;
            return this;
        }

        /**
         * Set the ID of the application.
         * If not set, default to {@link UpfEntity#DEFAULT_APP_ID}.
         *
         * @param applicationId Application ID
         * @return This builder object
         */
        public Builder withApplicationId(byte applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        /**
         * Set the dataplane counter cell ID.
         *
         * @param ctrId PDR counter cell ID
         * @return This builder object
         */
        public Builder withCounterId(int ctrId) {
            this.ctrId = ctrId;
            return this;
        }

        /**
         * Set the Traffic Class.
         *
         * @param trafficClass Traffic Class
         * @return This builder object
         */
        public Builder withTrafficClass(byte trafficClass) {
            this.trafficClass = trafficClass;
            return this;
        }

        /**
         * Set the identifier of the unidirectional GTP tunnel that should be used for the UE Session.
         *
         * @param teid tunnel ID
         * @return This builder object
         */
        public Builder withTeid(Integer teid) {
            this.teid = teid;
            return this;
        }

        /**
         * Set the QoS Flow Identifier.
         *
         * @param qfi GTP Tunnel QFI
         * @return This builder object
         */
        public Builder withQfi(byte qfi) {
            this.qfi = qfi;
            return this;
        }

        /**
         * Sets whether to drop downlink UPF termination traffic or not.
         *
         * @param drop True if request to drop, false otherwise
         * @return This builder object
         */
        public Builder needsDropping(boolean drop) {
            this.drop = drop;
            return this;
        }

        /**
         * Sets the app meter index.
         * If not set, default to {@link UpfEntity#DEFAULT_APP_INDEX}.
         *
         * @param appMeterIdx App meter index
         * @return This builder object
         */
        public Builder withAppMeterIdx(int appMeterIdx) {
            this.appMeterIdx = appMeterIdx;
            return this;
        }

        public UpfTerminationDownlink build() {
            // Match fields must be provided
            checkNotNull(ueSessionId, "UE session ID must be provided");
            if (applicationId == null) {
                applicationId = DEFAULT_APP_ID;
            }
            checkNotNull(ctrId, "Counter ID must be provided");
            if (!drop) {
                checkNotNull(trafficClass, "Traffic class must be provided");
            }
            // TODO: should we verify that when dropping no other fields are provided
            return new UpfTerminationDownlink(
                    this.ueSessionId, this.applicationId, this.ctrId, this.trafficClass,
                    this.teid, this.qfi, this.appMeterIdx, this.drop
            );
        }

    }

}
