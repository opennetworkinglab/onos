package org.onosproject.store.serializers;

import org.onosproject.net.flow.SncFlowRuleEntry;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class SncFlowRuleEntrySerializer extends Serializer<SncFlowRuleEntry> {

	public SncFlowRuleEntrySerializer() {
		super(false, true);
		// TODO Auto-generated constructor stub
	}

	@Override
	public SncFlowRuleEntry read(Kryo kryo, Input input,
			Class<SncFlowRuleEntry> object) {
		// TODO Auto-generated method stub
		int deviceid = input.readInt();
		int length = input.readInt();
		byte[] buf = input.readBytes(length);
		return new SncFlowRuleEntry(deviceid, buf);
	}

	@Override
	public void write(Kryo kyro, Output output, SncFlowRuleEntry object) {
		// TODO Auto-generated method stub
		output.writeInt(object.getDeviceId());
		output.writeInt(object.getLength());
		output.writeBytes(object.getSncflow());
	}
}