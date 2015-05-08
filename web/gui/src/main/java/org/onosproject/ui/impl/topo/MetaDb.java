/*
 * Copyright 2015 Open Networking Laboratory
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
 *
 */

package org.onosproject.ui.impl.topo;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A database of meta information stored for topology objects.
 */
// package private
class MetaDb {

    private static Map<String, ObjectNode> metaCache = new ConcurrentHashMap<>();

    /**
     * Adds meta UI information about the specified object to the given payload.
     *
     * @param id object identifier
     * @param payload payload to which the info should be added
     */
    public void addMetaUi(String id, ObjectNode payload) {
        ObjectNode meta = metaCache.get(id);
        if (meta != null) {
            payload.set("metaUi", meta);
        }
    }
}
