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
package org.onosproject.net.resource;

import com.google.common.annotations.Beta;

/**
 * Represents the common interface to encode an object of the specified type to an integer,
 * and to decode an integer to an object of the specified type.
 * This class is intended to be used only by the ResourceService implementation.
 */
@Beta
public interface DiscreteResourceCodec<T> {
    /**
     * Encodes the specified object to an integer.
     *
     * @param resource resource
     * @return encoded integer
     */
    int encode(T resource);

    /**
     * Decodes the specified integer to an object.
     *
     * @param value encoded integer
     * @return decoded discrete resource
     */
    T decode(int value);
}
