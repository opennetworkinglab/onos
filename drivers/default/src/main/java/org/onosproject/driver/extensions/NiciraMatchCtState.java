/*
 * Copyright 2017-present Open Networking Laboratory
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

import java.util.Map;
import java.util.Objects;

import com.google.common.collect.Maps;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.ExtensionSelectorType;

import com.google.common.base.MoreObjects;
/**
 * Nicira conntrack state extension selector.
 */
public class NiciraMatchCtState extends AbstractExtension implements ExtensionSelector {

    private long ctState;
    private long ctStateMask;

    private final KryoNamespace appKryo = new KryoNamespace.Builder().build();

    /**
     * Creates a new conntrack state selector.
     */
    NiciraMatchCtState() {
        ctState = 0L;
        ctStateMask = ~0L;
    }

    /**
     * Creates a new conntrack state selector with given state.
     *
     * @param ctState conntrack state
     * @param mask conntrack state mask
     */
    public NiciraMatchCtState(long ctState, long mask) {
        this.ctState = ctState;
        this.ctStateMask = mask;
    }

    /**
     * Gets the conntrack state.
     *
     * @return ctState
     */
    public long ctState() {
        return ctState;
    }

    /**
     * Gets the conntrack state mask.
     *
     * @return ctStateMask
     */
    public long ctStateMask() {
        return ctStateMask;
    }

    @Override
    public ExtensionSelectorType type() {
        return ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_CONNTRACK_STATE.type();
    }

    @Override
    public void deserialize(byte[] data) {
        Map<String, Object> values = appKryo.deserialize(data);
        ctState = (long) values.get("ctState");
        ctStateMask = (long) values.get("ctStateMask");
    }

    @Override
    public byte[] serialize() {
        Map<String, Object> values = Maps.newHashMap();
        values.put("ctState", ctState);
        values.put("ctStateMask", ctStateMask);
        return appKryo.serialize(values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ctState, ctStateMask);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NiciraMatchCtState) {
            NiciraMatchCtState that = (NiciraMatchCtState) obj;
            return Objects.equals(ctState, that.ctState)
                    && Objects.equals(ctStateMask, that.ctStateMask);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("ctState", ctState)
                .add("mask", ctStateMask)
                .toString();
    }
}
