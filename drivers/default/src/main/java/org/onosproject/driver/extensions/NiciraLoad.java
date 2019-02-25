/*
 * Copyright 2019-present Open Networking Foundation
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

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Nicira load extension instruction.
 */
public class NiciraLoad extends AbstractExtension implements ExtensionTreatment {
    private int ofsNbits;
    private long dst;
    private long value;

    private final KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(Map.class)
            .register(HashMap.class)
            .build();

    /**
     * Empty constructor.
     */
    public NiciraLoad() {
    }

    /**
     * Creates a new load treatment.
     *
     * @param ofsNbits off set and nBits
     * @param dst       destination
     * @param value     value
     */
    public NiciraLoad(int ofsNbits, long dst, long value) {
        this.ofsNbits = ofsNbits;
        this.dst = dst;
        this.value = value;
    }

    /**
     * Gets load nBits.
     *
     * @return nBits
     */
    public int ofsNbits() {
        return ofsNbits;
    }

    /**
     * Gets load destination.
     *
     * @return load destination
     */
    public long dst() {
        return dst;
    }

    /**
     * Gets load value.
     *
     * @return load value
     */
    public long value() {
        return value;
    }

    @Override
    public ExtensionTreatmentType type() {
        return ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_LOAD.type();
    }

    @Override
    public byte[] serialize() {
        Map<String, Object> values = Maps.newHashMap();
        values.put("ofsNbits", ofsNbits);
        values.put("dst", dst);
        values.put("value", value);
        return appKryo.serialize(values);
    }

    @Override
    public void deserialize(byte[] data) {
        Map<String, Object> values = appKryo.deserialize(data);
        ofsNbits = (int) values.get("ofsNbits");
        dst = (long) values.get("dst");
        value = (long) values.get("value");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NiciraLoad that = (NiciraLoad) o;
        return ofsNbits == that.ofsNbits &&
                dst == that.dst &&
                value == that.value &&
                Objects.equals(this.type(), that.type());
    }

    @Override
    public int hashCode() {
        return Objects.hash(ofsNbits, dst, value);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("ofsNbits", ofsNbits)
                .add("dst", dst)
                .add("value", value)
                .toString();
    }
}