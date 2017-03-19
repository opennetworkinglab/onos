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

import com.google.common.base.MoreObjects;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;

/**
 * Ofdpa pop l2 header extension instruction.
 */
public class Ofdpa3PopL2Header extends AbstractExtension implements ExtensionTreatment {

    private static final KryoNamespace APPKRYO = new KryoNamespace.Builder().build();

    /**
     * Creates a new pop l2 header instruction.
     */
    public Ofdpa3PopL2Header() {

    }

    @Override
    public ExtensionTreatmentType type() {
        return ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_POP_L2_HEADER.type();
    }

    @Override
    public void deserialize(byte[] data) {
    }

    @Override
    public byte[] serialize() {
        return APPKRYO.serialize(true);
    }

    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof Ofdpa3PopL2Header;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .toString();
    }
}
