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

package org.onosproject.protocol.restconf.server.utils.parser.api;

/**
 * Abstraction of an entity which represents a simple YANG node. This entity
 * is usually described by a path segment in URI, or a field name in a JSON
 * node.
 */
public class NormalizedYangNode {
    private final String namespace;
    private final String name;

    /**
     * Creates an instance of normalized YANG node using the supplied information.
     *
     * @param namespace namespace of a YANG node
     * @param name      name of a YANG node
     */
    public NormalizedYangNode(String namespace, String name) {
        this.namespace = namespace;
        this.name = name;
    }

    /**
     * Returns the namespace info of a YANG node.
     *
     * @return namespace info
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Returns the name of a YANG node.
     *
     * @return name
     */
    public String getName() {
        return name;
    }
}
