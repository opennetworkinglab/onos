/*
 * Copyright 2016 Open Networking Laboratory
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

package org.onosproject.yangutils.datamodel.utils;

import org.onosproject.yangutils.datamodel.CollisionDetector;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangLeavesHolder;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.utils.YangConstructType;

/**
 * Utilities for data model tree.
 */
public final class DataModelUtils {

    /**
     * Creates a new data model tree utility.
     */
    private DataModelUtils() {
    }

    /**
     * Detects the colliding identifier name in a given YANG node and its child.
     *
     * @param identifierName name for which collision detection is to be
     *            checked.
     * @param dataType type of YANG node asking for detecting collision.
     * @param node instance of calling node.
     * @throws DataModelException a violation of data model rules.
     */
    public static void detectCollidingChildUtil(String identifierName, YangConstructType dataType, YangNode node)
            throws DataModelException {
        if (((YangLeavesHolder) node).getListOfLeaf() != null) {
            for (YangLeaf leaf : ((YangLeavesHolder) node).getListOfLeaf()) {
                if (leaf.getLeafName().equals(identifierName)) {
                    throw new DataModelException("YANG file error: Duplicate input identifier detected, same as leaf \""
                            + leaf.getLeafName() + "\"");
                }
            }
        }
        if (((YangLeavesHolder) node).getListOfLeafList() != null) {
            for (YangLeafList leafList : ((YangLeavesHolder) node).getListOfLeafList()) {
                if (leafList.getLeafName().equals(identifierName)) {
                    throw new DataModelException("YANG file error: Duplicate input identifier detected, same as leaf " +
                            "list \"" + leafList.getLeafName() + "\"");
                }
            }
        }
        node = node.getChild();
        while ((node != null)) {
            if (node instanceof CollisionDetector) {
                ((CollisionDetector) node).detectSelfCollision(identifierName, dataType);
            }
            node = node.getNextSibling();
        }
    }
}
