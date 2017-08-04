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

import org.onlab.util.KryoNamespace;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;

/**
 * Nicira push nsh extension instruction.
 */
public class NiciraPushNsh extends AbstractExtension implements ExtensionTreatment {

    private final KryoNamespace appKryo = new KryoNamespace.Builder().build();

    /**
     * Creates a new push nsh instruction.
     */
    public NiciraPushNsh() {

    }

    @Override
    public ExtensionTreatmentType type() {
        return ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_PUSH_NSH.type();
    }

    @Override
    public void deserialize(byte[] data) {
    }

    @Override
    public byte[] serialize() {
        return appKryo.serialize(0);
    }

    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof NiciraPushNsh;
    }
}
