package org.onosproject.store.service.impl;

import net.kuujo.copycat.cluster.TcpMember;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class TcpMemberSerializer extends Serializer<TcpMember> {

    @Override
    public void write(Kryo kryo, Output output, TcpMember object) {
        output.writeString(object.host());
        output.writeInt(object.port());
    }

    @Override
    public TcpMember read(Kryo kryo, Input input, Class<TcpMember> type) {
        String host = input.readString();
        int port = input.readInt();
        return new TcpMember(host, port);
    }
}
