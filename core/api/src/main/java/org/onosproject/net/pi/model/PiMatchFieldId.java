/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.net.pi.model;

import com.google.common.annotations.Beta;
import org.onlab.util.Identifier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Identifier of a match field in a protocol-independent pipeline, unique within the scope of a table model.
 */
@Beta
public final class PiMatchFieldId extends Identifier<String> {

    private PiMatchFieldId(String name) {
        super(name);
    }

    /**
     * Returns an identifier for the given match field name.
     *
     * @param name match field name
     * @return match field ID
     */
    public static PiMatchFieldId of(String name) {
        checkNotNull(name);
        checkArgument(!name.isEmpty(), "Name cannot be empty");
        return new PiMatchFieldId(name);
    }
}
