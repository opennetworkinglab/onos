/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.gnmi.api;

import gnmi.Gnmi.Path;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utilities for gNMI protocol.
 */
public final class GnmiUtils {

    private GnmiUtils() {
        // Hide default constructor
    }

    /**
     * Convert gNMI path to human readable string.
     *
     * @param path the gNMI path
     * @return readable string of the path
     */
    public static String pathToString(Path path) {
        StringBuilder pathStringBuilder = new StringBuilder();

        path.getElemList().forEach(elem -> {
            pathStringBuilder.append("/").append(elem.getName());
            if (elem.getKeyCount() > 0) {
                pathStringBuilder.append("[");
                List<String> keys = elem.getKeyMap().entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .collect(Collectors.toList());
                pathStringBuilder.append(String.join(", ", keys));
                pathStringBuilder.append("]");
            }
        });
        return pathStringBuilder.toString();
    }
}
