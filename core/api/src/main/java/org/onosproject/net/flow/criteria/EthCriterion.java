/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.flow.criteria;

import org.onlab.packet.MacAddress;

import java.util.Objects;

/**
 * Implementation of MAC address criterion.
 */
public final class EthCriterion implements Criterion {
    private final MacAddress mac;
    private final MacAddress mask;
    private final Type type;

    /**
     * Constructor.
     *
     * @param mac the source or destination MAC address to match
     * @param mask the mask for the address
     * @param type the match type. Should be either Type.ETH_DST_MASKED or
     *             Type.ETH_SRC_MASKED
     */
    EthCriterion(MacAddress mac, MacAddress mask, Type type) {
        this.mac = mac;
        this.mask = mask;
        this.type = type;
    }

    /**
     * Constructor.
     *
     * @param mac the source or destination MAC address to match
     * @param type the match type. Should be either Type.ETH_DST or
     *             Type.ETH_SRC
     */
    EthCriterion(MacAddress mac, Type type) {
        this(mac, null, type);
    }

    @Override
    public Type type() {
        return this.type;
    }

    /**
     * Gets the MAC address to match.
     *
     * @return the MAC address to match
     */
    public MacAddress mac() {
        return this.mac;
    }

    /**
     * Gets the mask for the MAC address to match.
     *
     * @return the MAC address to match
     */
    public MacAddress mask() {
        return this.mask;
    }

    @Override
    public String toString() {
        return (mask != null) ?
            type().toString() + SEPARATOR + mac + "/" + mask :
            type().toString() + SEPARATOR + mac;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type.ordinal(), mac, mask);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof EthCriterion) {
            EthCriterion that = (EthCriterion) obj;
            return Objects.equals(mac, that.mac) &&
                    Objects.equals(mask, that.mask) &&
                    Objects.equals(type, that.type);
        }
        return false;
    }
}
