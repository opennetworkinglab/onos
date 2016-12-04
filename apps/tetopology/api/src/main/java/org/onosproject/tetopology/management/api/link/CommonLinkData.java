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

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.onosproject.tetopology.management.api.EncodingType;
import org.onosproject.tetopology.management.api.SwitchingType;
import org.onosproject.tetopology.management.api.TeStatus;

import java.util.BitSet;
import java.util.List;

/**
 * Representation of link common attributes.
 */
public class CommonLinkData {
    private final TeStatus adminStatus;
    private final TeStatus opStatus;
    private final BitSet flags;

    private final SwitchingType switchingLayer;
    private final EncodingType encodingLayer;

    private final ExternalLink externalLink;
    private final UnderlayPath underlayPath;
    private final TePathAttributes teAttributes;
    private final List<Long> interLayerLocks;
    private final LinkBandwidth bandwidth;
    private final Long adminGroup;

    /**
     * Creates an instance of CommonLinkData.
     *
     * @param adminStatus     the admin status
     * @param opStatus        the operational Status
     * @param flags           the flags
     * @param switchingLayer  the network layer switching type
     * @param encodingLayer   the network layer encoding type
     * @param externalLink    the external link specific attributes
     * @param underlayPath    the link underlay path and supporting tunnel
     * @param teAttributes    the link path TE attributes
     * @param adminGroup      the administrative group
     * @param interLayerLocks the supported inter-layer locks
     * @param bandwidth       the link maximum and available bandwidth at
     *                        each priority level
     */
    public CommonLinkData(TeStatus adminStatus,
                          TeStatus opStatus, BitSet flags, SwitchingType switchingLayer,
                          EncodingType encodingLayer, ExternalLink externalLink,
                          UnderlayPath underlayPath, TePathAttributes teAttributes,
                          Long adminGroup, List<Long> interLayerLocks,
                          LinkBandwidth bandwidth) {
        this.adminStatus = adminStatus;
        this.opStatus = opStatus;
        this.flags = flags;
        this.switchingLayer = switchingLayer;
        this.encodingLayer = encodingLayer;
        this.externalLink = externalLink;
        this.underlayPath = underlayPath;
        this.teAttributes = teAttributes;
        this.adminGroup = adminGroup;
        this.interLayerLocks = interLayerLocks != null ?
                Lists.newArrayList(interLayerLocks) : null;
        this.bandwidth = bandwidth;
    }

    /**
     * Creates an instance of CommonLinkData with a TeLink.
     *
     * @param link the TE link
     */
    public CommonLinkData(TeLink link) {
        this.adminStatus = link.adminStatus();
        this.opStatus = link.opStatus();
        this.flags = link.flags();
        this.switchingLayer = link.switchingLayer();
        this.encodingLayer = link.encodingLayer();
        this.externalLink = link.externalLink();
        this.underlayPath = new UnderlayPath(link);
        this.teAttributes = new TePathAttributes(link);
        this.adminGroup = link.administrativeGroup();
        this.interLayerLocks = link.interLayerLocks() != null ?
                Lists.newArrayList(link.interLayerLocks()) : null;
        this.bandwidth = new LinkBandwidth(link);
    }


    /**
     * Returns the admin status.
     *
     * @return the admin status
     */
    public TeStatus adminStatus() {
        return adminStatus;
    }

    /**
     * Returns the operational status.
     *
     * @return the optional status
     */
    public TeStatus opStatus() {
        return opStatus;
    }

    /**
     * Returns the flags.
     *
     * @return the flags
     */
    public BitSet flags() {
        return flags;
    }

    /**
     * Returns the network layer switching type for this link.
     *
     * @return the switching layer type
     */
    public SwitchingType switchingLayer() {
        return switchingLayer;
    }

    /**
     * Returns the network layer encoding type for this link.
     *
     * @return the encoding type
     */
    public EncodingType encodingLayer() {
        return encodingLayer;
    }

    /**
     * Returns the external link.
     *
     * @return the external link
     */
    public ExternalLink externalLink() {
        return externalLink;
    }

    /**
     * Returns the link underlay path and tunnel.
     *
     * @return the underlay path
     */
    public UnderlayPath underlayPath() {
        return underlayPath;
    }

    /**
     * Returns the path TE attributes.
     *
     * @return the path TE Attributes
     */
    public TePathAttributes teAttributes() {
        return teAttributes;
    }

    /**
     * Returns the link administrative group.
     *
     * @return the admin group
     */
    public Long adminGroup() {
        return adminGroup;
    }

    /**
     * Returns the supported inter-layer locks.
     *
     * @return the inter-layer locks
     */
    public List<Long> interLayerLocks() {
        if (interLayerLocks == null) {
            return null;
        }
        return ImmutableList.copyOf(interLayerLocks);
    }

    /**
     * Returns the link maximum and available bandwidth at each priority level.
     *
     * @return the bandwidth
     */
    public LinkBandwidth bandwidth() {
        return bandwidth;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(adminStatus, opStatus, flags, switchingLayer,
                                encodingLayer, externalLink, underlayPath,
                                teAttributes, interLayerLocks, bandwidth);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof CommonLinkData) {
            CommonLinkData that = (CommonLinkData) object;
            return Objects.equal(adminStatus, that.adminStatus) &&
                    Objects.equal(opStatus, that.opStatus) &&
                    Objects.equal(flags, that.flags) &&
                    Objects.equal(switchingLayer, that.switchingLayer) &&
                    Objects.equal(encodingLayer, that.encodingLayer) &&
                    Objects.equal(externalLink, that.externalLink) &&
                    Objects.equal(underlayPath, that.underlayPath) &&
                    Objects.equal(teAttributes, that.teAttributes) &&
                    Objects.equal(interLayerLocks, that.interLayerLocks) &&
                    Objects.equal(bandwidth, that.bandwidth);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("adminStatus", adminStatus)
                .add("opStatus", opStatus)
                .add("flags", flags)
                .add("switchingLayer", switchingLayer)
                .add("encodingLayer", encodingLayer)
                .add("externalLink", externalLink)
                .add("underlayPath", underlayPath)
                .add("teAttributes", teAttributes)
                .add("interLayerLocks", interLayerLocks)
                .add("bandwidth", bandwidth)
                .toString();
    }

}
