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
package org.onosproject.yangutils.linker.impl;

import java.io.Serializable;

import org.onosproject.yangutils.datamodel.YangEntityToResolveInfo;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.YangUses;
import org.onosproject.yangutils.linker.exceptions.LinkerException;

/**
 * Represents implementation of information about entity being resolved.
 *
 * @param <T> type of entity being resolved, uses / grouping
 */
public class YangEntityToResolveInfoImpl<T> implements YangEntityToResolveInfo<T>, Serializable {

    private static final long serialVersionUID = 806201659L;

    // Parsable node for which resolution is to be performed.
    private T entityToResolve;

    // Holder of the YANG construct for which resolution has to be carried out.
    private YangNode holderOfEntityToResolve;

    @Override
    public T getEntityToResolve() {
        return entityToResolve;
    }

    @Override
    public void setEntityToResolve(T entityToResolve) {
        this.entityToResolve = entityToResolve;
    }

    @Override
    public YangNode getHolderOfEntityToResolve() {
        return holderOfEntityToResolve;
    }

    @Override
    public void setHolderOfEntityToResolve(YangNode holderOfEntityToResolve) {
        this.holderOfEntityToResolve = holderOfEntityToResolve;
    }

    /**
     * Retrieves the prefix of the entity.
     *
     * @return entities prefix
     * @throws LinkerException linker error
     */
    public String getEntityPrefix()
            throws LinkerException {
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
            throw new LinkerException("Linker Exception: Entity to resolved is other than type/uses");
        }
        return prefix;
    }
}
