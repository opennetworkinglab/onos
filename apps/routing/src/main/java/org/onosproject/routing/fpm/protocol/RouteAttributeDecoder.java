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

package org.onosproject.routing.fpm.protocol;

import org.onlab.packet.DeserializationException;

/**
 * Decoder for a route attribute.
 */
@FunctionalInterface
public interface RouteAttributeDecoder<A extends RouteAttribute> {

    /**
     * Decodes the a route attribute from the input buffer.
     *
     * @param length length of the attribute
     * @param type type of the attribute
     * @param value input buffer
     * @return route attribute
     * @throws DeserializationException if a route attribute could not be
     * decoded from the input buffer
     */
    A decodeAttribute(int length, int type, byte[] value)
            throws DeserializationException;
}
