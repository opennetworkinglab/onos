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

/**
 * Abstraction of information about entity being resolved.
 *
 * @param <T> type of entity being resolved, uses / grouping
 */
public interface YangEntityToResolveInfo<T> {

    /**
     * Retrieves the entity to be resolved.
     *
     * @return entity to be resolved
     */
    T getEntityToResolve();

    /**
     * Sets entity to be resolved.
     *
     * @param entityToResolve entity to be resolved
     */
    void setEntityToResolve(T entityToResolve);

    /**
     * Retrieves the parent node which contains the entity to be resolved.
     *
     * @return parent node which contains the entity to be resolved
     */
    YangNode getHolderOfEntityToResolve();

    /**
     * Sets parent node which contains the entity to be resolved.
     *
     * @param holderOfEntityToResolve parent node which contains the entity to
     *                                be resolved
     */
    void setHolderOfEntityToResolve(YangNode holderOfEntityToResolve);
}
