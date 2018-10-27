/*
 * Copyright 2017-present Open Networking Foundation
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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.Maps;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.ExtensionSelectorType;

import com.google.common.base.MoreObjects;

/**
 * Nicira conntrack mark extension selector.
 */
public class NiciraMatchCtMark extends AbstractExtension implements ExtensionSelector {

    private long ctMark;
    private long ctMarkMask;

    private final KryoNamespace appKryo = new KryoNamespace.Builder()
                                                    .register(HashMap.class)
                                                    .build();

    /**
     * Creates a new conntrack mark selector.
     */
    NiciraMatchCtMark() {
        ctMark = 0L;
        ctMarkMask = ~0L;
    }

    /**
     * Creates a new conntrack state selector with given mark.
     *
     * @param ctMark conntrack mark
     */
    public NiciraMatchCtMark(long ctMark) {
        this.ctMark = ctMark;
        this.ctMarkMask = ~0L;
    }

    /**
     * Creates a new conntrack state selector with given mark.
     *
     * @param ctMark conntrack mark
     * @param mask   conntrack mark mask
     */
    public NiciraMatchCtMark(long ctMark, long mask) {
        this.ctMark = ctMark;
        this.ctMarkMask = mask;
    }

    /**
     * Gets the conntrack mark.
     *
     * @return ctMark
     */
    public long ctMark() {
        return ctMark;
    }

    /**
     * Gets the conntrack mark mask.
     *
     * @return ctMarkMask
     */
    public long ctMarkMask() {
        return ctMarkMask;
    }

    @Override
    public ExtensionSelectorType type() {
        return ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_CONNTRACK_MARK.type();
    }

    @Override
    public void deserialize(byte[] data) {
        Map<String, Object> values = appKryo.deserialize(data);
        ctMark = (long) values.get("ctMark");
        ctMarkMask = (long) values.get("ctMarkMask");
    }

    @Override
    public byte[] serialize() {
        Map<String, Object> values = Maps.newHashMap();
        values.put("ctMark", ctMark);
        values.put("ctMarkMask", ctMarkMask);
        return appKryo.serialize(values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ctMark, ctMarkMask);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NiciraMatchCtMark) {
            NiciraMatchCtMark that = (NiciraMatchCtMark) obj;
            return Objects.equals(ctMark, that.ctMark)
                    && Objects.equals(ctMarkMask, that.ctMarkMask);

        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("ctMark", ctMark)
                .add("ctMarkMask", ctMarkMask)
                .toString();
    }
}
