/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.config;

import java.util.Iterator;
import org.onosproject.yang.model.NodeKey;
import org.onosproject.yang.model.ResourceId;

/**
 * Representation of an entity which identifies a resource in the logical tree
 * data store. It is a list of node keys to identify the branch point
 * hierarchy to reach a resource in the instance tree.
 */

public final class ResourceIdParser {

    private ResourceIdParser() {

    }
    public static String asString(ResourceId path) {
        StringBuilder bldr = new StringBuilder();
            bldr.append("root.");
            Iterator<NodeKey> iter = path.nodeKeys().iterator();
            NodeKey key;
            while (iter.hasNext()) {
                key = iter.next();
                //log.info("Iter: key {}", key.toString());
                bldr.append(key.schemaId().name());
                bldr.append("#");
                bldr.append(key.schemaId().namespace());
                if (iter.hasNext()) {
                    bldr.append(".");
                }
            }
        return bldr.toString();
    }
}