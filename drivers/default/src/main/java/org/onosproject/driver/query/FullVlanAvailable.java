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
package org.onosproject.driver.query;

import java.util.Set;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSet;
import org.onlab.packet.VlanId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.VlanQuery;
import org.onosproject.net.driver.AbstractHandlerBehaviour;

import com.google.common.annotations.Beta;

/**
 * Driver which always responds that all VLAN IDs are available for the Device.
 */
@Beta
public class FullVlanAvailable
    extends AbstractHandlerBehaviour
    implements VlanQuery {

    private static final int MAX_VLAN_ID = VlanId.MAX_VLAN;
    private static final Set<Integer> EXCLUDED = ImmutableSet.of(
            (int) VlanId.NO_VID,
            (int) VlanId.RESERVED);
    private static final Set<VlanId> ENTIRE_VLAN = getEntireVlans();

    @Override
    public Set<VlanId> queryVlanIds(PortNumber port) {
        return ENTIRE_VLAN;
    }

    private static Set<VlanId> getEntireVlans() {
        return IntStream.range(0, MAX_VLAN_ID)
                .filter(x -> !EXCLUDED.contains(x))
                .mapToObj(x -> VlanId.vlanId((short) x))
                .collect(ImmutableSet.toImmutableSet());
    }

}
