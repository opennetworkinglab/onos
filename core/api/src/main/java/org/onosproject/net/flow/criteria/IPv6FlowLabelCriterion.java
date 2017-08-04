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

import java.util.Objects;

/**
 * Implementation of IPv6 Flow Label (RFC 6437) criterion (20 bits unsigned
 * integer).
 */
public final class IPv6FlowLabelCriterion implements Criterion {
    private static final int MASK = 0xfffff;
    private final int flowLabel;            // IPv6 flow label: 20 bits

    /**
     * Constructor.
     *
     * @param flowLabel the IPv6 flow label to match (20 bits)
     */
    IPv6FlowLabelCriterion(int flowLabel) {
        this.flowLabel = flowLabel & MASK;
    }

    @Override
    public Type type() {
        return Type.IPV6_FLABEL;
    }

    /**
     * Gets the IPv6 flow label to match.
     *
     * @return the IPv6 flow label to match (20 bits)
     */
    public int flowLabel() {
        return flowLabel;
    }

    @Override
    public String toString() {
        return type().toString() + SEPARATOR + Long.toHexString(flowLabel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), flowLabel);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof IPv6FlowLabelCriterion) {
            IPv6FlowLabelCriterion that = (IPv6FlowLabelCriterion) obj;
            return Objects.equals(flowLabel, that.flowLabel) &&
                    Objects.equals(this.type(), that.type());
        }
        return false;
    }
}
