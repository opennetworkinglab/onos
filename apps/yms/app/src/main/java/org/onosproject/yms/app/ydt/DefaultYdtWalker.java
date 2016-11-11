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

package org.onosproject.yms.app.ydt;


import org.onosproject.yms.app.utils.TraversalType;
import org.onosproject.yms.ydt.YdtContext;
import org.onosproject.yms.ydt.YdtListener;

import static org.onosproject.yms.app.utils.TraversalType.CHILD;
import static org.onosproject.yms.app.utils.TraversalType.PARENT;
import static org.onosproject.yms.app.utils.TraversalType.ROOT;
import static org.onosproject.yms.app.utils.TraversalType.SIBLING;

/**
 * Represents implementation of YDT walker, which walks the YDT.
 */
public class DefaultYdtWalker implements YdtExtendedWalker {

    @Override
    public void walk(YdtListener ydtListener, YdtContext rootNode) {
        walkTree(ydtListener, rootNode, false);
    }

    /**
     * Walks the YANG data tree till the node provided by the user.
     * Protocols implements YDT listener and YDT Extended Listener and
     * walks YDT tree with input as implemented object.
     * YDT walker provides call backs to implemented methods.
     *
     * @param ydtListener YDT listener implemented by the protocol
     * @param rootNode    root node of YDT
     * @param isExtended  flag denotes the call type
     */
    private void walkTree(YdtListener ydtListener, YdtContext rootNode,
                          boolean isExtended) {
        YdtContext curNode = rootNode;
        TraversalType curTraversal = ROOT;

        while (curNode != null) {
            if (curTraversal != PARENT) {

                // Visit (curNode) for entry callback
                if (isExtended) {
                    ((YdtExtendedListener) ydtListener)
                            .enterYdtNode((YdtExtendedContext) curNode);
                } else {
                    ydtListener.enterYdtNode(curNode);
                }
            }
            if (curTraversal != PARENT &&
                    curNode.getFirstChild() != null) {
                curTraversal = CHILD;
                curNode = curNode.getFirstChild();
            } else if (curNode.getNextSibling() != null) {
                // Revisit (curNode) for exit callback
                exitCallBack(ydtListener, curNode, isExtended);

                /*
                 *Stop traversing the tree , tree need to be traversed
                 * till user requested node
                 */
                if (curNode.equals(rootNode)) {
                    return;
                }
                curTraversal = SIBLING;
                curNode = curNode.getNextSibling();
            } else {
                // Revisit (curNode) for exit callback
                exitCallBack(ydtListener, curNode, isExtended);

                /*
                 *Stop traversing the tree , tree need to be traversed
                 * till user requested node
                 */
                if (curNode.equals(rootNode)) {
                    return;
                }

                curTraversal = PARENT;
                curNode = curNode.getParent();
            }
        }
    }

    /**
     * Provides exit call back per node on the basis of extended flag,
     * If isExtended set then YdtExtendedListener exit node call back will
     * be provided else YdtListener with respective type of context
     * (YdtContext/YdtExtendedContext).
     *
     * @param ydtListener YDT listener implemented by the protocol
     * @param curNode     current node of YDT
     * @param isExtended  flag denotes the call type
     */
    private void exitCallBack(YdtListener ydtListener, YdtContext curNode,
                              boolean isExtended) {
        if (isExtended) {
            ((YdtExtendedListener) ydtListener)
                    .exitYdtNode((YdtExtendedContext) curNode);
        } else {
            ydtListener.exitYdtNode(curNode);
        }
    }

    @Override
    public void walk(YdtExtendedListener ydtExtendedListener,
                     YdtExtendedContext rootNode) {
        walkTree(ydtExtendedListener, rootNode, true);
    }
}
