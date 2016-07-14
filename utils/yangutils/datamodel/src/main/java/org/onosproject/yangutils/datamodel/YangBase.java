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

import java.io.Serializable;

/**
 * Reference RFC 6020.
 *
 * Represents data model node to maintain information defined in YANG base.
 *  The "base" statement, which is optional, takes as an argument a
 * string that is the name of an existing identity, from which the new
 * identity is derived.  If no "base" statement is present, the identity
 * is defined from scratch.
 *
 * If a prefix is present on the base name, it refers to an identity
 * defined in the module that was imported with that prefix, or the
 * local module if the prefix matches the local module's prefix.
 * Otherwise, an identity with the matching name MUST be defined in the
 * current module or an included submodule.
 */

/**
 * Represents data model node to maintain information defined in YANG base.
 */
 public class YangBase implements Resolvable, Serializable {

    private static final long serialVersionUID = 806201693L;

    // YANG node identifier.
    private YangNodeIdentifier baseIdentifier;

    // Referred identity parent information.
    private YangIdentity referredIdentity;

     /**
     * Status of resolution. If completely resolved enum value is "RESOLVED",
     * if not enum value is "UNRESOLVED", in case reference of grouping/typedef/base/identityref
     * is added to uses/type/base/identityref but it's not resolved value of enum should be
     * "INTRA_FILE_RESOLVED".
     */
    private ResolvableStatus resolvableStatus;

    // Creates a base type of node.
    public YangBase() {
        resolvableStatus = ResolvableStatus.UNRESOLVED;
    }

    /**
     * Returns the YANG node identifier.
     *
     * @return the YANG node identifier
     */
    public YangNodeIdentifier getBaseIdentifier() {
        return baseIdentifier;
    }

    /**
     * Sets the YANG node identifier.
     *
     * @param baseIdentifier the YANG node identifier to set
     */
    public void setBaseIdentifier(YangNodeIdentifier baseIdentifier) {
        this.baseIdentifier = baseIdentifier;
    }

    /**
     * Returns the parent identity node.
     *
     * @return the parent identity node
     */
    public YangIdentity getReferredIdentity() {
        return referredIdentity;
    }

    /**
     * Sets the parent identity node.
     *
     * @param referredIdentity the parent identity node to set
     */
    public void setReferredIdentity(YangIdentity referredIdentity) {
        this.referredIdentity = referredIdentity;
    }

    @Override
    public ResolvableStatus getResolvableStatus() {
        return resolvableStatus;
    }

    @Override
    public void setResolvableStatus(ResolvableStatus resolvableStatus) {
        this.resolvableStatus = resolvableStatus;
    }

    @Override
    public void resolve() throws DataModelException {
    }
}
