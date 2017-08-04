/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.store.service;

/**
 * Metadata information for a consistent map.
 */
public class MapInfo {
    private final String name;
    private final int size;

    public MapInfo(String name, int size) {
        this.name = name;
        this.size = size;
    }

    /**
     * Returns the name of the map.
     *
     * @return map name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the number of entries in the map.
     *
     * @return map size
     */
    public int size() {
        return size;
    }
}
