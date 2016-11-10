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
package org.onosproject.net.flow.criteria;

import org.onosproject.net.flow.AbstractExtension;
import java.util.Arrays;
import java.util.Objects;
import static com.google.common.base.MoreObjects.toStringHelper;


/**
 * Unresolved extension selector.
 */
public class UnresolvedExtensionSelector extends AbstractExtension implements ExtensionSelector {

    private byte[] bytes;
    private ExtensionSelectorType unresolvedSelectorType;

    /**
     * Creates a new unresolved extension selector with given data in byte form.
     *
     * @param arraybyte byte data for the extension selector
     * @param type unresolved extension data type
     */
    public UnresolvedExtensionSelector(byte[] arraybyte, ExtensionSelectorType type) {
        this.bytes = arraybyte;
        this.unresolvedSelectorType = type;
    }

    @Override
    public byte[] serialize() {
        return bytes;
    }

    @Override
    public void deserialize(byte[] data) {
         bytes = data;
    }

    @Override
    public ExtensionSelectorType type() {
        return ExtensionSelectorType.ExtensionSelectorTypes.UNRESOLVED_TYPE.type();
    }

    @Override
    public int hashCode() {
        return Objects.hash(bytes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof UnresolvedExtensionSelector) {
            UnresolvedExtensionSelector that = (UnresolvedExtensionSelector) obj;
            return Arrays.equals(bytes, that.bytes);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(type().toString())
                .add("bytes", bytes)
                .add("unresolvedSelectorType", unresolvedSelectorType)
                .toString();
    }
}

