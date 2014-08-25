package org.projectfloodlight.openflow.util;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.protocol.OFBsnPktinFlag;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.ver13.OFBsnPktinFlagSerializerVer13;
import org.projectfloodlight.openflow.types.OFMetadata;


public class MultiplePktInReasonUtil {
    private MultiplePktInReasonUtil() {}

    /**
     * This function is used in BVS T5/6 to decode the multiple packet in
     * reasons in Match.MetaData field.
     * */
    public static Set<OFBsnPktinFlag> getOFBsnPktinFlags(OFPacketIn pktIn) {
        if(pktIn.getVersion() != OFVersion.OF_13) {
            throw new IllegalArgumentException("multiple pkt in reasons are "
                                               + "only supported by BVS using "
                                               + "openflow 1.3");
        }

        Match match = pktIn.getMatch();
        if(match == null) {
            return ImmutableSet.<OFBsnPktinFlag>of();
        }
        OFMetadata metaData = match.get(MatchField.METADATA);
        if(metaData == null) {
            return ImmutableSet.<OFBsnPktinFlag>of();
        }
        U64 metaDataValue = metaData.getValue();
        if(metaDataValue == null) {
            return ImmutableSet.<OFBsnPktinFlag>of();
        }
        return OFBsnPktinFlagSerializerVer13.ofWireValue(metaDataValue
                                                               .getValue());
    }
}
