package org.projectfloodlight.openflow.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.projectfloodlight.openflow.exceptions.OFParseError;

public interface OFMessageReader<T> {
    T readFrom(ChannelBuffer bb) throws OFParseError;
}
