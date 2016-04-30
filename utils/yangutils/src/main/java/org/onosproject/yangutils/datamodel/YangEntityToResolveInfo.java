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
 * Represents information about entity being resolved.
 *
 * @param <T> type of entity being resolved, uses / grouping
 */
public class YangEntityToResolveInfo<T> {

    // Parsable node for which resolution is to be performed.
    private T entityToResolve;

    // Holder of the YANG construct for which resolution has to be carried out.
    private YangNode holderOfEntityToResolve;

    /**
     * Retrieves the entity to be resolved.
     *
     * @return entity to be resolved
     */
    public T getEntityToResolve() {
        return entityToResolve;
    }

    /**
     * Sets entity to be resolved.
     *
     * @param entityToResolve entity to be resolved
     */
    public void setEntityToResolve(T entityToResolve) {
        this.entityToResolve = entityToResolve;
    }

    /**
     * Retrieves the parent node which contains the entity to be resolved.
     *
     * @return parent node which contains the entity to be resolved
     */
    public YangNode getHolderOfEntityToResolve() {
        return holderOfEntityToResolve;
    }

    /**
     * Sets parent node which contains the entity to be resolved.
     *
     * @param holderOfEntityToResolve parent node which contains the entity to
     * be resolved
     */
    public void setHolderOfEntityToResolve(YangNode holderOfEntityToResolve) {
        this.holderOfEntityToResolve = holderOfEntityToResolve;
    }

    /**
     * Retrieves the prefix of the entity.
     *
     * @return entities prefix
     * @throws DataModelException data model error
     */
    public String getEntityPrefix()
            throws DataModelException {
        if (getEntityToResolve() == null) {
            return null;
        }

        String prefix;
        T entityToBeResolved = getEntityToResolve();
        if (entityToBeResolved instanceof YangType) {
            prefix = ((YangType<?>) entityToBeResolved).getPrefix();
        } else if (entityToBeResolved instanceof YangUses) {
            prefix = ((YangUses) entityToBeResolved).getPrefix();
        } else {
            throw new DataModelException("Data Model Exception: Entity to resolved is other than type/uses");
        }
        return prefix;
    }
}
