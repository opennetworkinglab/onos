/*
 * Copyright 2015 Open Networking Laboratory
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

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Implementation of TCP port criterion (16 bits unsigned integer).
 */
public final class TcpPortCriterion implements Criterion {
    private static final int MASK = 0xffff;
    private final int tcpPort;              // Port value: 16 bits
    private final Type type;

    /**
     * Constructor.
     *
     * @param tcpPort the TCP port to match (16 bits unsigned integer)
     * @param type the match type. Should be either Type.TCP_SRC or
     * Type.TCP_DST
     */
    TcpPortCriterion(int tcpPort, Type type) {
        this.tcpPort = tcpPort & MASK;
        this.type = type;
    }

    @Override
    public Type type() {
        return this.type;
    }

    /**
     * Gets the TCP port to match.
     *
     * @return the TCP port to match (16 bits unsigned integer)
     */
    public int tcpPort() {
        return this.tcpPort;
    }

    @Override
    public String toString() {
        return toStringHelper(type().toString())
            .add("tcpPort", tcpPort).toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), tcpPort);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TcpPortCriterion) {
            TcpPortCriterion that = (TcpPortCriterion) obj;
            return Objects.equals(tcpPort, that.tcpPort) &&
                    Objects.equals(type, that.type);
        }
        return false;
    }
}
