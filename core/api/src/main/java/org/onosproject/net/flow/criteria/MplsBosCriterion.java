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
 * Implementation of MPLS BOS criterion (1 bit).
 */
public class MplsBosCriterion implements Criterion {
    private boolean mplsBos;

    MplsBosCriterion(boolean mplsBos) {
        this.mplsBos = mplsBos;
    }

    @Override
    public Type type() {
        return Type.MPLS_BOS;
    }

    public boolean mplsBos() {
        return mplsBos;
    }

    @Override
    public String toString() {
        return type().toString() + SEPARATOR + mplsBos;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), mplsBos);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof MplsBosCriterion) {
            MplsBosCriterion that = (MplsBosCriterion) obj;
            return Objects.equals(mplsBos, that.mplsBos()) &&
                    Objects.equals(this.type(), that.type());
        }
        return false;
    }
}
