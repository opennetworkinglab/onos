/*
 * Copyright 2016-present Open Networking Foundation
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
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;

import com.google.common.base.MoreObjects;

/**
 * Nicira nshNp extension instruction to set next protocol value in nsh header.
 */
public class NiciraNshNp extends AbstractExtension implements ExtensionTreatment {

    private byte nshNp;

    private final KryoNamespace appKryo = new KryoNamespace.Builder().build();

    /**
     * Creates a new nshNp instruction.
     */
    NiciraNshNp() {
        nshNp = (byte) 0;
    }

    /**
     * Creates a new nshNp instruction with given nsh np.
     *
     * @param nshNp nsh next protocol value
     */
    public NiciraNshNp(byte nshNp) {
        this.nshNp = nshNp;
    }

    /**
     * Gets the nsh np.
     *
     * @return nshNp
     */
    public byte nshNp() {
        return nshNp;
    }

    @Override
    public ExtensionTreatmentType type() {
        return ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_NSH_NP.type();
    }

    @Override
    public void deserialize(byte[] data) {
        nshNp = (byte) (appKryo.deserialize(data));
    }

    @Override
    public byte[] serialize() {
        return appKryo.serialize(nshNp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nshNp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NiciraNshNp) {
            NiciraNshNp that = (NiciraNshNp) obj;
            return Objects.equals(nshNp, that.nshNp);

        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).add("nshNp", nshNp).toString();
    }
}
