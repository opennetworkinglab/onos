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

import java.util.Objects;

/**
 * A test class which represents YANG data node identifier which is a
 * combination of name and namespace.
 */
public class TestYangSchemaId {
    private String name;
    private String namespace;

    /**
     * Returns the name of the node.
     *
     * @return name of the node
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets name of the node.
     *
     * @param name name of the node
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns namespace of the node.
     *
     * @return namespace of the node
     */
    public String getNameSpace() {
        return this.namespace;
    }

    /**
     * Sets namespace of the node.
     *
     * @param namespace namespace of the node
     */
    public void setNameSpace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof TestYangSchemaId)) {
            return false;
        } else {
            TestYangSchemaId other = (TestYangSchemaId) obj;
            return Objects.equals(this.name, other.name) &&
                    Objects.equals(this.namespace, other.namespace);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.namespace);
    }
}
