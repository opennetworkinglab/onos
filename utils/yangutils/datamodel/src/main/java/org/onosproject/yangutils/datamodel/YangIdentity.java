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
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.datamodel.utils.YangConstructType;

import java.io.Serializable;

/*-
 * Reference RFC 6020.
 *
 *  The "identity" statement is used to define a new globally unique,
 *  abstract, and untyped identity.  Its only purpose is to denote its
 *  name, semantics, and existence.  An identity can either be defined
 *  from scratch or derived from a base identity.  The identity's
 *  argument is an identifier that is the name of the identity.  It is
 *  followed by a block of substatements that holds detailed identity
 *  information.
 *
 *  The identity's Substatements
 *
 *                +--------------+---------+-------------+-----------------------+
 *                | substatement | section | cardinality |  data model mapping   |
 *                +--------------+---------+-------------+-----------------------+
 *                | base         | 7.16.2  | 0..1        |  -YangNodeIdentifier  |
 *                | description  | 7.19.3  | 0..1        |  -string              |
 *                | reference    | 7.19.4  | 0..1        |  -string              |
 *                | status       | 7.19.2  | 0..1        |  -YangStatus          |
 *                +--------------+---------+-------------+-----------------------+
 */

/**
 * Represents data model node to maintain information defined in YANG identity.
 */
public class YangIdentity extends YangNode implements YangCommonInfo, Parsable, Serializable {

    private static final long serialVersionUID = 806201691L;

    //Name of the identity.
    private String name;

    //Base node of identity.
    private YangBase baseNode;

    //Status of YANG identity.
    private YangStatusType status;

    //Description of YANG identity.
    private String description;

    //YANG reference of the identity.
    private String reference;

    //Creates a identity type of node.
    public YangIdentity() {
        super(YangNodeType.IDENTITY_NODE);
    }

    /**
     * Returns the name of identity.
     *
     * @return the identity name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of identity.
     *
     * @param name the identity name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public YangStatusType getStatus() {
        return status;
    }

    @Override
    public void setStatus(YangStatusType status) {
        this.status = status;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getReference() {
        return reference;
    }

    @Override
    public void setReference(String reference) {
        this.reference = reference;
    }

    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.IDENTITY_DATA;
    }

    @Override
    public void validateDataOnEntry() throws DataModelException {
    }

    @Override
    public void validateDataOnExit() throws DataModelException {
    }

    /**
     * Returns base node of identity.
     *
     * @return the base node of identity
     */
    public YangBase getBaseNode() {
        return baseNode;
    }

    /**
     * Sets the base node.
     *
     * @param baseNode the base node to set
     */
    public void setBaseNode(YangBase baseNode) {
        this.baseNode = baseNode;
    }
}
