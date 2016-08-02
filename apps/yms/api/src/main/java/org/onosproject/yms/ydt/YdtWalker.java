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

package org.onosproject.yms.ydt;

/**
 * Abstraction of an entity which provides interfaces for YDT walk.
 *
 * When YANG management system gets data from application to be returned
 * to protocol for any protocol operation or as a part of notification, YANG
 * management system encodes this data in a YANG data tree and sends the same
 * to protocol.
 * Protocols can use the YANG data tree walker utility to have their
 * callbacks to be invoked as per the YANG data tree walking.
 * By this way protocols can encode the data from abstract YANG data tree
 * into a protocol specific representation.
 *
 * YDT walker provides entry and exit callbacks for each node in YANG data
 * tree.
 */
public interface YdtWalker {

    /**
     * Walks the YANG data tree. Protocols implements YDT listener service
     * and walks YDT tree with input as implemented object. YDT walker provides
     * call backs to implemented methods.
     *
     * @param ydtListener YDT listener implemented by the protocol
     * @param rootNode    root node of YDT
     */
    void walk(YdtListener ydtListener, YdtContext rootNode);
}
