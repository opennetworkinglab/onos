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

package org.onosproject.pcepio.types;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.protocol.PcepVersion;

/**
 * Abstraction which Provides the PCEP Values of Type, Length ,Version.
 */
public interface PcepValueType {

    /**
     * Returns the Version Of PCEP Message.
     *
     * @return Version of PcepVersion Type.
     */
    PcepVersion getVersion();

    /**
     * Returns the Type of PCEP Message.
     *
     * @return value of type
     */
    short getType();

    /**
     * Returns the Length of PCEP Message.
     *
     * @return value of Length
     */
    short getLength();

    /**
     * Writes the byte Stream of PCEP Message to channel buffer.
     *
     * @param bb of type channel buffer
     * @return length of bytes written to channel buffer
     */
    int write(ChannelBuffer bb);
}
