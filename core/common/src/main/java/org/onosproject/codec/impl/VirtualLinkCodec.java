package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.virtual.DefaultVirtualLink;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualLink;
import org.onosproject.net.Link;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Codec for the VirtualLink class.
 */
public class VirtualLinkCodec extends JsonCodec<VirtualLink> {

    // JSON field names
    private static final String NETWORK_ID = "networkId";
    private static final String TUNNEL_ID = "tunnelId";

    private static final String NULL_OBJECT_MSG = "VirtualLink cannot be null";
    private static final String MISSING_MEMBER_MSG = " member is required in VirtualLink";

    @Override
    public ObjectNode encode(VirtualLink vLink, CodecContext context) {
        checkNotNull(vLink, NULL_OBJECT_MSG);

        JsonCodec<Link> codec = context.codec(Link.class);
        ObjectNode result = codec.encode(vLink, context);
        result.put(NETWORK_ID, vLink.networkId().toString());
        // TODO check if tunnelId needs to be part of VirtualLink interface.
        if (vLink instanceof DefaultVirtualLink) {
            result.put(TUNNEL_ID, ((DefaultVirtualLink) vLink).tunnelId().toString());
        }
        return result;
    }

    @Override
    public VirtualLink decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }
        JsonCodec<Link> codec = context.codec(Link.class);
        Link link = codec.decode(json, context);
        NetworkId nId = NetworkId.networkId(Long.parseLong(extractMember(NETWORK_ID, json)));
        String tunnelIdStr = json.path(TUNNEL_ID).asText();
        TunnelId tunnelId = tunnelIdStr != null ? TunnelId.valueOf(tunnelIdStr)
                : TunnelId.valueOf(0);
        return new DefaultVirtualLink(nId, link.src(), link.dst(), tunnelId);
    }

    /**
     * Extract member from JSON ObjectNode.
     *
     * @param key key for which value is needed
     * @param json JSON ObjectNode
     * @return member value
     */
    private String extractMember(String key, ObjectNode json) {
        return nullIsIllegal(json.get(key), key + MISSING_MEMBER_MSG).asText();
    }
}
