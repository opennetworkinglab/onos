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

import org.onlab.packet.MplsLabel;

import java.util.Objects;

/**
 * Implementation of MPLS tag criterion (20 bits).
 */
public final class MplsCriterion implements Criterion {
    private static final int MASK = 0xfffff;
    private final MplsLabel mplsLabel;

    MplsCriterion(MplsLabel mplsLabel) {
        this.mplsLabel = mplsLabel;
    }

    @Override
    public Type type() {
        return Type.MPLS_LABEL;
    }

    public MplsLabel label() {
        return mplsLabel;
    }

    @Override
    public String toString() {
        return type().toString() + SEPARATOR + mplsLabel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), mplsLabel);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof MplsCriterion) {
            MplsCriterion that = (MplsCriterion) obj;
            return Objects.equals(mplsLabel, that.mplsLabel) &&
                    Objects.equals(this.type(), that.type());
        }
        return false;
    }
}
