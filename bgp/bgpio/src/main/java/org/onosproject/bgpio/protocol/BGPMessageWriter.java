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
 */

package org.onosproject.bgpio.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BGPParseException;

/**
 * Abstraction of an entity providing BGP Message Writer.
 */
public interface BGPMessageWriter<T> {

    /**
     * Writes the Objects of the BGP Message into Channel Buffer.
     *
     * @param cb Channel Buffer
     * @param message BGP Message
     * @throws BGPParseException
     *                     While writing message
     */
     void write(ChannelBuffer cb, T message) throws BGPParseException;
}