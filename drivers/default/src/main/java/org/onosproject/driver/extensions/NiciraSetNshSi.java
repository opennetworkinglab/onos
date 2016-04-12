/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.driver.extensions;

import java.util.Objects;

import org.onlab.util.KryoNamespace;
import org.onosproject.net.NshServiceIndex;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;

import com.google.common.base.MoreObjects;

/**
 * Nicira set NSH SI extension instruction.
 */
public class NiciraSetNshSi extends AbstractExtension implements
        ExtensionTreatment {

    private NshServiceIndex nshSi;

    private final KryoNamespace appKryo = new KryoNamespace.Builder().build();

    /**
     * Creates a new set nsh si instruction.
     */
    NiciraSetNshSi() {
        nshSi = NshServiceIndex.of((short) 0);
    }

    /**
     * Creates a new set nsh si instruction with given si.
     *
     * @param nshSi nsh service index
     */
    public NiciraSetNshSi(NshServiceIndex nshSi) {
        this.nshSi = nshSi;
    }

    /**
     * Gets the nsh service index.
     *
     * @return nsh service index
     */
    public NshServiceIndex nshSi() {
        return nshSi;
    }

    @Override
    public ExtensionTreatmentType type() {
        return ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_SI.type();
    }

    @Override
    public void deserialize(byte[] data) {
        nshSi = NshServiceIndex.of(appKryo.deserialize(data));
    }

    @Override
    public byte[] serialize() {
        return appKryo.serialize(nshSi.serviceIndex());
    }

    @Override
    public int hashCode() {
        return Objects.hash(nshSi);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NiciraSetNshSi) {
            NiciraSetNshSi that = (NiciraSetNshSi) obj;
            return Objects.equals(nshSi, that.nshSi);

        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("nshSi", nshSi)
                .toString();
    }
}
