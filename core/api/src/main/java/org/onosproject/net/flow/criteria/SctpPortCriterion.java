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
 * Implementation of SCTP port criterion (16 bits unsigned integer).
 */
public final class SctpPortCriterion implements Criterion {
    private static final int MASK = 0xffff;
    private final int sctpPort;             // Port value: 16 bits
    private final Type type;

    /**
     * Constructor.
     *
     * @param sctpPort the SCTP port to match (16 bits unsigned integer)
     * @param type the match type. Should be either Type.SCTP_SRC or
     * Type.SCTP_DST
     */
    SctpPortCriterion(int sctpPort, Type type) {
        this.sctpPort = sctpPort & MASK;
        this.type = type;
    }

    @Override
    public Type type() {
        return this.type;
    }

    /**
     * Gets the SCTP port to match.
     *
     * @return the SCTP port to match (16 bits unsigned integer)
     */
    public int sctpPort() {
        return this.sctpPort;
    }

    @Override
    public String toString() {
        return toStringHelper(type().toString())
            .add("sctpPort", sctpPort).toString();
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
