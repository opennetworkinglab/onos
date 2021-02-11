/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onosproject.p4runtime.model;

import p4.config.v1.P4InfoOuterClass;

/**
 * Provides utility methods for P4Info annotations.
 */
public final class P4InfoAnnotationUtils {

    public static final String ONE_SHOT_ONLY_ANNOTATION = "oneshot";
    public static final String MAX_GROUP_SIZE_ANNOTATION = "max_group_size";

    private P4InfoAnnotationUtils() {
    }

    /**
     * Gets the annotation value if available in the given P4Info preamble.
     * Supports annotation in the form @my_annotation(value).
     *
     * @param name Annotation name
     * @param preamble preamble of the P4Info object
     * @return The annotation value if present, null otherwise
     */
    public static String getAnnotationValue(String name, P4InfoOuterClass.Preamble preamble) {
        return preamble.getAnnotationsList().stream()
                .filter(a -> a.startsWith("@" + name))
                // e.g. @my_annotation(value)
                .map(a -> a.substring(name.length() + 2, a.length() - 1))
                .findFirst()
                .orElse(null);
    }

    /**
     * Checks if the given annotation name is present in the given P4Info preamble.
     * Supports annotation in the form @my_annotation* (i.e., @oneshot, @max_group_size(10)).
     *
     * @param name Annotation name
     * @param preamble preamble of the P4Info object
     * @return True if the annotation is available, False otherwise.
     */
    public static boolean isAnnotationPresent(String name, P4InfoOuterClass.Preamble preamble) {
        return preamble.getAnnotationsList().stream()
                .anyMatch(a -> a.startsWith("@" + name));
    }
}
