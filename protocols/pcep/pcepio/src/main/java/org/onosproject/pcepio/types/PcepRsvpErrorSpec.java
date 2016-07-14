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

/**
 * Abstraction of an entity which provides PCPE RSVP error spec.
 */
public interface PcepRsvpErrorSpec extends PcepValueType {

    /**
     *  To write the object information to channelBuffer.
     *
     *  @param cb of type channel buffer
     */
    @Override
    int write(ChannelBuffer cb);

    /**
     * Returns class number.
     *
     * @return class number
     */
    byte getClassNum();

    /**
     * Returns class type.
     *
     * @return class type
     */
    byte getClassType();
}
