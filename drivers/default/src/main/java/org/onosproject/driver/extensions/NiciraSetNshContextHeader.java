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

import java.util.Map;
import java.util.Objects;

import org.onlab.util.KryoNamespace;
import org.onosproject.net.NshContextHeader;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;

/**
 * Nicira set NSH Context header extension instruction.
 */
public class NiciraSetNshContextHeader extends AbstractExtension implements
        ExtensionTreatment {

    private NshContextHeader nshCh;
    private ExtensionTreatmentType type;

    private final KryoNamespace appKryo = new KryoNamespace.Builder().build();

    /**
     * Creates a new set nsh context header instruction.
     *
     * @param type extension treatment type
     */
    NiciraSetNshContextHeader(ExtensionTreatmentType type) {
        this.nshCh = NshContextHeader.of(0);
        this.type = type;
    }

    /**
     * Creates a new set nsh context header instruction.
     *
     * @param nshCh nsh context header
     * @param type extension treatment type
     */
    public NiciraSetNshContextHeader(NshContextHeader nshCh, ExtensionTreatmentType type) {
        this.nshCh = nshCh;
        this.type = type;
    }

    /**
     * Gets the nsh context header.
     *
     * @return nsh context header
     */
    public NshContextHeader nshCh() {
        return nshCh;
    }

    @Override
    public ExtensionTreatmentType type() {
        return type;
    }

    @Override
    public void deserialize(byte[] data) {
        Map<String, Object> values = appKryo.deserialize(data);
        nshCh = (NshContextHeader) values.get("nshCh");
        type = (ExtensionTreatmentType) values.get("type");
    }

    @Override
    public byte[] serialize() {
        Map<String, Object> values = Maps.newHashMap();
        values.put("nshCh", nshCh);
        values.put("type", type);
        return appKryo.serialize(values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nshCh, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NiciraSetNshContextHeader) {
            NiciraSetNshContextHeader that = (NiciraSetNshContextHeader) obj;
            return Objects.equals(nshCh, that.nshCh) && Objects.equals(type, that.type);

        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("nshCh", nshCh)
                .add("type", type)
                .toString();
    }
}
