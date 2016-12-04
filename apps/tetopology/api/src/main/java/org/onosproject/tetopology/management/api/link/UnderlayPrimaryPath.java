/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.tetopology.management.api.link;

import java.util.List;

/**
 * Represents the underlay primary path that supports a TE link.
 */
public class UnderlayPrimaryPath extends UnderlayAbstractPath {
    // Underlay primary path currently has the same data structure defined in
    // the underlay abstract path. It may be extended per standard definitions.

    /**
     * Creates an instance of UnderlayPrimaryPath.
     *
     * @param pathElements the list of elements along the path
     * @param loose        loose if true, or otherwise strict
     */
    public UnderlayPrimaryPath(List<PathElement> pathElements, Boolean loose) {
        super(pathElements, loose);
    }
}
