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

import org.onosproject.yangutils.datamodel.YangNamespace;

import java.util.List;

/**
 * Abstraction of an entity which provides YANG module information.
 *
 * Reference RFC 7895
 * The following information is needed by a client application (for each
 * YANG module in the library) to fully utilize the YANG data modeling
 * language:
 *
 * o  name: The name of the YANG module.
 *
 * o  revision: Each YANG module and submodule within the library has a
 * revision.  This is derived from the most recent revision statement
 * within the module or submodule.  If no such revision statement
 * exists, the module's or submodule's revision is the zero-length
 * string.
 *
 * o  submodule list: The name and revision of each submodule used by
 * the module MUST be identified.
 *
 * o  feature list: The name of each YANG feature supported by the
 * server MUST be identified.
 *
 * o  deviation list: The name of each YANG module used for deviation
 * statements MUST be identified.
 */
public interface YangModuleInformation {
    /**
     * Retrieves the YANG modules identifier.
     *
     * @return YANG modules identifier
     */
    YangModuleIdentifier moduleIdentifier();

    /**
     * Retrieves the YANG modules namespace.
     * The XML namespace identifier for this module.
     *
     * @return YANG modules namespace
     */
    YangNamespace namespace();

    /**
     * Reference RFC 7895
     * Retrieves the list of YANG feature names from this module that are
     * supported by the server, regardless of whether they are
     * defined in the module or any included submodule.
     *
     * @return list of YANG features
     */
    List<String> featureList();

    /**
     * Retrieves the list of submodules in the module.
     * The name and revision of each submodule used by
     * the module MUST be identified.
     *
     * Each entry represents one submodule within the
     * parent module.
     *
     * @return list of submodules in the module
     */
    List<YangModuleIdentifier> subModuleIdentifiers();
}
