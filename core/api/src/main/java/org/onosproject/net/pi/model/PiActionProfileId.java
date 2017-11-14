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

/**
 * Identifier of an action profile in a protocol-independent pipeline, unique withing the scope of a pipeline model.
 */
@Beta
public final class PiActionProfileId extends Identifier<String> {

    private PiActionProfileId(String actionProfileName) {
        super(actionProfileName);
    }

    /**
     * Returns an identifier for the given action profile name.
     *
     * @param name action profile name
     * @return action profile ID
     */
    public static PiActionProfileId of(String name) {
        return new PiActionProfileId(name);
    }
}
