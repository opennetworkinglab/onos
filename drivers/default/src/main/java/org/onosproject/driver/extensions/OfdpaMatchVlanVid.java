/*
 * Copyright 2015-present Open Networking Laboratory
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
import org.onlab.util.KryoNamespace;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.ExtensionSelectorType;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * OFDPA VLAN ID extension match.
 */
public class OfdpaMatchVlanVid extends AbstractExtension implements ExtensionSelector {
    private VlanId vlanId;

    private static final KryoNamespace APPKRYO = new KryoNamespace.Builder()
            .register(VlanId.class)
            .build();

    /**
     * OFDPA VLAN ID extension match.
     */
    protected OfdpaMatchVlanVid() {
        vlanId = null;
    }

    /**
     * Constructs a new VLAN ID match with given VLAN ID.
     *
     * @param vlanId VLAN ID
     */
    public OfdpaMatchVlanVid(VlanId vlanId) {
        checkNotNull(vlanId);
        this.vlanId = vlanId;
    }

    /**
     * Gets the VLAN ID.
     *
     * @return VLAN ID
     */
    public VlanId vlanId() {
        return vlanId;
    }

    @Override
    public ExtensionSelectorType type() {
        return ExtensionSelectorType.ExtensionSelectorTypes.OFDPA_MATCH_VLAN_VID.type();
    }

    @Override
    public void deserialize(byte[] data) {
        vlanId = APPKRYO.deserialize(data);
    }

    @Override
    public byte[] serialize() {
        return APPKRYO.serialize(vlanId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vlanId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && obj.getClass() == OfdpaMatchVlanVid.class) {
            OfdpaMatchVlanVid that = (OfdpaMatchVlanVid) obj;
            return Objects.equals(vlanId, that.vlanId);

        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("vlanId", vlanId)
                .toString();
    }
}
