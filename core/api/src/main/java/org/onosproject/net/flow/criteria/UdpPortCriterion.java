/*
 * Copyright 2015-present Open Networking Laboratory
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

import org.onlab.packet.TpPort;

import java.util.Objects;

/**
 * Implementation of UDP port criterion (16 bits unsigned integer).
 */
public final class UdpPortCriterion implements Criterion {
    private final TpPort udpPort;
    private final Type type;

    /**
     * Constructor.
     *
     * @param udpPort the UDP port to match
     * @param type the match type. Should be either Type.UDP_SRC or
     * Type.UDP_DST
     */
    UdpPortCriterion(TpPort udpPort, Type type) {
        this.udpPort = udpPort;
        this.type = type;
    }

    @Override
    public Type type() {
        return this.type;
    }

    /**
     * Gets the UDP port to match.
     *
     * @return the UDP port to match
     */
    public TpPort udpPort() {
        return this.udpPort;
    }

    @Override
    public String toString() {
        return type().toString() + SEPARATOR + udpPort;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), udpPort);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof UdpPortCriterion) {
            UdpPortCriterion that = (UdpPortCriterion) obj;
            return Objects.equals(udpPort, that.udpPort) &&
                    Objects.equals(type, that.type);
        }
        return false;
    }
}
