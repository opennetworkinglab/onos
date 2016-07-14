/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.ui.topo;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Partial implementation of the highlighting to apply to topology
 * view elements.
 */
public abstract class AbstractHighlight {
    private final TopoElementType type;
    private final String elementId;
    private boolean keepSubdued = false;

    /**
     * Constructs the highlight.
     *
     * @param type highlight element type
     * @param elementId element identifier
     */
    public AbstractHighlight(TopoElementType type, String elementId) {
        this.type = checkNotNull(type);
        this.elementId = checkNotNull(elementId);
    }

    /**
     * Sets a flag to tell the renderer to keep this element subdued.
     */
    public void keepSubdued() {
        keepSubdued = true;
    }

    /**
     * Returns the element type.
     *
     * @return element type
     */
    public TopoElementType type() {
        return type;
    }

    /**
     * Returns the element identifier.
     *
     * @return element identifier
     */
    public String elementId() {
        return elementId;
    }

    /**
     * Returns the subdued flag.
     *
     * @return subdued flag
     */
    public boolean subdued() {
        return keepSubdued;
    }
}
