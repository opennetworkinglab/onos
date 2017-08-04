/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.net.domain;

import org.onlab.util.Identifier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of a domain identity.
 */
public class DomainId extends Identifier<String> {

    private static final int DOMAIN_ID_MAX_LENGTH = 1024;

    /**
     * Represents the domain directly managed by ONOS.
     */
    public static final DomainId LOCAL = domainId("local");

    /**
     * Constructor of the peer id.
     *
     * @param identifier of the peer
     */
    public DomainId(String identifier) {
        super(identifier);
    }

    /**
     * Creates a peer id from the string identifier.
     *
     * @param identifier string identifier
     * @return instance of the class DomainId
     */
    public static DomainId domainId(String identifier) {
        checkNotNull(identifier, "identifier cannot be null");
        checkArgument(identifier.length() <= DOMAIN_ID_MAX_LENGTH,
                "identifier exceeds maximum length " + DOMAIN_ID_MAX_LENGTH);
        return new DomainId(identifier);
    }
}
