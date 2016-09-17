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
import org.onlab.util.KryoNamespace;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * OFDPA set MPLS Type extension instruction.
 */
public class Ofdpa3SetMplsType extends AbstractExtension implements ExtensionTreatment {
    private short mplsType;

    private static final KryoNamespace APPKRYO = new KryoNamespace.Builder()
            .register(Ofdpa3MplsType.class)
            .build();

    /**
     * Constructs a new set MPLS type instruction.
     */
    protected Ofdpa3SetMplsType() {
        mplsType = Ofdpa3MplsType.NONE.getValue();
    }

    /**
     * Constructs a new set MPLS type instruction with given type.
     *
     * @param mplsType MPLS type in short
     */
    public Ofdpa3SetMplsType(short mplsType) {
        checkNotNull(mplsType);
        this.mplsType = mplsType;
    }

    /**
     * Constructs a new set MPLS type instruction with given type.
     *
     * @param mplsType Ofdpa3MplsType
     */
    public Ofdpa3SetMplsType(Ofdpa3MplsType mplsType) {
        checkNotNull(mplsType);
        this.mplsType = mplsType.getValue();
    }

    /**
     * Gets the MPLS type.
     *
     * @return MPLS type
     */
    public short mplsType() {
        return mplsType;
    }

    @Override
    public ExtensionTreatmentType type() {
        return ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_SET_MPLS_TYPE.type();
    }

    @Override
    public void deserialize(byte[] data) {
        mplsType = APPKRYO.deserialize(data);
    }

    @Override
    public byte[] serialize() {
        return APPKRYO.serialize(mplsType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mplsType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Ofdpa3SetMplsType) {
            Ofdpa3SetMplsType that = (Ofdpa3SetMplsType) obj;
            return Objects.equals(mplsType, that.mplsType);

        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("mplsType", mplsType)
                .toString();
    }
}
