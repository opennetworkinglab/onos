/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.segmentrouting.pwaas;


import com.google.common.base.MoreObjects;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of the default l2 tunnel.
 */
public class DefaultL2Tunnel {

    /**
     * Mode of the pseudo wire.
     */
    private L2Mode pwMode;
    /**
     * Service delimiting tag.
     */
    private VlanId sdTag;
    /**
     * Tunnel id.
     */
    private long tunnelId;
    /**
     * Pseudo wire label.
     */
    private MplsLabel pwLabel;
    /**
     * Inter-CO label.
     */
    private MplsLabel interCoLabel;

    /**
     * Creates a inter-co l2 tunnel using the
     * supplied parameters.
     *
     * @param mode the tunnel mode
     * @param sdtag the service delimiting tag
     * @param tunnelId the tunnel id
     * @param pwLabel the pseudo wire label
     * @param interCoLabel the inter central office label
     */
    public DefaultL2Tunnel(L2Mode mode,
                           VlanId sdtag,
                           long tunnelId,
                           MplsLabel pwLabel,
                           MplsLabel interCoLabel) {
        checkNotNull(mode);
        checkArgument(tunnelId > 0);
        checkNotNull(pwLabel);
        checkNotNull(interCoLabel);

        this.pwMode = mode;
        this.sdTag = sdtag;
        this.tunnelId = tunnelId;
        this.pwLabel = pwLabel;
        this.interCoLabel = interCoLabel;
    }

    /**
     * Creates a intra-co l2 tunnel using the
     * supplied parameters.
     *
     * @param mode the tunnel mode
     * @param sdtag the service delimiting tag
     * @param tunnelId the tunnel id
     * @param pwLabel the pseudo wire label
     */
    public DefaultL2Tunnel(L2Mode mode,
                           VlanId sdtag,
                           long tunnelId,
                           MplsLabel pwLabel) {
        this(mode, sdtag, tunnelId, pwLabel, MplsLabel.mplsLabel(MplsLabel.MAX_MPLS));
    }

    /**
     * Creates an empty l2 tunnel.
     *
     **/
    public DefaultL2Tunnel() {
        this.pwMode = null;
        this.sdTag = null;
        this.tunnelId = 0;
        this.pwLabel = null;
        this.interCoLabel = null;
    }

    /**
     * Returns the mode of the pseudo wire.
     *
     * @return the pseudo wire mode
     */
    public L2Mode pwMode() {
        return pwMode;
    }

    /**
     * Returns the service delimitation
     * tag.
     *
     * @return the service delimitation vlan id
     */
    public VlanId sdTag() {
        return sdTag;
    }

    /**
     * Returns the tunnel id of the pseudo wire.
     *
     * @return the pseudo wire tunnel id
     */
    public long tunnelId() {
        return tunnelId;
    }

    /**
     * Returns the pw label.
     *
     * @return the mpls pw label
     */
    public MplsLabel pwLabel() {
        return pwLabel;
    }

    /**
     * Returns the inter-co label.
     *
     * @return the mpls inter-co label
     */
    public MplsLabel interCoLabel() {
        return interCoLabel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.tunnelId, this.pwLabel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (getClass() != o.getClass()) {
            return false;
        }

        if (o instanceof DefaultL2Tunnel) {
            DefaultL2Tunnel that = (DefaultL2Tunnel) o;
            return this.tunnelId == that.tunnelId &&
                    this.pwMode.equals(that.pwMode) &&
                    this.sdTag.equals(that.sdTag) &&
                    this.pwLabel.equals(that.pwLabel) &&
                    this.interCoLabel.equals(that.interCoLabel);
        }

        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("pwMode", pwMode())
                .add("sdTag", sdTag())
                .add("tunnelId", tunnelId())
                .add("pwLabel", pwLabel())
                .add("interCoLabel", interCoLabel())
                .toString();
    }

}
