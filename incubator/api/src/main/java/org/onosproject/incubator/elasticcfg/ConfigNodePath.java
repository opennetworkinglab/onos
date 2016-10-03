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
package org.onosproject.incubator.elasticcfg;

import org.onlab.util.Identifier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstraction of the key to an instance in the elastic config store.
 * Path to each node would contain info about its hierarchy too.
 */
public final class ConfigNodePath extends Identifier<String> {

    private final String parentKey;
    private final String nodeKey;

    /**
     * Creates a new ConfigNodePath from parentKey and nodeKey.
     *
     * @param parentKey absolute path to the parent.
     * @param nodeKey relative path to the node.
     */
    public ConfigNodePath(String parentKey, String nodeKey) {
        super(checkNotNull(parentKey, "parent key is null").concat(checkNotNull(nodeKey, "node key is null")));
        this.parentKey = parentKey;
        this.nodeKey = nodeKey;
    }

    /**
     * Returns the parent key.
     *
     * @return absolute path to the parent
     */
    public String parentKey() {
        return parentKey;
    }

    /**
     * Returns the node key.
     *
     * @return relative path to the node
     */
    public String nodeKey() {
        return nodeKey;
    }
}