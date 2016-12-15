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

import org.onlab.util.Identifier;

/**
 * Identifier of a patch.
 */
public final class PatchId extends Identifier<Integer> {

    private PatchId(int id) {
        super(id);
    }

    /**
     * Creates a new patch based of an integer.
     *
     * @param id backing value
     * @return patch ID
     */
    public static PatchId patchId(int id) {
        return new PatchId(id);
    }
}
