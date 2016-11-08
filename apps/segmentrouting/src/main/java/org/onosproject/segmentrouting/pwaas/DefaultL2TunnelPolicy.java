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
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of the default l2 tunnel policy.
 */
public class DefaultL2TunnelPolicy {

    /**
     * Id of the tunnel associated to this policy.
     */
    private long tunnelId;
    /**
     * First connect point.
     */
    private ConnectPoint cP1;
    /**
     * Second connect point.
     */
    private ConnectPoint cP2;
    /**
     * cP1 inner vlan tag. Used in QinQ packets.
     */
    private VlanId cP1InnerTag;
    /**
     * cP1 outer vlan tag.
     */
    private VlanId cP1OuterTag;
    /**
     * cP2 inner vlan tag. Used in QinQ packets.
     */
    private VlanId cP2InnerTag;
    /**
     * cP2 outer vlan tag.
     */
    private VlanId cP2OuterTag;
    /**
     * Boolean value to indicate if the pseudo wire is port based.
     */
    private boolean allVlan;

    /**
     * Creates a default l2 tunnel policy using
     * the given parameters.
     *
     * @param tunnelId the tunnel id
     * @param cP1 the first connect point
     * @param cP1InnerTag the cP1 inner tag
     * @param cP1OuterTag the cP1 outer tag
     * @param cP2 the second connect point
     * @param cP2InnerTag the cP2 inner tag
     * @param cP2OuterTag the cP2 outer tag
     * @param allVlan if the tunnel is port based or not
     */
    public DefaultL2TunnelPolicy(long tunnelId,
                                 ConnectPoint cP1, VlanId cP1InnerTag, VlanId cP1OuterTag,
                                 ConnectPoint cP2, VlanId cP2InnerTag, VlanId cP2OuterTag,
                                 boolean allVlan) {
        this.cP1 = checkNotNull(cP1);
        this.cP2 = checkNotNull(cP2);
        this.tunnelId = tunnelId;
        this.cP1InnerTag = cP1InnerTag;
        this.cP1OuterTag = cP1OuterTag;
        this.cP2InnerTag = cP2InnerTag;
        this.cP2OuterTag = cP2OuterTag;
        this.allVlan = allVlan;
    }

    /**
     * Returns the first connect point of the policy.
     *
     * @return first connect point
     */
    public ConnectPoint cP1() {
        return cP1;
    }

    /**
     * Returns the second connect point of the policy.
     *
     * @return second connect point
     */
    public ConnectPoint cP2() {
        return cP2;
    }

    /**
     * Returns the cP1 inner vlan tag of the policy.
     *
     * @return cP1 inner vlan tag
     */
    public VlanId cP1InnerTag() {
        return cP1InnerTag;
    }

    /**
     * Returns the cP1 outer vlan tag of the policy.
     *
     * @return cP1 outer vlan tag
     */
    public VlanId cP1OuterTag() {
        return cP1OuterTag;
    }

    /**
     * Returns the cP2 inner vlan tag of the policy.
     *
     * @return cP2 inner vlan tag
     */
    public VlanId cP2InnerTag() {
        return cP2InnerTag;
    }

    /**
     * Returns the cP2 outer vlan tag of the policy.
     *
     * @return cP2 outer vlan tag
     */
    public VlanId cP2OuterTag() {
        return cP2OuterTag;
    }

    /**
     * Return all vlan value.
     *
     * @return true, if the pw is port based. False if the traffic is sliced
     *         through the inner and outer tags
     */
    public boolean isAllVlan() {
        return allVlan;
    }

    /**
     * Returns the tunnel ID of the policy.
     *
     * @return Tunnel ID
     */
    public long tunnelId() {
        return this.tunnelId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tunnelId,
                            cP1,
                            cP2,
                            cP1InnerTag,
                            cP1OuterTag,
                            cP2InnerTag,
                            cP2OuterTag,
                            allVlan
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof DefaultL2TunnelPolicy) {
            DefaultL2TunnelPolicy that = (DefaultL2TunnelPolicy) o;
            if (this.tunnelId == that.tunnelId &&
                    this.cP1.equals(that.cP1) &&
                    this.cP2.equals(that.cP2) &&
                    this.cP1InnerTag.equals(that.cP1InnerTag) &&
                    this.cP1OuterTag.equals(that.cP1OuterTag) &&
                    this.cP2InnerTag.equals(that.cP2InnerTag) &&
                    this.cP2OuterTag.equals(that.cP2OuterTag) &&
                    this.allVlan == that.allVlan) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("tunnelId", tunnelId())
                .add("cP1", cP1())
                .add("cP2", cP2())
                .add("cP1InnerTag", cP1InnerTag())
                .add("cP1OuterTag", cP1OuterTag())
                .add("cP2InnerTag", cP2InnerTag())
                .add("cP2OuterTag", cP2OuterTag())
                .add("allVlan", isAllVlan())
                .toString();
    }

}
