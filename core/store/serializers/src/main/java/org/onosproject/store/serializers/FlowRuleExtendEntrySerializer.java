package org.onosproject.store.serializers;

import org.onosproject.net.flowextend.FlowRuleExtendEntry;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class FlowRuleExtendEntrySerializer extends Serializer<FlowRuleExtendEntry> {

	public FlowRuleExtendEntrySerializer() {
		super(false, true);
		// TODO Auto-generated constructor stub
	}

	@Override
	public FlowRuleExtendEntry read(Kryo kryo, Input input,
			Class<FlowRuleExtendEntry> object) {
		// TODO Auto-generated method stub
		int deviceid = input.readInt();
		int length = input.readInt();
		byte[] buf = input.readBytes(length);
		return new FlowRuleExtendEntry(deviceid, buf);
	}

	@Override
	public void write(Kryo kyro, Output output, FlowRuleExtendEntry object) {
		// TODO Auto-generated method stub
		output.writeInt(object.getDeviceId());
		output.writeInt(object.getLength());
		output.writeBytes(object.getSncflow());
	}
}