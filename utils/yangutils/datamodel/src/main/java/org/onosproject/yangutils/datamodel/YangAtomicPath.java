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

import java.io.Serializable;
import java.util.List;

/**
 * Representation of data model node to maintain absolute path defined in YANG path-arg.
 */
public class YangAtomicPath implements Serializable {

    private static final long serialVersionUID = 806201688L;

    // YANG node identifier.
    private YangNodeIdentifier nodeIdentifier;

    // List of path predicates expression.
    private List<YangPathPredicate> pathPredicatesList;

    /**
     * Resolved node for the absolute path.
     */
    private YangNode resolvedNode;

    /**
     * Returns the node identifier.
     *
     * @return the node identifier
     */
    public YangNodeIdentifier getNodeIdentifier() {
        return nodeIdentifier;
    }

    /**
     * Sets the node identifier.
     *
     * @param nodeIdentifier Sets the node identifier
     */
    public void setNodeIdentifier(YangNodeIdentifier nodeIdentifier) {
        this.nodeIdentifier = nodeIdentifier;
    }

    /**
     * Returns the path predicate expression.
     *
     * @return the path predicate expression
     */
    public List<YangPathPredicate> getPathPredicatesList() {
        return pathPredicatesList;
    }

    /**
     * Sets the path predicate expression.
     *
     * @param pathPredicatesList Sets the path predicate expression
     */
    public void setPathPredicatesList(List<YangPathPredicate> pathPredicatesList) {
        this.pathPredicatesList = pathPredicatesList;
    }

    /**
     * Adds predicate expression in data holder.
     *
     * @param predicatesExp the predicate expression to be added
     */
    public void addLeavesPredicate(YangPathPredicate predicatesExp) {
        getPathPredicatesList().add(predicatesExp);
    }


    /**
     * Returns resolved node.
     *
     * @return resolved node
     */
    public YangNode getResolvedNode() {
        return resolvedNode;
    }

    /**
     * Sets resolved node.
     *
     * @param resolvedNode resolved node
     */
    public void setResolvedNode(YangNode resolvedNode) {
        this.resolvedNode = resolvedNode;
    }
}
