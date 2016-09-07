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

/*-
 * Abstraction of an entity which provides the servers YANG library information.
 *
 * Reference RFC 7895
 * The "ietf-yang-library" module provides information about the YANG
 * library used by a server.  This module is defined using YANG version
 * 1, but it supports the description of YANG modules written in any
 * revision of YANG.
 *
 * Following is the YANG Tree Diagram for the "ietf-yang-library"
 * module:
 *
 *     +--ro modules-state
 *        +--ro module-set-id    string
 *        +--ro module* [name revision]
 *           +--ro name                yang:yang-identifier
 *           +--ro revision            union
 *           +--ro schema?             inet:uri
 *           +--ro namespace           inet:uri
 *           +--ro feature*            yang:yang-identifier
 *           +--ro deviation* [name revision]
 *           |  +--ro name        yang:yang-identifier
 *           |  +--ro revision    union
 *           +--ro conformance-type    enumeration
 *           +--ro submodule* [name revision]
 *              +--ro name        yang:yang-identifier
 *              +--ro revision    union
 *              +--ro schema?     inet:uri
 */

import java.util.List;

public interface YangModuleLibrary {
    /**
     * Retrieves the current module set id of the YANG library.
     *
     * Reference RFC7895.
     * modules-state/module-set-id
     *
     * This mandatory leaf contains a unique implementation-specific
     * identifier representing the current set of modules and submodules on
     * a specific server.  The value of this leaf MUST change whenever the
     * set of modules and submodules in the YANG library changes.  There is
     * no requirement that the same set always results in the same "module-
     * set-id" value.
     *
     * @return module set id of the YANG library
     */
    String getModuleSetId();

    /**
     * Retrieves the current list of YANG modules supported in the server.
     *
     * Reference RFC 7895.
     * modules-state/module
     *
     * This mandatory list contains one entry for each YANG data model
     * module supported by the server.  There MUST be an entry in this list
     * for each revision of each YANG module that is used by the server.  It
     * is possible for multiple revisions of the same module to be imported,
     * in addition to an entry for the revision that is implemented by the
     * server.
     *
     * @return the current list of YANG modules supported in the server
     */
    List<YangModuleInformation> getYangModuleList();
}
