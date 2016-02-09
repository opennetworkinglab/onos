package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.mcast.McastRoute;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Codec to encode and decode a multicast route to and from JSON.
 */
public class McastRouteCodec extends JsonCodec<McastRoute> {

    private static final String SOURCE = "source";
    private static final String GROUP = "group";
    private static final String TYPE = "type";

    @Override
    public ObjectNode encode(McastRoute route, CodecContext context) {
        checkNotNull(route);
        ObjectNode root = context.mapper().createObjectNode()
                .put(TYPE, route.type().toString())
                .put(SOURCE, route.source().toString())
                .put(GROUP, route.group().toString());

        return root;
    }

    @Override
    public McastRoute decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        IpAddress source = IpAddress.valueOf(json.path(SOURCE).asText());
        IpAddress group = IpAddress.valueOf(json.path(GROUP).asText());

        McastRoute route = new McastRoute(source, group, McastRoute.Type.STATIC);

        return route;
    }
}
