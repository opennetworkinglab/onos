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
import static org.onosproject.yms.ydt.YdtType.SINGLE_INSTANCE_LEAF_VALUE_NODE;

/**
 * Represents YDT single instance leaf node which is an atomic element
 * and doesn't have any child.
 */
public class YdtSingleInstanceLeafNode extends YdtNode {

    /*
     * Value of the leaf.
     */
    private String value;

    /*
     * Value of the leaf.
     */
    private Boolean isKeyLeaf = false;

    /**
     * Creates a YANG single instance leaf node.
     *
     * @param node schema of YDT single instance leaf node
     */
    YdtSingleInstanceLeafNode(YangSchemaNode node) {
        super(SINGLE_INSTANCE_LEAF_VALUE_NODE, node);
    }

    /**
     * Returns the flag indicating that requested leaf is key-leaf or not.
     *
     * @return isKeyLeaf true, for key leaf; false non key leaf
     */
    public Boolean isKeyLeaf() {
        return isKeyLeaf;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void addValue(String value) throws YdtException {
        // Check the value against corresponding data-type.
        //TODO validation need to be decided
//        try {
//            getYangSchemaNode().isValueValid(value);
//        } catch (Exception e) {
//            throw new YdtException(e.getLocalizedMessage());
//        }

        // After validation is successful then add value to node.
        this.value = value;
    }


    @Override
    public void addValueWithoutValidation(String value, boolean isKeyLeaf) {
        this.value = value;
        this.isKeyLeaf = isKeyLeaf;
    }

    @Override
    public void validDuplicateEntryProcessing() throws YdtException {
        throw new YdtException(errorMsg(FMT_DUP_ENTRY, getName()));
    }
}
