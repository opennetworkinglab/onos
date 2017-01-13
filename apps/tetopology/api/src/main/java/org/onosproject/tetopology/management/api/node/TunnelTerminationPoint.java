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

import java.util.BitSet;
import java.util.List;

import org.onosproject.tetopology.management.api.EncodingType;
import org.onosproject.tetopology.management.api.SwitchingType;

/**
 * Representation of a tunnel termination point.
 */
public interface TunnelTerminationPoint {
    /**
     * Indicates that the TTP supports one-plus-one protection.
     */
    public static final short BIT_1PLUS1_PROTECTION_CAPABLE = 0;

    /**
     * Indicates that the TTP is disabled.
     */
    public static final short BIT_DISABLED = 1;

    /**
     * Indicates that the TTP is operationally down.
     */
    public static final short BIT_STATUS_DOWN = 2;

    /**
     * Returns the tunnel termination point identifier.
     *
     * @return tunnel termination point id
     */
    long ttpId();

    /**
     * Returns the network layer switching type to which this TTP belongs.
     *
     * @return the switching type
     */
    SwitchingType switchingLayer();

    /**
     * Returns the network layer encoding type to which this TTP belongs.
     *
     * @return the encoding type
     */
    EncodingType encodingLayer();

    /**
     * Returns the flags of this TTP.
     *
     * @return the flags
     */
    BitSet flags();

    /**
     * Returns the supported inter-layer locks.
     *
     * @return list of inter-layer locks
     */
    List<Long> interLayerLockList();

    /**
     * Returns the local link connectivity list.
     *
     * @return the local link connectivity list
     */
    List<LocalLinkConnectivity> localLinkConnectivityList();

    /**
     * Returns the remaining adaptation bandwidth at each priority level.
     *
     * @return list of available adaptation bandwidth
     */
    float[] availAdaptBandwidth();

    /**
     * Returns the supporting TTP identifier.
     *
     * @return the supporting TTP key
     */
    TtpKey supportingTtpId();
}
