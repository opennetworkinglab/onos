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
 * Nicira nshMdType extension instruction.
 */
public class NiciraNshMdType extends AbstractExtension implements ExtensionTreatment {

    private byte nshMdType;

    private final KryoNamespace appKryo = new KryoNamespace.Builder().build();

    /**
     * Creates a new nshMdType instruction.
     */
    NiciraNshMdType() {
        nshMdType = (byte) 0;
    }

    /**
     * Creates a new nshMdType instruction with given nsh md type.
     *
     * @param nshMdType nsh md type
     */
    public NiciraNshMdType(byte nshMdType) {
        this.nshMdType = nshMdType;
    }

    /**
     * Gets the nsh md type.
     *
     * @return nshMdType
     */
    public byte nshMdType() {
        return nshMdType;
    }

    @Override
    public ExtensionTreatmentType type() {
        return ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_NSH_MDTYPE.type();
    }

    @Override
    public void deserialize(byte[] data) {
        nshMdType = (byte) (appKryo.deserialize(data));
    }

    @Override
    public byte[] serialize() {
        return appKryo.serialize(nshMdType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nshMdType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NiciraNshMdType) {
            NiciraNshMdType that = (NiciraNshMdType) obj;
            return Objects.equals(nshMdType, that.nshMdType);

        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).add("nshMdType", nshMdType).toString();
    }
}
