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

import org.onlab.packet.MacAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.store.serializers.MacAddressSerializer;

import com.google.common.base.MoreObjects;

/**
 * Nicira EncapEthDst extension instruction to set encapsulated eth destination.
 */
public class NiciraEncapEthDst extends AbstractExtension implements ExtensionTreatment {

    private MacAddress encapEthDst;

    private final KryoNamespace appKryo = new KryoNamespace.Builder()
    .register(new MacAddressSerializer(), MacAddress.class).register(byte[].class).build();

    /**
     * Creates a new nshEncapEthDst instruction.
     */
    NiciraEncapEthDst() {
    }

    /**
     * Creates a new encapEthDst instruction with given mac address.
     *
     * @param encapEthDst encapsulated ethernet destination
     */
    public NiciraEncapEthDst(MacAddress encapEthDst) {
        this.encapEthDst = encapEthDst;
    }

    /**
     * Gets the encapEthDst.
     *
     * @return encapEthDst
     */
    public MacAddress encapEthDst() {
        return encapEthDst;
    }

    @Override
    public ExtensionTreatmentType type() {
        return ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_ENCAP_ETH_DST.type();
    }

    @Override
    public void deserialize(byte[] data) {
        encapEthDst = appKryo.deserialize(data);
    }

    @Override
    public byte[] serialize() {
        return appKryo.serialize(encapEthDst);
    }

    @Override
    public int hashCode() {
        return Objects.hash(encapEthDst);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NiciraEncapEthDst) {
            NiciraEncapEthDst that = (NiciraEncapEthDst) obj;
            return Objects.equals(encapEthDst, that.encapEthDst);

        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).add("encapEthDst", encapEthDst).toString();
    }
}
