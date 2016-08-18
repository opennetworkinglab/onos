/*
 * Copyright 2016-present Open Networking Laboratory
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

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.Link;
import org.onosproject.net.DisjointPath;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * DisjointPath JSON codec.
 */
public final class DisjointPathCodec extends AnnotatedCodec<DisjointPath> {
    @Override
    public ObjectNode encode(DisjointPath disjointPath, CodecContext context) {
        checkNotNull(disjointPath, "Path cannot be null");
        JsonCodec<Link> codec = context.codec(Link.class);
        ObjectNode result = context.mapper()
                .createObjectNode();

        ObjectNode primary =  context.mapper()
                .createObjectNode()
                .put("cost", disjointPath.primary().cost());

        result.set("primary", primary);
        ArrayNode jsonLinks = primary.putArray("links");
        for (Link link : disjointPath.primary().links()) {
               jsonLinks.add(codec.encode(link, context));
        }
        if (disjointPath.backup() != null) {
            ObjectNode backup = context.mapper()
                .createObjectNode()
                .put("cost", disjointPath.backup().cost());
            result.set("backup", backup);
            ArrayNode jsonLinks1 = backup.putArray("links");
            for (Link link1 : disjointPath.backup().links()) {
                       jsonLinks1.add(codec.encode(link1, context));
        }
        }
        return annotate(result, disjointPath, context);
    }

}

