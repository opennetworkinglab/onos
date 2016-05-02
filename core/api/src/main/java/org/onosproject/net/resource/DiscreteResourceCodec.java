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
 * Represents the common interface to encode a discrete resource to an integer,
 * and to decode an integer to a discrete resource.
 * This class is intended to be used only by the ResourceService implementation.
 */
@Beta
public interface DiscreteResourceCodec {
    /**
     * Encodes the specified discrete resource to an integer.
     *
     * @param resource resource
     * @return encoded integer
     */
    int encode(DiscreteResource resource);

    /**
     * Decodes the specified integer to a discrete resource.
     *
     * @param parent parent of the returned resource
     * @param value encoded integer
     * @return decoded discrete resource
     */
    DiscreteResource decode(DiscreteResourceId parent, int value);
}
