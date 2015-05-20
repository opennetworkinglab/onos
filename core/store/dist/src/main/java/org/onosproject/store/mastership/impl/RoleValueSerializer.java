/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.store.mastership.impl;

import java.util.List;
import java.util.Map;

import org.onosproject.cluster.NodeId;
import org.onosproject.net.MastershipRole;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Serializer for RoleValues used by {@link org.onosproject.mastership.MastershipStore}.
 */
public class RoleValueSerializer extends Serializer<RoleValue> {

    //RoleValues are assumed to hold a Map of MastershipRoles (an enum)
    //to a List of NodeIds.

    @Override
    public RoleValue read(Kryo kryo, Input input, Class<RoleValue> type) {
        RoleValue rv = new RoleValue();
        int size = input.readInt();
        for (int i = 0; i < size; i++) {
            MastershipRole role = MastershipRole.values()[input.readInt()];
            int s = input.readInt();
            for (int j = 0; j < s; j++) {
                rv.add(role, new NodeId(input.readString()));
            }
        }
        return rv;
    }

    @Override
    public void write(Kryo kryo, Output output, RoleValue type) {
        final Map<MastershipRole, List<NodeId>> map = type.value();
        output.writeInt(map.size());

        for (Map.Entry<MastershipRole, List<NodeId>> el : map.entrySet()) {
            output.writeInt(el.getKey().ordinal());

            List<NodeId> nodes = el.getValue();
            output.writeInt(nodes.size());
            for (NodeId n : nodes) {
                output.writeString(n.toString());
            }
        }
    }

}
