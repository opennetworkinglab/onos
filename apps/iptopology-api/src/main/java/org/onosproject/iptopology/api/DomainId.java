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

package org.onosproject.iptopology.api;

import org.onlab.util.Identifier;

/**
 * Domain Identifier(32 Bit).
 */
public class DomainId extends Identifier<Integer> {
    /**
     * Constructor to initialize domain identifier.
     *
     * @param domainIdentifier domain identifier
     */
    public DomainId(int domainIdentifier) {
        super(domainIdentifier);
    }

    /**
     * Obtain domain identifier.
     *
     * @return domain identifier
     */
    public int domainIdentifier() {
        return identifier;
    }
}