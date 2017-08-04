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

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

import org.onlab.util.KryoNamespace;
import org.onosproject.net.NshServicePathId;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.ExtensionSelectorType;

/**
 * Implementation of NSH Service Path Id selector.
 */
public final class NiciraMatchNshSpi extends AbstractExtension implements ExtensionSelector  {
    private NshServicePathId nshSpi;

    private final KryoNamespace appKryo = new KryoNamespace.Builder().build();

    /**
     * Default constructor.
     */
    public NiciraMatchNshSpi() {
        this.nshSpi = null;
    }

    /**
     * Creates an instance with initialized Nsh Service Path ID.
     *
     * @param nshSpi nsh service path ID
     */
    public NiciraMatchNshSpi(NshServicePathId nshSpi) {
        this.nshSpi = nshSpi;
    }

    /**
     * Gets the network service path id to match.
     *
     * @return the nshSpi to match
     */
    public NshServicePathId nshSpi() {
        return nshSpi;
    }

    @Override
    public ExtensionSelectorType type() {
        return ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_SPI.type();
    }

    @Override
    public byte[] serialize() {
        return appKryo.serialize(nshSpi);
    }

    @Override
    public void deserialize(byte[] data) {
        nshSpi = NshServicePathId.of(appKryo.deserialize(data));
    }

    @Override
    public String toString() {
        return toStringHelper(type().toString())
                .add("nshSpi", nshSpi.toString())
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(type(), nshSpi);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NiciraMatchNshSpi) {
            NiciraMatchNshSpi that = (NiciraMatchNshSpi) obj;
            return Objects.equals(nshSpi, that.nshSpi) &&
                    Objects.equals(this.type(), that.type());
        }
        return false;
    }
}
