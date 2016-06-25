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
import org.onosproject.net.NshServicePathId;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;

import com.google.common.base.MoreObjects;

/**
 * Nicira set NSH SPI extension instruction.
 */
public class NiciraSetNshSpi extends AbstractExtension implements
        ExtensionTreatment {

    private NshServicePathId nshSpi;

    private final KryoNamespace appKryo = new KryoNamespace.Builder().build();

    /**
     * Creates a new set nsh spi instruction.
     */
    NiciraSetNshSpi() {
        nshSpi = NshServicePathId.of(0);
    }

    /**
     * Creates a new set nsh spi instruction with given spi.
     *
     * @param nshSpi nsh service path id
     */
    public NiciraSetNshSpi(NshServicePathId nshSpi) {
        this.nshSpi = nshSpi;
    }

    /**
     * Gets the nsh service path id.
     *
     * @return nsh service path id
     */
    public NshServicePathId nshSpi() {
        return nshSpi;
    }

    @Override
    public ExtensionTreatmentType type() {
        return ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_SPI.type();
    }

    @Override
    public void deserialize(byte[] data) {
        nshSpi = NshServicePathId.of(appKryo.deserialize(data));
    }

    @Override
    public byte[] serialize() {
        return appKryo.serialize(nshSpi.servicePathId());
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
                .add("nshSpi", nshSpi.toString())
                .toString();
    }
}
