package org.onosproject.ipran.serializers;

import static org.slf4j.LoggerFactory.getLogger;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMessageReader;
import org.slf4j.Logger;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class HuaweiFlowSerializer extends Serializer<OFMessage>{
    private final Logger log = getLogger(getClass());
    @Override
    public void write(Kryo kryo, Output output, OFMessage object) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public OFMessage read(Kryo kryo, Input input,
                                 Class<OFMessage> type) {
        // TODO Auto-generated method stub
        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(input.getBuffer());
        OFMessageReader<OFMessage> reader = OFFactories.getGenericReader();
        try {
            OFMessage message = reader.readFrom(buffer);
        } catch (OFParseError e) {
            // TODO Auto-generated catch block
            log.error(e.getMessage());
        }
        return null;
    }

}
