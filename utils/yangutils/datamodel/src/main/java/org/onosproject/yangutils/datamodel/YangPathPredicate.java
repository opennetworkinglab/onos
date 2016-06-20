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

/**
 * Representation of data model node to maintain path predicate in YANG absolute-path or relative-path.
 */
public class YangPathPredicate<T> implements Serializable {

    private static final long serialVersionUID = 806201689L;

    // YANG node identifier.
    private YangNodeIdentifier nodeIdentifier;

    // Left axis node will represent the nodeidentifier before equality sign in path predicate.
    private T leftAxisNode;

    // YANG path operator.
    private YangPathOperator pathOperator;

    // YANG relative path.
    private YangRelativePath rightRelativePath;

    // Right axis node will represent the nodeidentifier after equality sign in path predicate.
    private T rightAxisNode;

    /**
     * Returns the path expression operator.
     *
     * @return the path expression operator
     */
    public YangPathOperator getPathOperator() {
        return pathOperator;
    }

    /**
     * Sets the path expression operator.
     *
     * @param pathOperator Sets the path expression operator
     */
    public void setPathOperator(YangPathOperator pathOperator) {
        this.pathOperator = pathOperator;
    }

    /**
     * Returns the right relative path expression.
     *
     * @return the right relative path expression
     */
    public YangRelativePath getRightRelativePath() {
        return rightRelativePath;
    }

    /**
     * Sets the right relative path expression.
     *
     * @param rightRelativePath Sets the right relative path expression
     */
    public void setRightRelativePath(YangRelativePath rightRelativePath) {
        this.rightRelativePath = rightRelativePath;
    }

    /**
     * Returns the nodeidentifier.
     *
     * @return the nodeidentifier
     */
    public YangNodeIdentifier getNodeIdentifier() {
        return nodeIdentifier;
    }

    /**
     * Sets the YANG node identifier.
     *
     * @param nodeIdentifier Sets the node identifier
     *
     */
    public void setNodeIdentifier(YangNodeIdentifier nodeIdentifier) {
        this.nodeIdentifier = nodeIdentifier;
    }

    /**
     * Returns the left axis node.
     *
     * @return the left axis node
     */
    public T getLeftAxisNode() {
        return leftAxisNode;
    }

    /**
     * Sets the left axis node.
     *
     * @param leftAxisNode Sets the left axis node
     */
    public void setLeftAxisNode(T leftAxisNode) {
        this.leftAxisNode = leftAxisNode;
    }

    /**
     * Returns the right axis node.
     *
     * @return the right axis node
     */
    public T getRightAxisNode() {
        return rightAxisNode;
    }

    /**
     * Sets the right axis node.
     *
     * @param rightAxisNode Sets the right axis node
     */
    public void setRightAxisNode(T rightAxisNode) {
        this.rightAxisNode = rightAxisNode;
    }
}
