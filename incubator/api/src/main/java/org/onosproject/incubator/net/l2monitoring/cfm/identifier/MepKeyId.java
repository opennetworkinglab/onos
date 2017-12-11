/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.incubator.net.l2monitoring.cfm.identifier;

import org.onosproject.incubator.net.l2monitoring.cfm.Mep;

/**
 * Immutable class to represent a unique identifier of a Mep.
 */
public class MepKeyId {
    private MdId mdId;
    private MaIdShort maId;
    private MepId mepId;

    public MepKeyId(MdId mdId, MaIdShort maId, MepId mepId) {
        this.mdId = mdId;
        this.maId = maId;
        this.mepId = mepId;
        if (mdId == null || maId == null || mepId == null) {
            throw new IllegalArgumentException("Arguments to MepKeyId constructor cannot be null");
        }
    }

    public MepKeyId(Mep mep) {
        this.mdId = mep.mdId();
        this.maId = mep.maId();
        this.mepId = mep.mepId();
    }

    public MdId mdId() {
        return mdId;
    }

    public MaIdShort maId() {
        return maId;
    }

    public MepId mepId() {
        return mepId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MepKeyId mepKeyId = (MepKeyId) o;

        if (mdId != null ? !mdId.equals(mepKeyId.mdId) : mepKeyId.mdId != null) {
            return false;
        }
        if (maId != null ? !maId.equals(mepKeyId.maId) : mepKeyId.maId != null) {
            return false;
        }
        return mepId != null ? mepId.equals(mepKeyId.mepId) : mepKeyId.mepId == null;
    }

    @Override
    public int hashCode() {
        int result = mdId != null ? mdId.hashCode() : 0;
        result = 31 * result + (maId != null ? maId.hashCode() : 0);
        result = 31 * result + (mepId != null ? mepId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return mdId.mdName() + "/" + maId.maName() + "/" + mepId();
    }
}
