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
 * Nicira EncapEthSrc extension instruction to set encapsulated eth source.
 */
public class NiciraEncapEthSrc extends AbstractExtension implements ExtensionTreatment {

    private MacAddress encapEthSrc;

    private final KryoNamespace appKryo = new KryoNamespace.Builder()
    .register(new MacAddressSerializer(), MacAddress.class).register(byte[].class).build();

    /**
     * Creates a new nshEncapEthSrc instruction.
     */
    NiciraEncapEthSrc() {
    }

    /**
     * Creates a new encapEthSrc instruction with given mac address.
     *
     * @param encapEthSrc encapsulated ethernet source
     */
    public NiciraEncapEthSrc(MacAddress encapEthSrc) {
        this.encapEthSrc = encapEthSrc;
    }

    /**
     * Gets the encapEthSrc.
     *
     * @return encapEthSrc
     */
    public MacAddress encapEthSrc() {
        return encapEthSrc;
    }

    @Override
    public ExtensionTreatmentType type() {
        return ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_ENCAP_ETH_SRC.type();
    }

    @Override
    public void deserialize(byte[] data) {
        encapEthSrc = appKryo.deserialize(data);
    }

    @Override
    public byte[] serialize() {
        return appKryo.serialize(encapEthSrc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(encapEthSrc);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NiciraEncapEthSrc) {
            NiciraEncapEthSrc that = (NiciraEncapEthSrc) obj;
            return Objects.equals(encapEthSrc, that.encapEthSrc);

        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).add("encapEthSrc", encapEthSrc).toString();
    }
}
