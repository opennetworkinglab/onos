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

import com.google.common.collect.Lists;
import gnmi.Gnmi;
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

    /**
     * Helper class which builds gNMI path.
     *
     * Example usage:
     * Path: /interfaces/interface[name=if1]/state/oper-status
     * Java code:
     * <code>
     * Gnmi.Path path = GnmiPathBuilder.newBuilder()
     *     .addElem("interfaces")
     *     .addElem("interface").withKeyValue("name", "if1")
     *     .addElem("state")
     *     .addElem("oper-status")
     *     .build();
     * </code>
     */
    public static final class GnmiPathBuilder {
        List<Gnmi.PathElem> elemList;
        private GnmiPathBuilder() {
            elemList = Lists.newArrayList();
        }

        public static GnmiPathBuilder newBuilder() {
            return new GnmiPathBuilder();
        }

        public GnmiPathBuilder addElem(String elemName) {
            Gnmi.PathElem elem =
                    Gnmi.PathElem.newBuilder()
                            .setName(elemName)
                            .build();
            elemList.add(elem);
            return this;
        }
        public GnmiPathBuilder withKeyValue(String key, String value) {
            if (elemList.isEmpty()) {
                // Invalid case. ignore it
                return this;
            }
            Gnmi.PathElem lastElem = elemList.remove(elemList.size() - 1);
            Gnmi.PathElem newElem =
                    Gnmi.PathElem.newBuilder(lastElem)
                            .putKey(key, value)
                            .build();
            elemList.add(newElem);
            return this;
        }

        public Gnmi.Path build() {
            return Gnmi.Path.newBuilder().addAllElem(elemList).build();
        }

    }
}
