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

/**
 * Abstraction of resolution object which will be resolved by linker.
 *
 * @param <T> type of resolution entity uses / type
 */
public interface YangResolutionInfo<T> extends LocationInfo {

    /**
     * Resolves linking with all the ancestors node for a resolution info.
     *
     * @param dataModelRootNode module/sub-module node
     * @throws DataModelException DataModelException a violation of data model
     *                            rules
     */
    void resolveLinkingForResolutionInfo(YangReferenceResolver dataModelRootNode)
            throws DataModelException;

    /**
     * Retrieves information about the entity that needs to be resolved.
     *
     * @return information about the entity that needs to be resolved
     */
    YangEntityToResolveInfo<T> getEntityToResolveInfo();

    /**
     * Performs inter file linking of uses/type referring to typedef/grouping
     * of other YANG file.
     *
     * @param dataModelRootNode module/sub-module node
     * @throws DataModelException a violation in data model rule
     */
    void linkInterFile(YangReferenceResolver dataModelRootNode)
            throws DataModelException;
}
