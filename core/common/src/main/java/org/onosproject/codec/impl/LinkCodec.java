/*
 * Copyright 2015-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.Annotations;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.Link;
import org.onosproject.net.Link.Type;
import org.onosproject.net.provider.ProviderId;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Link JSON codec.
 */
public final class LinkCodec extends AnnotatedCodec<Link> {

    // JSON field names
    private static final String SRC = "src";
    private static final String DST = "dst";
    private static final String TYPE = "type";
    private static final String STATE = "state";

    @Override
    public ObjectNode encode(Link link, CodecContext context) {
        checkNotNull(link, "Link cannot be null");
        JsonCodec<ConnectPoint> codec = context.codec(ConnectPoint.class);
        ObjectNode result = context.mapper().createObjectNode();
        result.set(SRC, codec.encode(link.src(), context));
        result.set(DST, codec.encode(link.dst(), context));
        result.put(TYPE, link.type().toString());
        if (link.state() != null) {
            result.put(STATE, link.state().toString());
        }
        return annotate(result, link, context);
    }


    /**
     * {@inheritDoc}
     *
     * Note: ProviderId is not part of JSON representation.
     *       Returned object will have random ProviderId set.
     */
    @Override
    public Link decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        JsonCodec<ConnectPoint> codec = context.codec(ConnectPoint.class);
        // TODO: add providerId to JSON if we need to recover them.
        ProviderId pid = new ProviderId("json", "LinkCodec");

        ConnectPoint src = codec.decode(get(json, SRC), context);
        ConnectPoint dst = codec.decode(get(json, DST), context);
        Type type = Type.valueOf(json.get(TYPE).asText());
        Annotations annotations = extractAnnotations(json, context);

        return DefaultLink
                .builder()
                .providerId(pid)
                .src(src)
                .dst(dst)
                .type(type)
                .annotations(annotations)
                .build();
    }
}
