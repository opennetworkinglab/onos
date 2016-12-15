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

package org.onosproject.patchpanel.impl;

import org.onosproject.net.ConnectPoint;

/**
 * Abstraction of a patch between two ports on a switch.
 */
public class Patch {

    private final PatchId patchId;
    private final ConnectPoint port1;
    private final ConnectPoint port2;

    /**
     * Creates a new patch.
     *
     * @param patchId patch ID
     * @param port1 first switch port
     * @param port2 second switch port
     */
    public Patch(PatchId patchId, ConnectPoint port1, ConnectPoint port2) {
        this.patchId = patchId;
        this.port1 = port1;
        this.port2 = port2;
    }

    /**
     * Gets the patch ID.
     *
     * @return patch ID
     */
    public PatchId id() {
        return patchId;
    }

    /**
     * Gets the first connect point.
     *
     * @return connect point
     */
    public ConnectPoint port1() {
        return port1;
    }

    /**
     * Gets the second connect point.
     *
     * @return connect point
     */
    public ConnectPoint port2() {
        return port2;
    }
}
