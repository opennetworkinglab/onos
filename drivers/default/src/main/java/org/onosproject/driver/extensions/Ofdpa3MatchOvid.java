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
import org.onosproject.net.flow.criteria.ExtensionSelectorType;

import java.util.Objects;

/**
 * OFDPA OVID extension match. OVID is a meta-data of the OFDPA
 * pipeline, but basically it is a VLAN ID.
 */
public class Ofdpa3MatchOvid extends OfdpaMatchVlanVid {

    /**
     * Constructs a new match OVID instruction.
     */
    protected Ofdpa3MatchOvid() {
        super();
    }

    /**
     * Constructs a new match OVID instruction with a given VLAN ID.
     *
     * @param vlanId VLAN ID
     */
    public Ofdpa3MatchOvid(VlanId vlanId) {
        super(vlanId);
    }

    /**
     * Returns the treatment type.
     *
     * @return the match OVID extension type
     */
    @Override
    public ExtensionSelectorType type() {
        return ExtensionSelectorType.ExtensionSelectorTypes.OFDPA_MATCH_OVID.type();
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
        if (obj != null && obj.getClass() == Ofdpa3MatchOvid.class) {
            Ofdpa3MatchOvid that = (Ofdpa3MatchOvid) obj;
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
