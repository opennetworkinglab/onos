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

package org.onosproject.ui.model.topo;

/**
 * Designates the logical layer of the network that an element belongs to.
 */
public enum UiLayer {
    PACKET, OPTICAL;

    /**
     * Returns the default layer (for those elements that do not explicitly
     * define which layer they belong to).
     *
     * @return default layer
     */
    public static UiLayer defaultLayer() {
        return PACKET;
    }
}
