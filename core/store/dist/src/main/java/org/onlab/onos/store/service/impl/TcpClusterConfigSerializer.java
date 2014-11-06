package org.onlab.onos.store.service.impl;

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
