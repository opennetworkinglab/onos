package org.onosproject.ipran.serializers.impl;

import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.store.topology.impl.DefaultTopology;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class TopologySerializer extends Serializer<DefaultTopology> {

	/**
	 * Creates {@link DefaultTopology} serializer instance.
	 */
	public TopologySerializer() {
		super(false, true);
	}

	@Override
	public void write(Kryo kryo, Output output, DefaultTopology object) {
		kryo.writeClassAndObject(output, object.providerId());
		kryo.writeClassAndObject(output, object.deviceCount());
		kryo.writeClassAndObject(output, object.clusterCount());
		kryo.writeClassAndObject(output, object.time());
		kryo.writeClassAndObject(output, object.computeCost());
		TopologyService topologyService = DefaultServiceDirectory
				.getService(TopologyService.class);
		kryo.writeClassAndObject(output, topologyService.getGraph(object));

	}

	@Override
	public DefaultTopology read(Kryo kryo, Input input,
			Class<DefaultTopology> type) {
		//TODO
		return null;

	}

}
