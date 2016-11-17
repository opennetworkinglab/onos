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
import org.onlab.packet.VlanId;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;

import java.util.Objects;

/**
 * OFDPA set OVID extension instruction. OVID is a meta-data of the OFDPA
 * pipeline, but basically it is a VLAN ID.
 */
public class Ofdpa3SetOvid extends OfdpaSetVlanVid {

    /**
     * Constructs a new set OVID instruction.
     */
    protected Ofdpa3SetOvid() {
        super();
    }

    /**
     * Constructs a new set OVID instruction with a given VLAN ID.
     *
     * @param vlanId VLAN ID
     */
    public Ofdpa3SetOvid(VlanId vlanId) {
        super(vlanId);
    }

    /**
     * Returns the treatment type.
     *
     * @return the set OVID extension type
     */
    @Override
    public ExtensionTreatmentType type() {
        return ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_SET_OVID.type();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.vlanId());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Ofdpa3SetOvid) {
            Ofdpa3SetOvid that = (Ofdpa3SetOvid) obj;
            return Objects.equals(this.vlanId(), that.vlanId());

        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("oVid", this.vlanId())
                .toString();
    }

}
