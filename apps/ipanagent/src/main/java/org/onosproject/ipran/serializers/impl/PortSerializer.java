package org.onosproject.ipran.serializers.impl;

import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Element;
import org.onosproject.net.Port;
import org.onosproject.net.Port.Type;
import org.onosproject.net.PortNumber;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class PortSerializer extends Serializer<Port> {

	/**
	 * Creates {@link Port} serializer instance.
	 */
	public PortSerializer() {
		// non-null, immutable
		super(false, true);
	}

	@Override
	public void write(Kryo kryo, Output output, Port object) {
		kryo.writeClassAndObject(output, object.element());
		kryo.writeClassAndObject(output, object.number());
		kryo.writeClassAndObject(output, object.isEnabled());
		kryo.writeClassAndObject(output, object.type());
		kryo.writeClassAndObject(output, object.portSpeed());
		kryo.writeClassAndObject(output, object.annotations());

	}

	@Override
	public Port read(Kryo kryo, Input input, Class<Port> type) {
		Element element = (Element) kryo.readClassAndObject(input);
		PortNumber number = (PortNumber) kryo.readClassAndObject(input);
		boolean isEnabled = (boolean) kryo.readClassAndObject(input);
		Type portType = (Type) kryo.readClassAndObject(input);
		long portSpeed = (long) kryo.readClassAndObject(input);
		Annotations annotations = (Annotations) kryo.readClassAndObject(input);
		return new DefaultPort(element, number, isEnabled, portType, portSpeed,
				annotations);
	}

}
