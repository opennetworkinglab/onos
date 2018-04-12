/*
 * Copyright 2018-present Open Networking Foundation
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
 * OFDPA set Allow  Vlan Translation extension instruction.
 */
public class OfdpaSetAllowVlanTranslation extends AbstractExtension implements ExtensionTreatment {
    private byte vlanTranslation;

    private static final KryoNamespace APPKRYO = new KryoNamespace.Builder()
            .register(Ofdpa3AllowVlanTranslationType.class)
            .build();

    protected OfdpaSetAllowVlanTranslation() {
        vlanTranslation = Ofdpa3AllowVlanTranslationType.NOT_ALLOW.getValue();
    }

    public OfdpaSetAllowVlanTranslation(byte allow) {
        this.vlanTranslation = allow;
    }

    public OfdpaSetAllowVlanTranslation(Ofdpa3AllowVlanTranslationType allow) {
        this.vlanTranslation = allow.getValue();
    }

    public byte getVlanTranslation() {
        return vlanTranslation;
    }

    @Override
    public ExtensionTreatmentType type() {
        return ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_ALLOW_VLAN_TRANSLATION.type();
    }

    @Override
    public void deserialize(byte[] data) {
        vlanTranslation = APPKRYO.deserialize(data);
    }

    @Override
    public byte[] serialize() {
        return APPKRYO.serialize(vlanTranslation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vlanTranslation);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OfdpaSetAllowVlanTranslation) {
            OfdpaSetAllowVlanTranslation that = (OfdpaSetAllowVlanTranslation) obj;
            return Objects.equals(vlanTranslation, that.vlanTranslation);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("ofdpaSetAllowVlanTranslation", vlanTranslation)
                .toString();
    }
}
