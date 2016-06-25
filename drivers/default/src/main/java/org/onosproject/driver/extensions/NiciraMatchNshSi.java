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

import org.onlab.util.KryoNamespace;
import org.onosproject.net.NshServiceIndex;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.ExtensionSelectorType;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
/**
 * Implementation of NSH Service Index(SI).
 */
public final class NiciraMatchNshSi extends AbstractExtension implements ExtensionSelector {

    private NshServiceIndex nshSi;

    private final KryoNamespace appKryo = new KryoNamespace.Builder().build();

    /**
     * Default constructor.
     *
     */
    public NiciraMatchNshSi() {
        this.nshSi = null;
    }

    /**
     * Creates an instance with initialized Nsh Service Index.
     *
     * @param nshSi nsh service index
     */
    public NiciraMatchNshSi(NshServiceIndex nshSi) {
        this.nshSi = nshSi;
    }

    /**
     * Gets the nsh service index to match.
     *
     * @return the si to match
     */
    public NshServiceIndex nshSi() {
        return nshSi;
    }

    @Override
    public byte[] serialize() {
        return appKryo.serialize(nshSi.serviceIndex());
    }

    @Override
    public void deserialize(byte[] data) {
        nshSi = NshServiceIndex.of(appKryo.deserialize(data));
    }

    @Override
    public ExtensionSelectorType type() {
        return ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_SI.type();
    }

    @Override
    public String toString() {
        return toStringHelper(type().toString())
                .add("nshSi", nshSi.toString())
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(type(), nshSi);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NiciraMatchNshSi) {
            NiciraMatchNshSi that = (NiciraMatchNshSi) obj;
            return Objects.equals(nshSi, that.nshSi);
        }
        return false;
    }
}
