package org.projectfloodlight.openflow.protocol;

import org.jboss.netty.buffer.ChannelBuffer;

public interface OFObjectFactory<T extends OFObject> {
    T read(ChannelBuffer buffer);
}
