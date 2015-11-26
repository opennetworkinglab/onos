/*
 * Copyright 2015 Open Networking Laboratory
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
import org.onosproject.net.flow.instructions.AbstractExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.store.serializers.Ip4AddressSerializer;

import com.google.common.base.MoreObjects;

/**
 * Nicira set NSH SPI extension instruction.
 */
public class NiciraSetNshSpi extends AbstractExtensionTreatment {

    private int nshSpi;

    private final KryoNamespace appKryo = new KryoNamespace.Builder()
    .register(new Ip4AddressSerializer(), Integer.class)
    .register(byte[].class)
    .build();

    /**
     * Creates a new set nsh spi instruction.
     */
    NiciraSetNshSpi() {
        nshSpi = 0;
    }

    /**
     * Creates a new set nsh spi instruction with given spi.
     *
     * @param nshSpi nsh service path index
     */
    NiciraSetNshSpi(int nshSpi) {
        this.nshSpi = nshSpi;
    }

    /**
     * Gets the nsh service path index.
     *
     * @return nsh service path index
     */
    public int nshSpi() {
        return nshSpi;
    }

    @Override
    public ExtensionTreatmentType type() {
        return ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_SPI.type();
    }

    @Override
    public void deserialize(byte[] data) {
        nshSpi = appKryo.deserialize(data);
    }

    @Override
    public byte[] serialize() {
        return appKryo.serialize(nshSpi);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nshSpi);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NiciraSetNshSpi) {
            NiciraSetNshSpi that = (NiciraSetNshSpi) obj;
            return Objects.equals(nshSpi, that.nshSpi);

        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("nshSpi", nshSpi)
                .toString();
    }
}
