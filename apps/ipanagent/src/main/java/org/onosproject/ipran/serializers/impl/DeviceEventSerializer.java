package org.onosproject.ipran.serializers.impl;

import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceEvent;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class DeviceEventSerializer extends Serializer<DeviceEvent> {

	/**
	 * Creates {@link DeviceEvent} serializer instance.
	 */
	public DeviceEventSerializer() {
		// non-null, immutable
		super(false, true);
	}

	@Override
	public void write(Kryo kryo, Output output, DeviceEvent object) {
		kryo.writeClassAndObject(output, object.type());
		kryo.writeClassAndObject(output, object.subject());
		kryo.writeClassAndObject(output, object.port());
		kryo.writeClassAndObject(output, object.time());

	}

	@Override
	public DeviceEvent read(Kryo kryo, Input input, Class<DeviceEvent> type) {
		org.onosproject.net.device.DeviceEvent.Type deviceEventType = (org.onosproject.net.device.DeviceEvent.Type) kryo
				.readClassAndObject(input);
		Device device = (Device) kryo.readClassAndObject(input);
		Port port = (Port) kryo.readClassAndObject(input);
		long time = (long) kryo.readClassAndObject(input);
		return new DeviceEvent(deviceEventType, device, port, time);
	}

}
