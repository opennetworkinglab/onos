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
 * Implementation of SCTP port criterion (16 bits unsigned integer).
 */
public final class SctpPortCriterion implements Criterion {
    private final TpPort sctpPort;
    private final Type type;

    /**
     * Constructor.
     *
     * @param sctpPort the SCTP port to match
     * @param type the match type. Should be either Type.SCTP_SRC or
     * Type.SCTP_DST
     */
    SctpPortCriterion(TpPort sctpPort, Type type) {
        this.sctpPort = sctpPort;
        this.type = type;
    }

    @Override
    public Type type() {
        return this.type;
    }

    /**
     * Gets the SCTP port to match.
     *
     * @return the SCTP port to match
     */
    public TpPort sctpPort() {
        return this.sctpPort;
    }

    @Override
    public String toString() {
        return type().toString() + SEPARATOR + sctpPort;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), sctpPort);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SctpPortCriterion) {
            SctpPortCriterion that = (SctpPortCriterion) obj;
            return Objects.equals(sctpPort, that.sctpPort) &&
                    Objects.equals(type, that.type);
        }
        return false;
    }
}
