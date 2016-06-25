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

package org.onosproject.yangutils.datamodel.utils;

/**
 * Represents the status of resolvable entity.
 */
public enum ResolvableStatus {

    /**
     * Identifies that resolvable entity is unresolved.
     */
    UNRESOLVED,

    /**
     * Identifies that resolvable entity's reference is linked.
     */
    LINKED,

    /**
     * Identifies that resolvable entity is IntraFile resolved (i.e. complete
     * linking with in the intra file).
     */
    INTRA_FILE_RESOLVED,

    /**
     * Identifies that resolvable entity is resolved.
     */
    RESOLVED,

    /**
     * Identifies that resolvable entity is inter file linked (i.e. complete
     * linking with external files).
     */
    INTER_FILE_LINKED

}
