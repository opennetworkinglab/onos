package org.onosproject.ipran.serializers.impl;

import java.util.List;

import org.onosproject.event.Event;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyEvent.Type;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class TopologyEventSerializer extends Serializer<TopologyEvent> {

	/**
	 * Creates {@link TopologyEvent} serializer instance.
	 */
	public TopologyEventSerializer() {
		super(false, true);
	}

	@Override
	public void write(Kryo kryo, Output output, TopologyEvent object) {
		kryo.writeClassAndObject(output, object.type());
		kryo.writeClassAndObject(output, object.subject());
		kryo.writeClassAndObject(output, object.reasons());
	}

	@Override
	public TopologyEvent read(Kryo kryo, Input input, Class<TopologyEvent> type) {
		Type typeTopoChange = (Type) kryo.readClassAndObject(input);
		Topology topology = (Topology) kryo.readClassAndObject(input);
		List<Event> reasons = (List<Event>) kryo.readClassAndObject(input);
		return new TopologyEvent(typeTopoChange, topology, reasons);

	}

}
