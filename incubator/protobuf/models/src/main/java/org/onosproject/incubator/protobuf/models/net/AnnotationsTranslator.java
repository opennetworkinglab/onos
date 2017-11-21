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

import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.SparseAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * gRPC message conversion related utilities for annotations service.
 */
public final class AnnotationsTranslator {

    private static final Logger log = LoggerFactory.getLogger(AnnotationsTranslator.class);

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

    // Utility class not intended for instantiation.
    private AnnotationsTranslator() {}
}
