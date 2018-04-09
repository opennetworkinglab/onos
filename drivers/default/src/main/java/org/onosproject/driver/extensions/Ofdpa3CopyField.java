/*
 * Copyright 2018-present Open Networking Foundation
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
import org.onlab.util.KryoNamespace;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;

import java.util.Objects;

/**
 * OFDPA copy field extension instruction.
 */
public class Ofdpa3CopyField extends AbstractExtension implements ExtensionTreatment {
    // OXM ID of VLAN_VID field from OF-DPA spec.
    public static final int OXM_ID_VLAN_VID = 0x80000c02;
    // OXM ID of PACKET_REG(1) field from OF-DPA spec.
    public static final int OXM_ID_PACKET_REG_1 = 0x80010200;

    private int nBits;
    private int srcOffset;
    private int dstOffset;
    private int src;
    private int dst;

    private static final KryoNamespace APPKRYO = new KryoNamespace.Builder()
            .register(Ofdpa3AllowVlanTranslationType.class)
            .build();

    public Ofdpa3CopyField() {
        nBits = 0;
        srcOffset = 0;
        dstOffset = 0;
        src = OXM_ID_VLAN_VID;
        dst = OXM_ID_PACKET_REG_1;

    }

    public Ofdpa3CopyField(int nBits, int srcOffset, int dstOffset, int src, int dst) {
        this.nBits = nBits;
        this.srcOffset = srcOffset;
        this.dstOffset = dstOffset;
        this.src = src;
        this.dst = dst;
    }

    /**
     * Returns the nBits value.
     * @return nBits value.
     */
    public int getnBits() {
        return nBits;
    }

    /**
     * Returns the srcOffset value.
     * @return srcOffset value.
     */
    public int getSrcOffset() {
        return srcOffset;
    }

    /**
     * Returns the dstOffset value.
     * @return dstOffset value.
     */
    public int getDstOffset() {
        return dstOffset;
    }

    /**
     * Returns the src value.
     * @return src value.
     */
    public int getSrc() {
        return src;
    }

    /**
     * Returns the dst value.
     * @return dst value.
     */
    public int getDst() {
        return dst;
    }

    @Override
    public ExtensionTreatmentType type() {
        return ExtensionTreatmentType.ExtensionTreatmentTypes.ONF_COPY_FIELD.type();
    }

    @Override
    public void deserialize(byte[] data) {
    }

    @Override
    public byte[] serialize() {
        return APPKRYO.serialize(this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Ofdpa3CopyField) {
            Ofdpa3CopyField that = (Ofdpa3CopyField) obj;
            return Objects.equals(nBits, that.nBits) &&
                    Objects.equals(srcOffset, that.srcOffset) &&
                    Objects.equals(dstOffset, that.dstOffset) &&
                    Objects.equals(src, that.src) &&
                    Objects.equals(dst, that.dst);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("nBits", nBits)
                .add("srcOffset", Integer.toHexString(srcOffset))
                .add("dstOffset", Integer.toHexString(dstOffset))
                .add("src", Integer.toHexString(src))
                .add("dst", Integer.toHexString(dst))
                .toString();
    }
}
