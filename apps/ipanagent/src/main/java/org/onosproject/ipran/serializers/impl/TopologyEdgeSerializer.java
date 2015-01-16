package org.onosproject.ipran.serializers.impl;

import org.onosproject.net.Link;
import org.onosproject.net.topology.DefaultTopologyEdge;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyVertex;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class TopologyEdgeSerializer extends Serializer<TopologyEdge> {

	public TopologyEdgeSerializer() {
		// non-null, immutable
		super(false, true);
	}

	@Override
	public void write(Kryo kryo, Output output, TopologyEdge object) {
		kryo.writeClassAndObject(output, object.src());
		kryo.writeClassAndObject(output, object.dst());
		kryo.writeClassAndObject(output, object.link());

	}

	@Override
	public TopologyEdge read(Kryo kryo, Input input, Class<TopologyEdge> type) {
		TopologyVertex src = (TopologyVertex) kryo.readClassAndObject(input);
		TopologyVertex dst = (TopologyVertex) kryo.readClassAndObject(input);
		Link link = (Link) kryo.readClassAndObject(input);
		return new DefaultTopologyEdge(src, dst, link);
	}

}
