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
package org.onosproject.store.service.impl;

import java.util.Collection;

import net.kuujo.copycat.cluster.TcpClusterConfig;
import net.kuujo.copycat.cluster.TcpMember;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class TcpClusterConfigSerializer extends Serializer<TcpClusterConfig> {

    @Override
    public void write(Kryo kryo, Output output, TcpClusterConfig object) {
        kryo.writeClassAndObject(output, object.getLocalMember());
        kryo.writeClassAndObject(output, object.getRemoteMembers());
    }

    @Override
    public TcpClusterConfig read(Kryo kryo, Input input,
                                 Class<TcpClusterConfig> type) {
        TcpMember localMember = (TcpMember) kryo.readClassAndObject(input);
        @SuppressWarnings("unchecked")
        Collection<TcpMember> remoteMembers = (Collection<TcpMember>) kryo.readClassAndObject(input);
        return new TcpClusterConfig(localMember, remoteMembers);
    }

}
