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

/**
 * Model of a packet metadata for a protocol-independent pipeline.
 */
@Beta
public interface PiPacketMetadataModel {

    /**
     * Returns the ID of this packet metadata.
     *
     * @return packet operation metadata ID
     */
    PiPacketMetadataId id();

    /**
     * Returns the size in bits of this metadata.
     *
     * @return size in bit
     */
    int bitWidth();

    /**
     * Return true is the packet metadata has a fixed bit width.
     * It returns false if it can have flexible bit width.
     *
     * @return True if the packet metadata has fixed bit width, false otherwise
     */
    boolean hasBitWidth();
}
