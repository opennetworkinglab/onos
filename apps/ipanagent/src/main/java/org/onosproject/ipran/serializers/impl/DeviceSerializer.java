package org.onosproject.ipran.serializers.impl;

import org.onlab.packet.ChassisId;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.Device.Type;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.ProviderId;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class DeviceSerializer extends Serializer<Device> {

	/**
	 * Creates {@link Device} serializer instance.
	 */
	public DeviceSerializer() {
		// non-null, immutable
		super(false, true);
	}

	@Override
	public void write(Kryo kryo, Output output, Device object) {
		kryo.writeClassAndObject(output, object.providerId());
		kryo.writeClassAndObject(output, object.id());
		kryo.writeClassAndObject(output, object.type());
		kryo.writeClassAndObject(output, object.manufacturer());
		kryo.writeClassAndObject(output, object.hwVersion());
		kryo.writeClassAndObject(output, object.swVersion());
		kryo.writeClassAndObject(output, object.serialNumber());
		kryo.writeClassAndObject(output, object.chassisId());
		kryo.writeClassAndObject(output, object.annotations());

	}

	@Override
	public Device read(Kryo kryo, Input input, Class<Device> type) {
		ProviderId providerId = (ProviderId) kryo.readClassAndObject(input);
		DeviceId deviceId = (DeviceId) kryo.readClassAndObject(input);
		Type deviceType = (Type) kryo.readClassAndObject(input);
		String manufacturer = (String) kryo.readClassAndObject(input);
		String hwVersion = (String) kryo.readClassAndObject(input);
		String swVersion = (String) kryo.readClassAndObject(input);
		String serialNumber = (String) kryo.readClassAndObject(input);
		ChassisId chassisId = (ChassisId) kryo.readClassAndObject(input);
		Annotations annotations = (Annotations) kryo.readClassAndObject(input);
		return new DefaultDevice(providerId, deviceId, deviceType,
				manufacturer, hwVersion, swVersion, serialNumber, chassisId,
				annotations);

	}

}
