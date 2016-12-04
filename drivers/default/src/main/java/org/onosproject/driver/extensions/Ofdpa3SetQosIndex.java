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

import java.util.Objects;

/**
 * OFDPA set Qos Index extension instruction.
 */
public class Ofdpa3SetQosIndex extends AbstractExtension implements ExtensionTreatment {
    /**
     * Byte storing the Qos index.
     */
    private int qosIndex;

    private static final KryoNamespace APPKRYO = new KryoNamespace.Builder()
            .build();

    /**
     * Constructs a new set Qos index instruction.
     */
    protected Ofdpa3SetQosIndex() {
        qosIndex = 0x00;
    }

    /**
     * Constructs a new set Qos index instruction with a given int.
     *
     * @param qosindex Qos index as integer
     */
    public Ofdpa3SetQosIndex(int qosindex) {
        qosIndex = qosindex;
    }

    /**
     * Gets the Qos index as int.
     *
     * @return the Qos index as int.
     */
    public int qosIndex() {
        return qosIndex;
    }

    @Override
    public ExtensionTreatmentType type() {
        return ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_SET_QOS_INDEX.type();
    }

    @Override
    public void deserialize(byte[] data) {
        qosIndex = APPKRYO.deserialize(data);
    }

    @Override
    public byte[] serialize() {
        return APPKRYO.serialize(qosIndex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(qosIndex);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Ofdpa3SetQosIndex) {
            Ofdpa3SetQosIndex that = (Ofdpa3SetQosIndex) obj;
            return Objects.equals(qosIndex, that.qosIndex);

        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("qosIndex", qosIndex)
                .toString();
    }
}