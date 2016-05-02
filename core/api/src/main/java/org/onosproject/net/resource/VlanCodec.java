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
package org.onosproject.net.resource;

import com.google.common.annotations.Beta;
import org.onlab.packet.VlanId;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Codec for Vlan.
 */
@Beta
public final class VlanCodec implements DiscreteResourceCodec {
    @Override
    public int encode(DiscreteResource resource) {
        Optional<VlanId> vlan = resource.valueAs(VlanId.class);
        checkArgument(vlan.isPresent());
        return vlan.map(x -> (int) x.toShort()).get();
    }

    @Override
    public DiscreteResource decode(DiscreteResourceId parent, int value) {
        return Resources.discrete(parent, VlanId.vlanId((short) value)).resource();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return VlanCodec.class.hashCode();
    }
}
