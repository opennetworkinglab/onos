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

package org.onosproject.segmentrouting.pwaas;


import com.google.common.base.MoreObjects;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.net.Link;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of the default l2 tunnel.
 */
public class DefaultL2Tunnel implements L2Tunnel {

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

    private List<Link> pathUsed;

    /**
     * Vlan which will be used for the encapsualted
     * vlan traffic.
     */
    private VlanId transportVlan;

    /**
     * Creates a inter-co l2 tunnel using the
     * supplied parameters.
     *
     * @param mode         the tunnel mode
     * @param sdtag        the service delimiting tag
     * @param tunnelId     the tunnel id
     * @param pwLabel      the pseudo wire label
     * @param interCoLabel the inter central office label
     */
    public DefaultL2Tunnel(L2Mode mode, VlanId sdtag, long tunnelId, MplsLabel pwLabel, MplsLabel interCoLabel) {
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
     * Creates a l2Tunnel from a given tunnel.
     *
     * @param l2Tunnel to replicate
     */
    public DefaultL2Tunnel(DefaultL2Tunnel l2Tunnel) {

        this.pwMode = l2Tunnel.pwMode();
        this.sdTag = l2Tunnel.sdTag();
        this.tunnelId = l2Tunnel.tunnelId();
        this.pwLabel = l2Tunnel.pwLabel();
        this.interCoLabel = l2Tunnel.interCoLabel();
        this.pathUsed = l2Tunnel.pathUsed();
        this.transportVlan = l2Tunnel.transportVlan;
    }

    /**
     * Creates a intra-co l2 tunnel using the
     * supplied parameters.
     *
     * @param mode     the tunnel mode
     * @param sdtag    the service delimiting tag
     * @param tunnelId the tunnel id
     * @param pwLabel  the pseudo wire label
     */
    public DefaultL2Tunnel(L2Mode mode, VlanId sdtag, long tunnelId, MplsLabel pwLabel) {
        this(mode, sdtag, tunnelId, pwLabel, MplsLabel.mplsLabel(MplsLabel.MAX_MPLS));
    }


    /**
     * Creates an empty l2 tunnel.
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
    @Override
    public L2Mode pwMode() {
        return pwMode;
    }

    /**
     * Returns the service delimitation
     * tag.
     *
     * @return the service delimitation vlan id
     */
    @Override
    public VlanId sdTag() {
        return sdTag;
    }

    /**
     * Returns the tunnel id of the pseudo wire.
     *
     * @return the pseudo wire tunnel id
     */
    @Override
    public long tunnelId() {
        return tunnelId;
    }

    /**
     * Returns the pw label.
     *
     * @return the mpls pw label
     */
    @Override
    public MplsLabel pwLabel() {
        return pwLabel;
    }

    /**
     * Set the path for the pseudowire.
     *
     * @param path The path to set
     */
    @Override
    public void setPath(List<Link> path) {
        pathUsed = new ArrayList<>(path);
    }

    /**
     * Set the transport vlan for the pseudowire.
     *
     * @param vlan the vlan to use.
     */
    @Override
    public void setTransportVlan(VlanId vlan) {
        transportVlan = vlan;
    }

    /**
     * Returns the used path of the pseudowire.
     *
     * @return pathUsed
     */
    @Override
    public List<Link> pathUsed() {
        return pathUsed;
    }

    @Override
    public VlanId transportVlan() {
        return transportVlan;
    }


    /**
     * Returns the inter-co label.
     *
     * @return the mpls inter-co label
     */
    @Override
    public MplsLabel interCoLabel() {
        return interCoLabel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.tunnelId, this.pwMode, this.sdTag, this.pwLabel, this.interCoLabel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
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
                .add("transportVlan", transportVlan())
                .toString();
    }

}
