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
package org.onosproject.tetopology.management.api.node;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import org.onosproject.tetopology.management.api.EncodingType;
import org.onosproject.tetopology.management.api.SwitchingType;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Default implementation of a tunnel termination point.
 */
public class DefaultTunnelTerminationPoint implements TunnelTerminationPoint {
    private final long ttpId;
    private final SwitchingType switchingLayer;
    private final EncodingType encodingLayer;
    private final BitSet flags;
    private final List<Long> interLayerLockList;
    private final List<LocalLinkConnectivity> localLinkConnectivityList;
    private final float[] availAdaptBandwidth;
    private final TtpKey supportTtpKey;

    /**
     * Create a tunnel termination point.
     *
     * @param ttpId                     tunnel termination point id
     * @param switchingLayer            switching network layer to which this
     *                                  TTP belongs
     * @param encodingLayer             encoding layer to which this TTP belongs
     * @param flags                     the TTP flags
     * @param interLayerLockList        the supported inter-layer locks
     * @param localLinkConnectivityList the local link connectivity list
     * @param availAdaptBandwidth       the remaining adaptation bandwidth
     *                                  at each priority level
     * @param supportTtpKey             supporting TTP key from underlay topology
     */
    public DefaultTunnelTerminationPoint(long ttpId,
                                         SwitchingType switchingLayer,
                                         EncodingType encodingLayer,
                                         BitSet flags,
                                         List<Long> interLayerLockList,
                                         List<LocalLinkConnectivity> localLinkConnectivityList,
                                         float[] availAdaptBandwidth,
                                         TtpKey supportTtpKey) {
        this.ttpId = ttpId;
        this.switchingLayer = switchingLayer;
        this.encodingLayer = encodingLayer;
        this.flags = flags;
        this.interLayerLockList = interLayerLockList != null ?
                Lists.newArrayList(interLayerLockList) : null;
        this.localLinkConnectivityList = localLinkConnectivityList != null ?
                Lists.newArrayList(localLinkConnectivityList) : null;
        this.availAdaptBandwidth = availAdaptBandwidth != null ?
                Arrays.copyOf(availAdaptBandwidth,
                              availAdaptBandwidth.length) : null;
        this.supportTtpKey = supportTtpKey;
    }

    @Override
    public long ttpId() {
        return ttpId;
    }

    @Override
    public SwitchingType switchingLayer() {
        return switchingLayer;
    }

    @Override
    public EncodingType encodingLayer() {
        return encodingLayer;
    }

    @Override
    public BitSet flags() {
        return flags;
    }

    @Override
    public List<Long> interLayerLockList() {
        if (interLayerLockList == null) {
            return null;
        }
        return ImmutableList.copyOf(interLayerLockList);
    }

    @Override
    public List<LocalLinkConnectivity> localLinkConnectivityList() {
        if (localLinkConnectivityList == null) {
            return null;
        }
        return ImmutableList.copyOf(localLinkConnectivityList);
    }

    @Override
    public float[] availAdaptBandwidth() {
        if (availAdaptBandwidth == null) {
            return null;
        }
        return Arrays.copyOf(availAdaptBandwidth, availAdaptBandwidth.length);
    }

    @Override
    public TtpKey supportingTtpId() {
        return supportTtpKey;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ttpId, switchingLayer, encodingLayer, flags,
                                interLayerLockList, localLinkConnectivityList,
                                Arrays.hashCode(availAdaptBandwidth), supportTtpKey);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof DefaultTunnelTerminationPoint) {
            DefaultTunnelTerminationPoint that = (DefaultTunnelTerminationPoint) object;
            return Objects.equal(ttpId, that.ttpId) &&
                    Objects.equal(switchingLayer, that.switchingLayer) &&
                    Objects.equal(encodingLayer, that.encodingLayer) &&
                    Objects.equal(flags, that.flags) &&
                    Objects.equal(interLayerLockList, that.interLayerLockList) &&
                    Objects.equal(localLinkConnectivityList, that.localLinkConnectivityList) &&
                    Arrays.equals(availAdaptBandwidth, that.availAdaptBandwidth) &&
                    Objects.equal(supportTtpKey, that.supportTtpKey);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("ttpId", ttpId)
                .add("switchingLayer", switchingLayer)
                .add("encodingLayer", encodingLayer)
                .add("flags", flags)
                .add("interLayerLockList", interLayerLockList)
                .add("localLinkConnectivityList", localLinkConnectivityList)
                .add("availAdaptBandwidth", availAdaptBandwidth)
                .add("supportTtpKey", supportTtpKey)
                .toString();
    }

}
