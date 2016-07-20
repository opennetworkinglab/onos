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

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;

import java.util.Map;
import java.util.Objects;

/**
 * Default implementation of Move treatment.
 */
public class DefaultMoveExtensionTreatment extends AbstractExtension
        implements MoveExtensionTreatment {

    private int srcOfs;
    private int dstOfs;
    private int nBits;
    private int src;
    private int dst;
    private ExtensionTreatmentType type;

    private final KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(Map.class)
            .build("DefaultMoveExtensionTreatment");

    /**
     * Creates a new move Treatment.
     *
     * @param srcOfs source offset
     * @param dstOfs destination offset
     * @param nBits nbits
     * @param src source
     * @param dst destination
     * @param type extension treatment type
     */
    public DefaultMoveExtensionTreatment(int srcOfs, int dstOfs, int nBits,
                                         int src, int dst, ExtensionTreatmentType type) {
        this.srcOfs = srcOfs;
        this.dstOfs = dstOfs;
        this.nBits = nBits;
        this.src = src;
        this.dst = dst;
        this.type = type;
    }

    @Override
    public ExtensionTreatmentType type() {
        return type;
    }

    @Override
    public byte[] serialize() {
        Map<String, Integer> values = Maps.newHashMap();
        values.put("srcOfs", srcOfs);
        values.put("dstOfs", dstOfs);
        values.put("nBits", nBits);
        values.put("src", src);
        values.put("dst", dst);
        values.put("type", ExtensionTreatmentType.ExtensionTreatmentTypes.valueOf(type.toString()).ordinal());
        return appKryo.serialize(values);
    }

    @Override
    public void deserialize(byte[] data) {
        Map<String, Integer> values = appKryo.deserialize(data);
        srcOfs = values.get("srcOfs");
        dstOfs = values.get("dstOfs");
        nBits = values.get("nBits");
        src = values.get("src");
        dst = values.get("dst");
        type = new ExtensionTreatmentType(values.get("type").intValue());
    }

    @Override
    public int srcOffset() {
        return srcOfs;
    }

    @Override
    public int dstOffset() {
        return dstOfs;
    }

    @Override
    public int src() {
        return src;
    }

    @Override
    public int dst() {
        return dst;
    }

    @Override
    public int nBits() {
        return nBits;
    }

    @Override
    public int hashCode() {
        return Objects.hash(srcOfs, dstOfs, src, dst, nBits);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultMoveExtensionTreatment) {
            DefaultMoveExtensionTreatment that = (DefaultMoveExtensionTreatment) obj;
            return Objects.equals(srcOfs, that.srcOfs)
                    && Objects.equals(dstOfs, that.dstOfs)
                    && Objects.equals(src, that.src)
                    && Objects.equals(dst, that.dst)
                    && Objects.equals(nBits, that.nBits);

        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).add("srcOfs", srcOfs)
                .add("dstOfs", dstOfs).add("nBits", nBits).add("src", src)
                .add("dst", dst).toString();
    }
}
