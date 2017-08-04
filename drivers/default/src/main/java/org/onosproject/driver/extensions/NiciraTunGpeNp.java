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
 * Nicira tunnel gpe next protocol extension instruction to tunGpeNp value.
 */
public class NiciraTunGpeNp extends AbstractExtension implements ExtensionTreatment {

    private byte tunGpeNp;

    private final KryoNamespace appKryo = new KryoNamespace.Builder().build();

    /**
     * Creates a new NiciraTunGpeNp instruction.
     */
    NiciraTunGpeNp() {
        tunGpeNp = (byte) 0;
    }

    /**
     * Creates a new NiciraTunGpeNp instruction with given value.
     *
     * @param tunGpeNp tunnel gpe next protocol value
     */
    public NiciraTunGpeNp(byte tunGpeNp) {
        this.tunGpeNp = tunGpeNp;
    }

    /**
     * Gets the tunGpeNp.
     *
     * @return tunGpeNp
     */
    public byte tunGpeNp() {
        return tunGpeNp;
    }

    @Override
    public ExtensionTreatmentType type() {
        return ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_TUN_GPE_NP.type();
    }

    @Override
    public void deserialize(byte[] data) {
        tunGpeNp = (byte) (appKryo.deserialize(data));
    }

    @Override
    public byte[] serialize() {
        return appKryo.serialize(tunGpeNp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tunGpeNp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NiciraTunGpeNp) {
            NiciraTunGpeNp that = (NiciraTunGpeNp) obj;
            return Objects.equals(tunGpeNp, that.tunGpeNp);

        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).add("tunGpeNp", tunGpeNp).toString();
    }
}
