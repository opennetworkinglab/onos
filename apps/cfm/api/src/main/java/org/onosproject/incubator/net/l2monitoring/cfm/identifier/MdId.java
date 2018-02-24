/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.incubator.net.l2monitoring.cfm.identifier;

/**
 * Representation of a Maintenance Domain ID.
 *
 * The MD Id consists of a name and a name type.
 * In certain applications the MD name and type are embedded in to a TLV and
 * passed between systems, and so it is important that all combinations of the
 * name and name type can be represented here.
 *
 * For example the name "test1.domain.tld" could be a valid with either a
 * CharacterString name type and a DomainName name type. Both could be present
 * on the same system concurrently and must be distinguished by the name type
 *
 * IEEE 802.1Q Table 21-19—Maintenance Domain Name Format.
 */
public interface MdId {
    public static final int MD_NAME_MAX_LEN = 45;
    public static final int MD_NAME_MIN_LEN = 1;

    /**
     * Get the MD name as a string.
     * @return A string representation of the name
     */
    String mdName();

    /**
     * Get the length of the MD name.
     * @return The length of the name in bytes
     */
    int getNameLength();

    /**
     * The type of the name.
     * @return An enumerated value
     */
    MdNameType nameType();

    /**
     * Supported types of MA identifier.
     */
    enum MdNameType {
        /**
         * Implemented as {@link MdIdCharStr}.
         */
        CHARACTERSTRING,
        /**
         * Implemented as {@link MdIdDomainName}.
         */
        DOMAINNAME,
        /**
         * Implemented as {@link MdIdMacUint}.
         */
        MACANDUINT,
        /**
         * Implemented as {@link MdIdNone}.
         */
        NONE
    }
}
