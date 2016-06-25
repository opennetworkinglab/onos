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

import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.ResolvableStatus;

/**
 * Abstraction of YANG resolvable information. Abstracted to obtain the
 * information required for linking resolution.
 */
public interface Resolvable {

    /**
     * Returns the status of resolution. If completely resolved returns enum
     * value "RESOLVED", if not returns "UNRESOLVED", in case reference of
     * grouping/typedef is added to uses/type but it's not resolved
     * "INTRA_FILE_RESOLVED" is returned.
     *
     * @return status of resolution
     */
    ResolvableStatus getResolvableStatus();

    /**
     * Set the status of type/uses resolution. If completely resolved set enum
     * value "RESOLVED", if not set it to "UNRESOLVED", in case reference of
     * grouping/typedef is added to uses/type but it's not resolved
     * "INTRA_FILE_RESOLVED" should be set.
     *
     * @param resolvableStatus status of resolution
     */
    void setResolvableStatus(ResolvableStatus resolvableStatus);

    /**
     * Resolves the linking.
     *
     * @throws DataModelException data model exception
     */
    void resolve()
            throws DataModelException;
}
