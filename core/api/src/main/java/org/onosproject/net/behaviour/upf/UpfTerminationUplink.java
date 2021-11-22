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
 * A structure representing the UE Termination in the uplink direction on the
 * UPF-programmable device.
 * Provides means to configure the traffic behavior (e.g. set Traffic Class).
 */
@Beta
public final class UpfTerminationUplink implements UpfEntity {
    // Match Keys
    private final Ip4Address ueSessionId; // UE Session ID, use UE IP address to uniquely identify a session.
    // Action parameters
    private final Integer ctrId;  // Counter ID unique to this UPF Termination Rule
    private final Byte trafficClass;
    private final boolean dropping;

    private UpfTerminationUplink(Ip4Address ueSessionId, Integer ctrId, Byte trafficClass,
                                 boolean dropping) {
        this.ueSessionId = ueSessionId;
        this.ctrId = ctrId;
        this.trafficClass = trafficClass;
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
        UpfTerminationUplink that = (UpfTerminationUplink) obj;

        // Safe comparisons between potentially null objects
        return this.dropping == that.dropping &&
                Objects.equals(this.ueSessionId, that.ueSessionId) &&
                Objects.equals(this.ctrId, that.ctrId) &&
                Objects.equals(this.trafficClass, that.trafficClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ueSessionId, ctrId, trafficClass, dropping);
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
     * True if this UPF Termination needs to drop traffic.
     *
     * @return true if the UPF Termination needs dropping.
     */
    public boolean needsDropping() {
        return dropping;
    }

    @Override
    public UpfEntityType type() {
        return UpfEntityType.TERMINATION_UPLINK;
    }

    @Override
    public String toString() {
        return "TerminationDL{" + matchString() + "->" + actionString() + "}";
    }

    private String matchString() {
        return "Match(ue_addr=" + this.ueSessionId() + ")";
    }

    private String actionString() {
        return "(CTR_ID=" + this.counterId() + ", TC=" + this.trafficClass() + ")";
    }

    public static class Builder {
        private Ip4Address ueSessionId = null;
        private Integer ctrId = null;
        private Byte trafficClass = null;
        private boolean dropping = false;

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
         * Sets whether to drop uplink UPF termination traffic or not.
         *
         * @param dropping True if request to buffer, false otherwise
         * @return This builder object
         */
        public Builder needsDropping(boolean dropping) {
            this.dropping = dropping;
            return this;
        }

        public UpfTerminationUplink build() {
            // Match fields must be provided
            checkNotNull(ueSessionId, "UE session ID must be provided");

            checkNotNull(ctrId, "Counter ID must be provided");
            // TODO: should we verify that when dropping no other fields are provided
            return new UpfTerminationUplink(
                    this.ueSessionId, this.ctrId, this.trafficClass, this.dropping);
        }

    }

}
