/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.cluster;

import org.onlab.util.Identifier;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Controller cluster identity.
 */
public final class NodeId extends Identifier<String> implements Comparable<NodeId> {

    private static final int ID_MAX_LENGTH = 1024;

    /**
     * Constructor for serialization.
     */
    private NodeId() {
        super("");
    }

    /**
     * Creates a new cluster node identifier from the specified string.
     *
     * @param id string identifier
     */
    public NodeId(String id) {
        super(id);
        checkArgument(id.length() <= ID_MAX_LENGTH, "id exceeds maximum length " + ID_MAX_LENGTH);
    }

    /**
     * Creates a new cluster node identifier from the specified string.
     *
     * @param id string identifier
     * @return node id
     */
    public static NodeId nodeId(String id) {
        return new NodeId(id);
    }

    @Override
    public int compareTo(NodeId that) {
        return identifier.compareTo(that.identifier);
    }
}
