/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.incubator.protobuf.models.net;

import org.onosproject.incubator.protobuf.models.net.link.LinkEnumsProtoTranslator;
import org.onosproject.grpc.net.models.LinkProtoOuterClass;
import org.onosproject.net.Annotations;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.Link;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.provider.ProviderId;

import java.util.HashMap;
import java.util.Map;

/**
 * gRPC LinkProto message to equivalent ONOS Link conversion related utilities.
 */
public final class LinkProtoTranslator {

    /**
     * Translates gRPC LinkCore message to {@link Link}.
     *
     * @param link gRPC message
     * @return {@link Link} null if link is a default instance
     */
    public static Link translate(LinkProtoOuterClass.LinkProto link) {
        if (link.equals(LinkProtoOuterClass.LinkProto.getDefaultInstance())) {
            return null;
        }
        ProviderId providerId = ProviderIdProtoTranslator.translate(link.getProviderId());
        Link.State state = LinkEnumsProtoTranslator.translate(link.getState()).get();
        ConnectPoint src = ConnectPointProtoTranslator.translate(link.getSrc()).get();
        ConnectPoint dst = ConnectPointProtoTranslator.translate(link.getDst()).get();
        Link.Type type = LinkEnumsProtoTranslator.translate(link.getType()).get();
        Annotations annots = asAnnotations(link.getAnnotations());
        Boolean isExpected = link.getIsExpected();
        return DefaultLink.builder().state(state)
                .annotations(annots)
                .providerId(providerId)
                .src(src)
                .dst(dst)
                .type(type)
                .isExpected(isExpected)
                .build();
    }

    /**
     * Translates {@link Link} to gRPC LinkCore message.
     *
     * @param link {@link Link}
     * @return gRPC LinkCore message
     */
    public static LinkProtoOuterClass.LinkProto translate(Link link) {
        if (link == null) {
            return LinkProtoOuterClass.LinkProto.getDefaultInstance();
        }
        return LinkProtoOuterClass.LinkProto.newBuilder()
                .setProviderId(ProviderIdProtoTranslator.translate(link.providerId()))
                .setState(LinkEnumsProtoTranslator.translate(link.state()))
                .setSrc(ConnectPointProtoTranslator.translate(link.src()))
                .setDst(ConnectPointProtoTranslator.translate(link.dst()))
                .setType(LinkEnumsProtoTranslator.translate(link.type()))
                .setIsExpected(link.isExpected())
                .build();
    }

    // may be this can be moved to Annotation itself or AnnotationsUtils

    /**
     * Converts Map of Strings to {@link SparseAnnotations}.
     *
     * @param annotations Map of annotation key and values
     * @return {@link SparseAnnotations}
     */
    public static SparseAnnotations asAnnotations(Map<String, String> annotations) {
        DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
        annotations.entrySet().forEach(e -> {
            if (e.getValue() != null) {
                builder.set(e.getKey(), e.getValue());
            } else {
                builder.remove(e.getKey());
            }
        });
        return builder.build();
    }

    /**
     * Converts Annotations to Map of Strings.
     *
     * @param annotations {@link Annotations}
     * @return Map of annotation key and values
     */
    public static Map<String, String> asMap(Annotations annotations) {
        if (annotations instanceof DefaultAnnotations) {
            return ((DefaultAnnotations) annotations).asMap();
        }
        Map<String, String> map = new HashMap<>();
        annotations.keys()
                .forEach(k -> map.put(k, annotations.value(k)));

        return map;
    }

    // Utility class not intended for instantiation.
    private LinkProtoTranslator() {
    }

}

