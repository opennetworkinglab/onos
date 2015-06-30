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
 * Implementation of optical signal type criterion (8 bits unsigned
 * integer).
 *
 * @deprecated in Cardinal Release
 */
@Deprecated
public final class OpticalSignalTypeCriterion implements Criterion {
    private static final short MASK = 0xff;
    private final short signalType;         // Signal type value: 8 bits
    private final Type type;

    /**
     * Constructor.
     *
     * @param signalType the optical signal type to match (8 bits unsigned
     * integer)
     * @param type the match type. Should be Type.OCH_SIGTYPE
     */
    OpticalSignalTypeCriterion(short signalType, Type type) {
        this.signalType = (short) (signalType & MASK);
        this.type = type;
    }

    @Override
    public Type type() {
        return this.type;
    }

    /**
     * Gets the optical signal type to match.
     *
     * @return the optical signal type to match (8 bits unsigned integer)
     */
    public short signalType() {
        return signalType;
    }

    @Override
    public String toString() {
        return toStringHelper(type().toString())
            .add("signalType", signalType).toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), signalType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OpticalSignalTypeCriterion) {
            OpticalSignalTypeCriterion that = (OpticalSignalTypeCriterion) obj;
            return Objects.equals(signalType, that.signalType) &&
                    Objects.equals(type, that.type);
        }
        return false;
    }
}
