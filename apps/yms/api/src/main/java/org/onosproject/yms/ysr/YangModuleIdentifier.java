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

package org.onosproject.yms.ysr;

/**
 * Abstraction of an entity which provides YANG module identifiers.
 * Reference RFC 7895
 * YANG library provides information about all the YANG modules
 * used by a network management server
 */
public interface YangModuleIdentifier {
    /**
     * retrieves the name of the YANG module.
     *
     * @return the name of the YANG module
     */
    String moduleName();

    /**
     * Retrieves revision of the YANG module.
     * <p>
     * Reference RFC 7895
     * Each YANG module and submodule within the library has a
     * revision.  This is derived from the most recent revision statement
     * within the module or submodule.  If no such revision statement
     * exists, the module's or submodule's revision is the zero-length
     * string.
     *
     * @return revision of the YANG module
     */
    String revision();
}
