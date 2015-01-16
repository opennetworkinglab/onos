package org.onosproject.ipran.serializers.impl;

import java.util.Set;

import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyVertex;
import org.onosproject.store.topology.impl.DefaultTopologyGraph;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class DefaultTopologyGraphSerializer extends
		Serializer<DefaultTopologyGraph> {

	/**
	 * Creates {@link DefaultTopologyGraph} serializer instance.
	 */
	public DefaultTopologyGraphSerializer() {
		// non-null, immutable
		super(false, true);
	}

	@Override
	public void write(Kryo kryo, Output output, DefaultTopologyGraph object) {
		kryo.writeClassAndObject(output, object.getEdges());
		kryo.writeClassAndObject(output, object.getVertexes());

	}

	@Override
	public DefaultTopologyGraph read(Kryo kryo, Input input,
			Class<DefaultTopologyGraph> type) {
		Set<TopologyVertex> vertexes = (Set<TopologyVertex>) kryo
				.readClassAndObject(input);
		Set<TopologyEdge> edges = (Set<TopologyEdge>) kryo
				.readClassAndObject(input);
		return new DefaultTopologyGraph(vertexes, edges);
	}

}
