package org.projectfloodlight.openflow.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.projectfloodlight.openflow.exceptions.OFParseError;

public interface OFMessageWriter<T> {
    public void write(ChannelBuffer bb, T message) throws OFParseError;
}
