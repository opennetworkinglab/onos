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

package org.onosproject.cord.gui.model;


import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Designates a specific instance of an XOS function.
 */
public interface XosFunction extends JsonBlob {

    /**
     * Returns the descriptor for this function.
     *
     * @return function identifier
     */
    XosFunctionDescriptor descriptor();

    /**
     * Returns the current state of this function, encapsulated
     * as a JSON node.
     *
     * @return parameters for the function
     */
    ObjectNode params();
}

