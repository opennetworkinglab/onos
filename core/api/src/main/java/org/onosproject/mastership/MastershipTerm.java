/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.mastership;

import java.util.Objects;

import org.onosproject.cluster.NodeId;

import com.google.common.base.MoreObjects;

public final class MastershipTerm {

    private final NodeId master;
    private final long termNumber;

    private MastershipTerm(NodeId master, long term) {
        this.master = master;
        this.termNumber = term;
    }

    public static MastershipTerm of(NodeId master, long term) {
        return new MastershipTerm(master, term);
    }

    public NodeId master() {
        return master;
    }

    public long termNumber() {
        return termNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(master, termNumber);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof MastershipTerm) {
            MastershipTerm that = (MastershipTerm) other;
            return Objects.equals(this.master, that.master) &&
                    Objects.equals(this.termNumber, that.termNumber);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("master", this.master)
                .add("termNumber", this.termNumber)
                .toString();
    }
}
