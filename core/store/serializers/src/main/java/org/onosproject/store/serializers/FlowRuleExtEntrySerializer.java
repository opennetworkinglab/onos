package org.onosproject.store.serializers;

import org.onosproject.net.DeviceId;
import org.onosproject.net.flowext.FlowRuleExtEntry;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
* FlowRuleExtEntry Serializer for {@link FlowRuleExtEntry}.
*/
public class FlowRuleExtEntrySerializer extends Serializer<FlowRuleExtEntry> {
    
        /**
         * Creates {@link FlowRuleExtEntry} serializer instance.
         */
        public FlowRuleExtEntrySerializer() {
                super(false, true);
               // TODO Auto-generated constructor stub
        }

        @Override
        public FlowRuleExtEntry read(Kryo kryo, Input input,
                         Class<FlowRuleExtEntry> object) {
                // TODO Auto-generated method stub
                DeviceId deviceid = (DeviceId) kryo.readClassAndObject(input);
                Registration classT = kryo.readClass(input);
                int length = input.readInt();
                byte[] buf = input.readBytes(length);
                return new FlowRuleExtEntry(deviceid, classT.getType(), buf);
        }

        @Override
        public void write(Kryo kyro, Output output, FlowRuleExtEntry object) {
                // TODO Auto-generated method stub
                kyro.writeClassAndObject(output, object.getDeviceId());
                kyro.writeClass(output, object.getClassT());
                output.writeInt(object.getFlowEntryExt().length);
                output.writeBytes(object.getFlowEntryExt());
        }
}