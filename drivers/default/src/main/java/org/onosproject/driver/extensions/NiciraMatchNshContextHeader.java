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

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

import org.onlab.util.KryoNamespace;
import org.onosproject.net.NshContextHeader;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.ExtensionSelectorType;
/**
 * Implementation of Nsh context header criterion.
 */
public final class NiciraMatchNshContextHeader  extends AbstractExtension implements ExtensionSelector {
    private NshContextHeader nshContextHeader;
    private ExtensionSelectorType type;

    private final KryoNamespace appKryo = new KryoNamespace.Builder().build();

    /**
     * Constructor to create Nsh context header.
     *
     * @param type extension selector type
     */
    public NiciraMatchNshContextHeader(ExtensionSelectorType type) {
        this.nshContextHeader = null;
        this.type = type;
    }

    /**
     * Gets the nsh context header to match.
     *
     * @return the nsh context header to match
     */
    public NshContextHeader nshContextHeader() {
        return nshContextHeader;
    }

    @Override
    public byte[] serialize() {
        return appKryo.serialize(nshContextHeader.nshContextHeader());
    }

    @Override
    public void deserialize(byte[] data) {
        nshContextHeader = nshContextHeader.of(appKryo.deserialize(data));

    }

    @Override
    public ExtensionSelectorType type() {
        return type;
    }

    @Override
    public String toString() {
        return toStringHelper(type().toString())
                .add("nshContextHeader", nshContextHeader.toString())
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(type(), nshContextHeader);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NiciraMatchNshContextHeader) {
            NiciraMatchNshContextHeader that = (NiciraMatchNshContextHeader) obj;
            return Objects.equals(nshContextHeader, that.nshContextHeader) &&
                    Objects.equals(this.type(), that.type());
        }
        return false;
    }
}
