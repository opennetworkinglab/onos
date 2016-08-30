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

package org.onosproject.yms.ych;

/**
 * Abstraction of an entity which has the composite protocol request.
 *
 * Protocols like RESTCONF, have split the schema specific information across
 * different components in the protocol encoding.
 *
 * There is a resource identifier, which is part of the RESTCONF request URL.
 * and there is the information about the resource being operated on in the
 * request, this is part of the request body.
 */
public interface YangCompositeEncoding {

    /**
     * Retrieves the resource identifier on which the operation is being
     * performed.
     *
     * @return the string representation of the resource being identified
     */
    String getResourceIdentifier();

    /**
     * Retrieves the representation format of the resource identifier.
     *
     * @return the type of the resource identifier
     */
    YangResourceIdentifierType getResourceIdentifierType();

    /**
     * Retrieves the resource information in the protocol encoding format.
     *
     * @return the resource information in the protocol encoding format
     */
    String getResourceInformation();
}
