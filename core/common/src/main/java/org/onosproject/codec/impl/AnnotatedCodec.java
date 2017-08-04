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
import org.onosproject.net.Annotated;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;

/**
 * Base JSON codec for annotated entities.
 */
public abstract class AnnotatedCodec<T extends Annotated> extends JsonCodec<T> {

    /**
     * Adds JSON encoding of the given item annotations to the specified node.
     *
     * @param node    node to add annotations to
     * @param entity  annotated entity
     * @param context encode context
     * @return the given node
     */
    protected ObjectNode annotate(ObjectNode node, T entity, CodecContext context) {
        if (!entity.annotations().keys().isEmpty()) {
            JsonCodec<Annotations> codec = context.codec(Annotations.class);
            node.set("annotations", codec.encode(entity.annotations(), context));
        }
        return node;
    }

    /**
     * Extracts annotations of given Object.
     *
     * @param objNode annotated JSON object node
     * @param context decode context
     * @return extracted Annotations
     */
    protected Annotations extractAnnotations(ObjectNode objNode, CodecContext context) {

        JsonCodec<Annotations> codec = context.codec(Annotations.class);
        if (objNode.has("annotations") && objNode.isObject()) {
            return codec.decode(get(objNode, "annotations"), context);
        } else {
            return DefaultAnnotations.EMPTY;
        }
    }

}
