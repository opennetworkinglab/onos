/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.tetopology.management.api.link;

import org.onosproject.tetopology.management.api.EncodingType;
import org.onosproject.tetopology.management.api.SwitchingType;
import org.onosproject.tetopology.management.api.TeStatus;
import org.onosproject.tetopology.management.api.TeTopologyKey;

import java.util.BitSet;
import java.util.List;

/**
 * Abstraction of a TE link.
 */
public interface TeLink {
    /**
     * Indicates that the TE link belongs to an abstract topology.
     */
    public static final short BIT_ABSTRACT = 0;

    /**
     * Indicates that the underlay topology that supports this TE link
     * is dynamically created as opposed to being created by provisioning.
     */
    public static final short BIT_DYNAMIC = 1;

    /**
     * Indicates that the underlay topology is committed to service.
     */
    public static final short BIT_COMMITTED = 2;

    /**
     * Indicates that the TE link connects 2 TE domains.
     */
    public static final short BIT_ACCESS_INTERDOMAIN = 3;

    /**
     * Indicates that the TE link is not numbered.
     */
    public static final short BIT_UNNUMBERED = 4;

    /**
     * Returns the TE link key.
     *
     * @return the TE link key
     */
    TeLinkTpKey teLinkKey();

    /**
     * Returns the key of the bi-directional peer TE link.
     *
     * @return peer TE link key
     */
    TeLinkTpKey peerTeLinkKey();

    /**
     * Returns the flags of this TE link.
     *
     * @return the flags
     */
    BitSet flags();

    /**
     * Returns the network layer switching type for this link.
     *
     * @return the network layer switching type
     */
    SwitchingType switchingLayer();

    /**
     * Returns the network layer encoding type for this link.
     *
     * @return the encoding type
     */
    EncodingType encodingLayer();

    /**
     * Returns the external link.
     *
     * @return the external link
     */
    ExternalLink externalLink();

    /**
     * Returns the underlay TE topology identifier for the link.
     *
     * @return the underlay TE topology id
     */
    TeTopologyKey underlayTeTopologyId();

    /**
     * Returns the primary path.
     *
     * @return underlay primary path
     */
    UnderlayPrimaryPath primaryPath();

    /**
     * Returns the backup paths.
     *
     * @return list of underlay backup paths
     */
    List<UnderlayBackupPath> backupPaths();

    /**
     * Returns the supporting tunnel protection type.
     *
     * @return the tunnel protection type
     */
    TunnelProtectionType tunnelProtectionType();

    /**
     * Returns the supporting tunnel's source tunnel termination point
     * identifier.
     *
     * @return the source TTP id
     */
    long sourceTtpId();

    /**
     * Returns the supporting tunnel's destination tunnel termination
     * point identifier.
     *
     * @return the destination TTP id
     */
    long destinationTtpId();

    /**
     * Returns the supporting tunnel identifier.
     *
     * @return the supporting tunnel id
     */
    TeTunnelId teTunnelId();

    /**
     * Returns the supporting TE link identifier.
     *
     * @return the supporting TE link id
     */
    TeLinkTpGlobalKey supportingTeLinkId();

    /**
     * Returns the source TE link identifier.
     *
     * @return the source link id
     */
    TeLinkTpGlobalKey sourceTeLinkId();

    /**
     * Returns the link cost.
     *
     * @return the cost
     */
    Long cost();

    /**
     * Returns the link delay.
     *
     * @return the delay
     */
    Long delay();

    /**
     * Returns the link SRLG values.
     *
     * @return the srlgs
     */
    List<Long> srlgs();

    /**
     * Returns the link administrative group.
     *
     * @return the adminGroup
     */
    Long administrativeGroup();

    /**
     * Returns the supported inter-layer locks.
     *
     * @return the inter-layer locks
     */
    List<Long> interLayerLocks();

    /**
     * Returns the maximum bandwidth at each priority level.
     *
     * @return a list of maximum bandwidths
     */
    float[] maxBandwidth();

    /**
     * Returns the available bandwidth at each priority level.
     *
     * @return a list of available bandwidths
     */
    float[] availBandwidth();

    /**
     * Returns the maximum available bandwidth for a LSP at each priority level.
     *
     * @return a list of maximum available bandwidths
     */
    float[] maxAvailLspBandwidth();

    /**
     * Returns the minimum available bandwidth for a LSP at each priority level.
     *
     * @return a list of minimum available bandwidths
     */
    float[] minAvailLspBandwidth();

    /**
     * Returns the administrative status of this TE link.
     *
     * @return the admin status
     */
    TeStatus adminStatus();

    /**
     * Returns the operational status of this TE link.
     *
     * @return the operational status
     */
    TeStatus opStatus();

    /**
     * Returns the link ODUk resources.
     *
     * @return the ODUk resources
     */
    OduResource oduResource();
}
