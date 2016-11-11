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


import org.onosproject.yms.ydt.YdtListener;

/**
 * Abstraction of an entity which provide call back methods which are called
 * by YDT extended walker while walking the YANG data tree.
 * <p>
 * This interface needs to be implemented by protocol implementing listener's
 * based call backs while YDT walk, and update application specific information
 * in data node.
 */
public interface YdtExtendedListener extends YdtListener {

    /**
     * YANG data tree node's entry, it will be called during a node entry.
     * <p>
     * All the related information about the node can be obtain from the YDT
     * context. Also it can be used to maintain / query application specific
     * information.
     *
     * @param ydtExtendedContext YANG data tree context
     */
    void enterYdtNode(YdtExtendedContext ydtExtendedContext);

    /**
     * YANG data tree node's exit, it will be called during a node exit.
     * <p>
     * All the related information about the node can be obtain from the YDT
     * context. Also it can be used to maintain / query application specific
     * information.
     *
     * @param ydtExtendedContext YANG data tree context
     */
    void exitYdtNode(YdtExtendedContext ydtExtendedContext);
}
