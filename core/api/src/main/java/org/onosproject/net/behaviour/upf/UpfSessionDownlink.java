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
 * A structure representing the UE Session on the UPF-programmable device.
 * Provide means to set up the UPF UE Session in the downlink direction.
 */
@Beta
public final class UpfSessionDownlink implements UpfEntity {
    // Match Keys
    private final Ip4Address ueAddress;
    // Action parameters
    private final Byte tunPeerId;
    private final int sessionMeterIdx;
    private final boolean buffering;
    private final boolean dropping;

    private UpfSessionDownlink(Ip4Address ipv4Address,
                               Byte tunPeerId,
                               int sessionMeterIdx,
                               boolean buffering,
                               boolean drop) {
        this.ueAddress = ipv4Address;
        this.sessionMeterIdx = sessionMeterIdx;
        this.tunPeerId = tunPeerId;
        this.buffering = buffering;
        this.dropping = drop;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (object == null) {
            return false;
        }
        if (getClass() != object.getClass()) {
            return false;
        }

        UpfSessionDownlink that = (UpfSessionDownlink) object;

        return this.buffering == that.buffering &&
                this.dropping == that.dropping &&
                this.sessionMeterIdx == that.sessionMeterIdx &&
                Objects.equals(ueAddress, that.ueAddress) &&
                Objects.equals(tunPeerId, that.tunPeerId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(ueAddress, sessionMeterIdx, tunPeerId, buffering, dropping);
    }

    @Override
    public String toString() {
        return "UpfSessionDL(" + matchString() + " -> " + actionString() + ")";
    }

    private String matchString() {
        return "Match(ue_addr=" + this.ueAddress() + ")";
    }

    private String actionString() {
        StringBuilder actionStrBuilder = new StringBuilder("Action(");
        if (this.needsBuffering() && this.needsDropping()) {
            actionStrBuilder.append("BUFF+DROP, ");
        } else if (this.needsBuffering()) {
            actionStrBuilder.append("BUFF, ");
        } else if (this.needsDropping()) {
            actionStrBuilder.append("DROP, ");
        } else {
            actionStrBuilder.append("FWD, ");
        }
       return actionStrBuilder.append(" tun_peer=").append(this.tunPeerId())
               .append(", session_meter_idx=").append(this.sessionMeterIdx()).append(")")
               .toString();
    }

    /**
     * True if this UPF UE Session needs buffering of the downlink traffic.
     *
     * @return true if the UPF UE Session needs buffering.
     */
    public boolean needsBuffering() {
        return buffering;
    }

    /**
     * True if this UPF UE Session needs dropping of the downlink traffic.
     *
     * @return true if the UPF UE Session needs dropping.
     */
    public boolean needsDropping() {
        return dropping;
    }

    /**
     * Get the UE IP address of this downlink UPF UE session.
     *
     * @return UE IP address
     */
    public Ip4Address ueAddress() {
        return ueAddress;
    }

    /**
     * Get the GTP tunnel peer ID that is set by this UPF UE Session rule.
     *
     * @return GTP tunnel peer ID
     */
    public Byte tunPeerId() {
        return tunPeerId;
    }

    /**
     * Get the session meter index that is set by this UPF UE Session rule.
     *
     * @return Session meter index
     */
    public int sessionMeterIdx() {
        return this.sessionMeterIdx;
    }

    @Override
    public UpfEntityType type() {
        return UpfEntityType.SESSION_DOWNLINK;
    }

    public static class Builder {
        private Ip4Address ueAddress = null;
        private Byte tunPeerId = null;
        private int sessionMeterIdx = DEFAULT_SESSION_INDEX;
        private boolean buffer = false;
        private boolean drop = false;

        public Builder() {

        }

        /**
         * Sets the UE IP address that this downlink UPF UE session rule matches on.
         *
         * @param ueAddress UE IP address
         * @return This builder object
         */
        public Builder withUeAddress(Ip4Address ueAddress) {
            this.ueAddress = ueAddress;
            return this;
        }

        /**
         * Sets the GTP tunnel peer ID that is set by this UPF UE Session rule.
         *
         * @param tunnelPeerId GTP tunnel peer ID
         * @return This builder object
         */
        public Builder withGtpTunnelPeerId(Byte tunnelPeerId) {
            this.tunPeerId = tunnelPeerId;
            return this;
        }

        /**
         * Sets whether to buffer downlink UPF UE session traffic or not.
         *
         * @param buffer True if request to buffer, false otherwise
         * @return This builder object
         */
        public Builder needsBuffering(boolean buffer) {
            this.buffer = buffer;
            return this;
        }

        /**
         * Sets whether to drop downlink UPF UE session traffic or not.
         *
         * @param drop True if request to buffer, false otherwise
         * @return This builder object
         */
        public Builder needsDropping(boolean drop) {
            this.drop = drop;
            return this;
        }

        /**
         * Sets the meter index associated with this UE session.
         * If not set, default to {@link UpfEntity#DEFAULT_SESSION_INDEX}.
         *
         * @param sessionMeterIdx Session meter index
         * @return This builder object
         */
        public Builder withSessionMeterIdx(int sessionMeterIdx) {
            this.sessionMeterIdx = sessionMeterIdx;
            return this;
        }

        public UpfSessionDownlink build() {
            // Match fields are required
            checkNotNull(ueAddress, "UE address must be provided");
            return new UpfSessionDownlink(ueAddress, tunPeerId, sessionMeterIdx, buffer, drop);
        }
    }
}
