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
 * Provide means to set up the UPF UE Session in the uplink direction.
 */
@Beta
public final class UpfSessionUplink implements UpfEntity {
    // Match Keys
    private final Ip4Address tunDestAddr; // The tunnel destination address (N3/S1U IPv4 address)
    private final Integer teid;  // The Tunnel Endpoint ID that this UeSession matches on

    // Action parameters
    private final boolean dropping; // Used to convey dropping information

    private UpfSessionUplink(Ip4Address tunDestAddr,
                             Integer teid,
                             boolean drop) {
        this.tunDestAddr = tunDestAddr;
        this.teid = teid;
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

        UpfSessionUplink that = (UpfSessionUplink) object;

        return this.dropping == that.dropping &&
                Objects.equals(tunDestAddr, that.tunDestAddr) &&
                Objects.equals(teid, that.teid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tunDestAddr, teid, dropping);
    }

    @Override
    public String toString() {
        return "UpfSessionUL(" + matchString() + " -> " + actionString() + ")";
    }

    private String matchString() {
        return "Match(tun_dst_addr=" + this.tunDstAddr() + ", teid=" + this.teid() + ")";
    }

    private String actionString() {
        StringBuilder actionStrBuilder = new StringBuilder("Action(");
        if (this.needsDropping()) {
            actionStrBuilder.append("DROP");

        } else {
            actionStrBuilder.append("FWD");
        }
        return actionStrBuilder.append(")").toString();
    }

    /**
     * True if this UPF UE Session needs dropping of the uplink traffic.
     *
     * @return true if the UE Session needs dropping.
     */
    public boolean needsDropping() {
        return dropping;
    }

    /**
     * Get the tunnel destination IP address in the uplink UPF UE session (N3/S1U IP address).
     *
     * @return UE IP address
     */
    public Ip4Address tunDstAddr() {
        return tunDestAddr;
    }

    /**
     * Get the identifier of the GTP tunnel that this UPF UE Session rule matches on.
     *
     * @return GTP tunnel ID
     */
    public Integer teid() {
        return teid;
    }

    @Override
    public UpfEntityType type() {
        return UpfEntityType.SESSION_UPLINK;
    }

    public static class Builder {
        private Ip4Address tunDstAddr = null;
        private Integer teid = null;
        private boolean drop = false;

        public Builder() {

        }

        /**
         * Set the tunnel destination IP address (N3/S1U address) that this UPF UE Session rule matches on.
         *
         * @param tunDstAddr The tunnel destination IP address
         * @return This builder object
         */
        public Builder withTunDstAddr(Ip4Address tunDstAddr) {
            this.tunDstAddr = tunDstAddr;
            return this;
        }

        /**
         * Set the identifier of the GTP tunnel that this UPF UE Session rule matches on.
         *
         * @param teid GTP tunnel ID
         * @return This builder object
         */
        public Builder withTeid(Integer teid) {
            this.teid = teid;
            return this;
        }


        /**
         * Sets whether to drop uplink UPF UE session traffic or not.
         *
         * @param drop True if request to buffer, false otherwise
         * @return This builder object
         */
        public Builder needsDropping(boolean drop) {
            this.drop = drop;
            return this;
        }

        public UpfSessionUplink build() {
            // Match keys are required.
            checkNotNull(tunDstAddr, "Tunnel destination must be provided");
            checkNotNull(teid, "TEID must be provided");
            return new UpfSessionUplink(tunDstAddr, teid, drop);
        }
    }
}
