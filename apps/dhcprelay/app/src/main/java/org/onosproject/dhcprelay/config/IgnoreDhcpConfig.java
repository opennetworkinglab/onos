/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.dhcprelay.config;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;

import java.util.concurrent.atomic.AtomicBoolean;

public class IgnoreDhcpConfig extends Config<ApplicationId> {
    public static final String KEY = "ignoreDhcp";
    private static final String DEVICE_ID = "deviceId";
    private static final String VLAN_ID = "vlan";

    @Override
    public boolean isValid() {
        AtomicBoolean valid = new AtomicBoolean(true);
        if (array == null) {
            return false;
        }
        array.forEach(node -> {
            valid.compareAndSet(true, node.has(DEVICE_ID) && node.has(VLAN_ID));
        });
        return valid.get();
    }

    public Multimap<DeviceId, VlanId> ignoredVlans() {
        Multimap<DeviceId, VlanId> ignored = ArrayListMultimap.create();

        array.forEach(node -> {
            DeviceId deviceId = DeviceId.deviceId(node.get(DEVICE_ID).asText());
            VlanId vlanId = VlanId.vlanId((short) node.get(VLAN_ID).asInt());
            ignored.put(deviceId, vlanId);
        });

        return ignored;
    }
}
