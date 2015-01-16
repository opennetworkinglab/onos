package org.onosproject.ipran.serializers.impl;

import org.onosproject.net.DeviceId;
import org.onosproject.net.topology.DefaultTopologyVertex;
import org.onosproject.net.topology.TopologyVertex;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class TopologyVertexSerializer extends Serializer<TopologyVertex> {

	public TopologyVertexSerializer() {
		// non-null, immutable
		super(false, true);
	}

	@Override
	public void write(Kryo kryo, Output output, TopologyVertex object) {
		kryo.writeClassAndObject(output, object.deviceId());

	}

	@Override
	public TopologyVertex read(Kryo kryo, Input input,
			Class<TopologyVertex> type) {
		DeviceId deviceId = (DeviceId) kryo.readClassAndObject(input);
		return new DefaultTopologyVertex(deviceId);
	}

}
