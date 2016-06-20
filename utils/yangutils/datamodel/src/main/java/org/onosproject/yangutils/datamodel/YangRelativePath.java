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
 * Representation of data model node to maintain relative path defined in YANG path-arg.
 */
public class YangRelativePath implements Serializable {

    private static final long serialVersionUID = 806201690L;

    // Relative path ancestor node count the number of node exist between current node to parent node.
    private int ancestorNodeCount;

    // Absolute path expression.
    private List<YangAtomicPath> atomicPathList;

    /**
     * Returns the absolute path.
     *
     * @return the absolute path
     */
    public List<YangAtomicPath> getAtomicPathList() {
        return atomicPathList;
    }

    /**
     * Sets the absolute path.
     *
     * @param atomicPathList Sets the absolute path
     */
    public void setAtomicPathList(List<YangAtomicPath> atomicPathList) {
        this.atomicPathList = atomicPathList;
    }

    /**
     * Returns the relative path ancestor count.
     *
     * @return the relative path ancestor count
     */
    public int getAncestorNodeCount() {
        return ancestorNodeCount;
    }

    /**
     * Sets the relative path ancestor count.
     *
     * @param ancestorNodeCount Sets the relative path ancestor count
     */
    public void setAncestorNodeCount(int ancestorNodeCount) {
        this.ancestorNodeCount = ancestorNodeCount;
    }
}
