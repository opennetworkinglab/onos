package org.projectfloodlight.openflow.protocol.ver12;

import org.jboss.netty.buffer.ChannelBuffer;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.OFMatchBmap;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.ver12.OFMatchV3Ver12;
import org.projectfloodlight.openflow.protocol.OFBsnVportQInQ;

/**
 * Collection of helper functions for reading and writing into ChannelBuffers
 *
 * @author capveg
 */

public class ChannelUtilsVer12 {
    public static Match readOFMatch(final ChannelBuffer bb) throws OFParseError {
        return OFMatchV3Ver12.READER.readFrom(bb);
    }

    // TODO these need to be figured out / removed

    public static OFBsnVportQInQ readOFBsnVportQInQ(ChannelBuffer bb) {
        throw new UnsupportedOperationException("not implemented");
    }

    public static void writeOFBsnVportQInQ(ChannelBuffer bb,
            OFBsnVportQInQ vport) {
        throw new UnsupportedOperationException("not implemented");

    }

    public static OFMatchBmap readOFMatchBmap(ChannelBuffer bb) {
        throw new UnsupportedOperationException("not implemented");
    }

    public static void writeOFMatchBmap(ChannelBuffer bb, OFMatchBmap match) {
        throw new UnsupportedOperationException("not implemented");
    }
}
