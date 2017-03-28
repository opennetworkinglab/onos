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
 * Nicira conntrack zone extension selector.
 */
public class NiciraMatchCtZone extends AbstractExtension implements ExtensionSelector {

    private int ctZone;

    private final KryoNamespace appKryo = new KryoNamespace.Builder().build();

    /**
     * Creates a new conntrack zone selector.
     */
    NiciraMatchCtZone() {
        ctZone = 0;
    }

    /**
     * Creates a new conntrack zone selector with given zone.
     *
     * @param ctZone conntrack zone
     */
    public NiciraMatchCtZone(int ctZone) {
        this.ctZone = ctZone;
    }

    /**
     * Gets the conntrack zone.
     *
     * @return ctZone
     */
    public int ctZone() {
        return ctZone;
    }

    @Override
    public ExtensionSelectorType type() {
        return ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_CONNTRACK_ZONE.type();
    }

    @Override
    public void deserialize(byte[] data) {
        ctZone = (int) (appKryo.deserialize(data));
    }

    @Override
    public byte[] serialize() {
        return appKryo.serialize(ctZone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ctZone);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NiciraMatchCtZone) {
            NiciraMatchCtZone that = (NiciraMatchCtZone) obj;
            return Objects.equals(ctZone, that.ctZone());

        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).add("ctZone", ctZone).toString();
    }
}
