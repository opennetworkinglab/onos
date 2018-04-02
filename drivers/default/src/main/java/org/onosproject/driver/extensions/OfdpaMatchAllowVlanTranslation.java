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

import org.onlab.util.KryoNamespace;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.ExtensionSelectorType;

import java.util.Objects;

/**
 * OFDPA ALLOW_VLAN_TRANSLATION extension match.
 */
public class OfdpaMatchAllowVlanTranslation extends AbstractExtension implements ExtensionSelector {
    private static final KryoNamespace APPKRYO = new KryoNamespace.Builder()
            .register(Short.class)
            .build();

    private Short allowVlanTranslation;

    /**
     * OFDPA ALLOW_VLAN_TRANSLATION extension match.
     */
    public OfdpaMatchAllowVlanTranslation() {
        allowVlanTranslation = 0;
    }

    /**
     * Constructs new ALLOW_VLAN_TRANSLATION match with given boolean data.
     *
     * @param allowVlanTranslation allows vlan translation
     */
    public OfdpaMatchAllowVlanTranslation(Short allowVlanTranslation) {
        this.allowVlanTranslation = allowVlanTranslation;
    }

    /**
     * Gets allow vlan translation flag.
     *
     * @return allowVlanTranslation field
     */
    public Short allowVlanTranslation() {
        return allowVlanTranslation;
    }

    @Override
    public ExtensionSelectorType type() {
        return ExtensionSelectorType.ExtensionSelectorTypes.OFDPA_MATCH_ALLOW_VLAN_TRANSLATION.type();
    }

    @Override
    public byte[] serialize() {
        return APPKRYO.serialize(allowVlanTranslation);
    }

    @Override
    public void deserialize(byte[] data) {
        allowVlanTranslation = APPKRYO.deserialize(data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(allowVlanTranslation);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null || !obj.getClass().equals(OfdpaMatchAllowVlanTranslation.class)) {
            return false;
        }

        OfdpaMatchAllowVlanTranslation that = (OfdpaMatchAllowVlanTranslation) obj;
        return that.allowVlanTranslation().equals(allowVlanTranslation);
    }
}
