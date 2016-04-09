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
 * Implementation of PBB I-SID criterion (24 bits unsigned integer).
 */
public final class PbbIsidCriterion implements Criterion {
    private static final int MASK = 0xfffff;
    private final int pbbIsid;              // PBB I-SID: 24 bits

    /**
     * Constructor.
     *
     * @param pbbIsid the PBB I-SID to match (24 bits)
     */
    PbbIsidCriterion(int pbbIsid) {
        this.pbbIsid = pbbIsid & MASK;
    }

    @Override
    public Criterion.Type type() {
        return Criterion.Type.PBB_ISID;
    }

    /**
     * Gets the PBB I-SID to match.
     *
     * @return the PBB I-SID to match (24 bits)
     */
    public int pbbIsid() {
        return this.pbbIsid;
    }

    @Override
    public String toString() {
        return type().toString() + SEPARATOR + Long.toHexString(pbbIsid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), pbbIsid);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PbbIsidCriterion) {
            PbbIsidCriterion that = (PbbIsidCriterion) obj;
            return Objects.equals(pbbIsid, that.pbbIsid) &&
                    Objects.equals(this.type(), that.type());
        }
        return false;
    }
}
