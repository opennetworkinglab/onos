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
package org.onosproject.yangutils.datamodel;

/*
 * Reference:RFC 6020.
 * The "status" statement takes as an argument one of the strings
 * "current", "deprecated", or "obsolete". If no status is specified,
 * the default is "current".
 */

/**
 * Represents the status of YANG entities.
 */
public enum YangStatusType {
    /**
     * Reference:RFC 6020.
     *
     * "current" means that the definition is current and valid.
     */
    CURRENT,

    /**
     * Reference:RFC 6020.
     *
     * "deprecated" indicates an obsolete definition, but it
     * permits new/ continued implementation in order to foster interoperability
     * with older/existing implementations.
     */
    DEPRECATED,

    /**
     * Reference:RFC 6020.
     *
     * "obsolete" means the definition is obsolete and
     * SHOULD NOT be implemented and/or can be removed from implementations.
     */
    OBSOLETE
}
