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

import java.util.Objects;

/**
 * Implementation of MPLS TC criterion (3 bits).
 */
public final class MplsTcCriterion implements Criterion {
    private static final byte MASK = 0x7;
    private final byte mplsTc;

    /**
     * Constructor.
     *
     * @param mplsTc the MPLS TC to match (3 bits)
     */
    MplsTcCriterion(byte mplsTc) {
        this.mplsTc = (byte) (mplsTc & MASK);
    }

    @Override
    public Type type() {
        return Type.MPLS_TC;
    }

    /**
     * Gets the MPLS TC to match.
     *
     * @return the MPLS TC to match (3 bits)
     */
    public byte tc() {
        return mplsTc;
    }

    @Override
    public String toString() {
        return type().toString() + SEPARATOR + Long.toHexString(mplsTc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), mplsTc);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof MplsTcCriterion) {
            MplsTcCriterion that = (MplsTcCriterion) obj;
            return Objects.equals(mplsTc, that.mplsTc) &&
                    Objects.equals(this.type(), that.type());
        }
        return false;
    }
}
