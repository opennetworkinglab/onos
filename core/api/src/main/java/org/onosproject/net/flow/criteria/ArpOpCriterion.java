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
 * Implementation of arp operation type criterion.
 */
public final class ArpOpCriterion implements Criterion {
    private final int arpOp;
    private final Type type;

    /**
     * Constructor.
     *
     * @param arpOp the arp operation type to match.
     * @param type the match type. Should be the following:
     * Type.ARP_OP
     */
    ArpOpCriterion(int arpOp, Type type) {
        this.arpOp = arpOp;
        this.type = type;
    }

    @Override
    public Type type() {
        return this.type;
    }

    /**
     * Gets the arp operation type to match.
     *
     * @return the arp operation type to match
     */
    public int arpOp() {
        return this.arpOp;
    }

    @Override
    public String toString() {
        return type().toString() + SEPARATOR + arpOp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), arpOp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ArpOpCriterion) {
            ArpOpCriterion that = (ArpOpCriterion) obj;
            return Objects.equals(arpOp, that.arpOp) &&
                    Objects.equals(type, that.type);
        }
        return false;
    }
}
