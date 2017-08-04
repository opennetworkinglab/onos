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

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;

import java.util.Map;
import java.util.Objects;

/**
 * Instruction for Oplink channel attenuation.
 */
public class OplinkAttenuation extends AbstractExtension implements ExtensionTreatment {
    private static final String KEY_ATT = "attenuation";

    private int attenuation;

    private final KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(Map.class)
            .build("OplinkAttenuation");

    /**
     * Creates new attenuation instruction.
     * @param attenuation attenuation value
     */
    public OplinkAttenuation(int attenuation) {
        this.attenuation = attenuation;
    }

    /**
     * Gets the attenuation value.
     * @return attenuation
     */
    public int getAttenuation() {
        return attenuation;
    }

    /**
     * Modify the attenuation value.
     * @param attenuation new attenuation value
     */
    public void setAttenuation(int attenuation) {
        this.attenuation = attenuation;
    }

    @Override
    public ExtensionTreatmentType type() {
        return ExtensionTreatmentType.ExtensionTreatmentTypes.OPLINK_ATTENUATION.type();
    }

    @Override
    public byte[] serialize() {
        Map<String, Object> values = Maps.newHashMap();
        values.put(KEY_ATT, attenuation);
        return appKryo.serialize(values);
    }

    @Override
    public void deserialize(byte[] data) {
        Map<String, Object> values = appKryo.deserialize(data);
        attenuation = (int) values.get(KEY_ATT);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attenuation);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OplinkAttenuation) {
            OplinkAttenuation that = (OplinkAttenuation) obj;
            return Objects.equals(attenuation, that.attenuation);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add(KEY_ATT, attenuation)
                .toString();
    }
}
