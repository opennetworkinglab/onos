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

package org.onosproject.yms.ypm;

import org.onosproject.yms.ydt.YdtContext;

/**
 * Abstraction of an entity which provides interfaces to YANG Protocol Metadata Manager.
 */
public interface YpmService {

    /**
     * Returns the protocol data stored in sepecific data model path.
     *
     * @param rootNode YANG data tree
     * @return YANG protocol metadata
     */
    YpmContext getProtocolData(YdtContext rootNode);

    /**
     * Sets the YANG protocol metadata in specific ydt path in ypm tree.
     *
     * @param rootNode YANG data tree
     * @param data YANG protocol metadata
     */
    void setProtocolData(YdtContext rootNode, Object data);
}
