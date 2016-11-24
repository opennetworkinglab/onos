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

package org.onosproject.bgpio.types;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * Abstraction which Provides the BGP of TLV format.
 */
public interface BgpValueType {
    /**
     * Returns the Type of BGP Message.
     *
     * @return short value of type
     */
    short getType();

    /**
     * Writes the byte Stream of BGP Message to channel buffer.
     *
     * @param cb channel buffer
     * @return length written to channel buffer
     */
    int write(ChannelBuffer cb);

    /**
     * Compares two objects.
     *
     * @param o object
     * @return result after comparing two objects
     */
    int compareTo(Object o);
}
