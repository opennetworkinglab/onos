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

import org.onosproject.yms.ydt.YdtWalker;

/**
 * Abstraction of an entity which provides interfaces for YDT extended walker.
 */
public interface YdtExtendedWalker extends YdtWalker {

    /**
     * Walks the YANG data tree. Protocols implements YDT listener service and
     * walks YDT tree with input as implemented object.
     * YDT walker provides call backs to implemented methods.
     *
     * @param ydtListener YDT listener implemented by the protocol
     * @param rootNode    root node of YDT
     */
    void walk(YdtExtendedListener ydtListener, YdtExtendedContext rootNode);
}
