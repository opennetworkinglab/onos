package org.projectfloodlight.openflow.protocol;

import org.jboss.netty.buffer.ChannelBuffer;

public interface Writeable {
    void writeTo(ChannelBuffer bb);
}
