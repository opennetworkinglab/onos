/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.net.pi.runtime;

import com.google.common.annotations.Beta;
import org.onlab.util.Identifier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Identifier of a metadata for a packet I/O operation in a protocol-independent pipeline.
 */
@Beta
public final class PiPacketMetadataId extends Identifier<String> {

    /**
     * Creates a packet metadata identifier.
     *
     * @param name packet metadata name
     */
    private PiPacketMetadataId(String name) {
        super(name);
    }

    /**
     * Returns the name of the packet metadata.
     *
     * @return packet metadata name
     */
    public String name() {
        return this.identifier;
    }

    /**
     * Returns a identifier with the given name.
     *
     * @param name packet metadata name
     * @return packet metadata identifier
     */
    public static PiPacketMetadataId of(String name) {
        checkNotNull(name);
        checkArgument(!name.isEmpty(), "Name can't be empty");
        return new PiPacketMetadataId(name);
    }
}
