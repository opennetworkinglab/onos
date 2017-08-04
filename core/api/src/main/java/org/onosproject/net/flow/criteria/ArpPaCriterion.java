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

import org.onlab.packet.Ip4Address;

import java.util.Objects;

/**
 * Implementation of arp spa or tpa address criterion.
 */
public final class ArpPaCriterion implements Criterion {
    private final Ip4Address ip;
    private final Type type;

    /**
     * Constructor.
     *
     * @param ip the Ip4 Address to match.
     * @param type the match type. Should be one of the following:
     * Type.ARP_SPA, Type.ARP_TPA
     */
    ArpPaCriterion(Ip4Address ip, Type type) {
        this.ip = ip;
        this.type = type;
    }

    @Override
    public Type type() {
        return this.type;
    }

    /**
     * Gets the Ip4 Address to match.
     *
     * @return the Ip4 Address to match
     */
    public Ip4Address ip() {
        return this.ip;
    }

    @Override
    public String toString() {
        return type().toString() + SEPARATOR + ip;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), ip);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ArpPaCriterion) {
            ArpPaCriterion that = (ArpPaCriterion) obj;
            return Objects.equals(ip, that.ip) &&
                    Objects.equals(type, that.type);
        }
        return false;
    }
}
