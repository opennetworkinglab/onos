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

import org.onosproject.yangutils.datamodel.YangSchemaNode;
import org.onosproject.yms.app.ydt.exceptions.YdtException;

import static org.onosproject.yms.app.ydt.YdtConstants.FMT_DUP_ENTRY;
import static org.onosproject.yms.app.ydt.YdtConstants.errorMsg;
import static org.onosproject.yms.ydt.YdtType.SINGLE_INSTANCE_NODE;

/**
 * Represents a single instance YANG data tree node.
 */
class YdtSingleInstanceNode extends YdtNode {

    /**
     * Creates a YANG single instance node object.
     *
     * @param node schema of YDT single instance node
     */
    YdtSingleInstanceNode(YangSchemaNode node) {
        super(SINGLE_INSTANCE_NODE, node);
    }

    @Override
    public void validDuplicateEntryProcessing() throws YdtException {
        throw new YdtException(errorMsg(FMT_DUP_ENTRY, getName()));
    }
}
