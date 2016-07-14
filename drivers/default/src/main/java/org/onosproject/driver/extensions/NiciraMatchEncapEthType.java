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

import java.util.Objects;

import org.onlab.util.KryoNamespace;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.ExtensionSelectorType;

import com.google.common.base.MoreObjects;

/**
 * Nicira EncapEthType extension selector to set encapsulated eth type.
 */
public class NiciraMatchEncapEthType extends AbstractExtension implements ExtensionSelector {

    private short encapEthType;

    private final KryoNamespace appKryo = new KryoNamespace.Builder().build();

    /**
     * Creates a new nshEncapEthType selector.
     */
    NiciraMatchEncapEthType() {
        encapEthType = (short) 0;
    }

    /**
     * Creates a new nshEncapEthType selector with given eth type.
     *
     * @param encapEthType encapsulated ethernet type
     */
    public NiciraMatchEncapEthType(short encapEthType) {
        this.encapEthType = encapEthType;
    }

    /**
     * Gets the encapEthType.
     *
     * @return encapEthType
     */
    public short encapEthType() {
        return encapEthType;
    }

    @Override
    public ExtensionSelectorType type() {
        return ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_ENCAP_ETH_TYPE.type();
    }

    @Override
    public void deserialize(byte[] data) {
        encapEthType = (short) (appKryo.deserialize(data));
    }

    @Override
    public byte[] serialize() {
        return appKryo.serialize(encapEthType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(encapEthType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NiciraMatchEncapEthType) {
            NiciraMatchEncapEthType that = (NiciraMatchEncapEthType) obj;
            return Objects.equals(encapEthType, that.encapEthType);

        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).add("encapEthType", encapEthType).toString();
    }
}
