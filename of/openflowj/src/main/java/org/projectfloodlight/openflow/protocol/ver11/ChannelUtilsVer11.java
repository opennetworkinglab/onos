package org.projectfloodlight.openflow.protocol.ver11;

import org.jboss.netty.buffer.ChannelBuffer;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.OFMatchBmap;
import org.projectfloodlight.openflow.protocol.match.Match;

/**
 * Collection of helper functions for reading and writing into ChannelBuffers
 *
 * @author capveg
 */

public class ChannelUtilsVer11 {
    public static Match readOFMatch(final ChannelBuffer bb) throws OFParseError {
        return OFMatchV2Ver11.READER.readFrom(bb);
    }

    public static OFMatchBmap readOFMatchBmap(ChannelBuffer bb) {
        throw new UnsupportedOperationException("not implemented");
    }

    public static void writeOFMatchBmap(ChannelBuffer bb, OFMatchBmap match) {
        throw new UnsupportedOperationException("not implemented");
    }
}
