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

package org.onosproject.ui.topo;

/**
 * Partial implementation of the types of highlight to apply to topology
 * elements.
 */
public abstract class AbstractHighlight {
    private final TopoElementType type;
    private final String elementId;

    public AbstractHighlight(TopoElementType type, String elementId) {
        this.type = type;
        this.elementId = elementId;
    }

    public TopoElementType type() {
        return type;
    }

    public String elementId() {
        return elementId;
    }
}
