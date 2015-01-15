package org.onosproject.ipran.serializers.impl;

import org.onlab.packet.ChassisId;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class ChassisIdSerializer extends Serializer<ChassisId> {

	/**
	 * Creates {@link ChassisId} serializer instance.
	 */
	public ChassisIdSerializer() {
		// non-null, immutable
		super(false, true);
	}

	@Override
	public void write(Kryo kryo, Output output, ChassisId object) {
		kryo.writeClassAndObject(output, object.value());
		

	}

	@Override
	public ChassisId read(Kryo kryo, Input input, Class<ChassisId> type) {
		Long value = (Long) kryo.readClassAndObject(input);
		return new ChassisId(value);
	}

}
