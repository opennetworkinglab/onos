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

package org.onosproject.net.flow.instructions;

import com.google.common.base.MoreObjects;
import org.onosproject.net.flow.AbstractExtension;
import java.util.Arrays;

/**
 * Unresolved extension treatment.
 */
public class UnresolvedExtensionTreatment extends AbstractExtension implements ExtensionTreatment {

    private byte[] bytes;
    private ExtensionTreatmentType unresolvedTreatmentType;

    /**
     * Creates a new unresolved extension treatment with given data in byte form.
     *
     * @param arraybyte byte data for treatment
     * @param type unresolved extension data type
     */
    public UnresolvedExtensionTreatment(byte[] arraybyte, ExtensionTreatmentType type) {
        this.bytes = arraybyte;
        this.unresolvedTreatmentType = type;
    }

    @Override
    public ExtensionTreatmentType type() {
        return unresolvedTreatmentType;
    }

    @Override
    public void deserialize(byte[] data) {
        bytes = data;
    }

    @Override
    public byte[] serialize() {
        return bytes;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof UnresolvedExtensionTreatment) {
            UnresolvedExtensionTreatment that = (UnresolvedExtensionTreatment) obj;
            return Arrays.equals(bytes, that.bytes);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("bytes", bytes)
                .add("unresolvedTreatmentType", unresolvedTreatmentType)
                .toString();
    }
}
