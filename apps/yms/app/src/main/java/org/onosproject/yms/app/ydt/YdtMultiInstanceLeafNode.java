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

import com.google.common.collect.ImmutableSet;
import org.onosproject.yangutils.datamodel.YangSchemaNodeIdentifier;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;

import java.util.HashSet;
import java.util.Set;

import static org.onosproject.yms.app.ydt.YdtConstants.errorMsg;
import static org.onosproject.yms.ydt.YdtType.MULTI_INSTANCE_LEAF_VALUE_NODE;

/**
 * Represents YDT multi instance leaf node which can hold multiple values, it
 * is atomic element and doesn't have any child.
 */
class YdtMultiInstanceLeafNode extends YdtNode {

    // ydt formatted error string
    private static final String FMT_DUP_ENTRY =
            "Duplicate entry found under %s leaf-list node.";

    /**
     * Set of values.
     */
    private final Set<String> valueSet = new HashSet<>();

    /**
     * Creates a YANG multi instance leaf node.
     *
     * @param id node identifier of YDT multi instance node
     */
    protected YdtMultiInstanceLeafNode(YangSchemaNodeIdentifier id) {
        super(MULTI_INSTANCE_LEAF_VALUE_NODE, id);
    }

    @Override
    public Set<String> getValueSet() {
        return ImmutableSet.copyOf(valueSet);
    }

    @Override
    public void addValue(String value) {
        // check the value against corresponding data-type.
        try {
            getYangSchemaNode().isValueValid(value);
        } catch (Exception e) {
            errorHandler(e.getLocalizedMessage(), this);
        }
        addValueToValueSet(value);
    }

    /**
     * Adds value in the current node valueSet, after successful validation of
     * the value.
     *
     * @param value value to be added
     */
    private void addValueToValueSet(String value) {

        if (!valueSet.add(value)) {
            errorHandler(errorMsg(FMT_DUP_ENTRY,
                                  getYdtNodeIdentifier().getName()), this);
        }
    }

    @Override
    public void addValueSet(Set valueSet) {
        String value = null;
        // Check the value against corresponding data-type.
        for (Object aValueSet : valueSet) {

            try {
                value = String.valueOf(aValueSet);
                getYangSchemaNode().isValueValid(value);
            } catch (DataModelException e) {
                errorHandler(e.getLocalizedMessage(), this);
            }
            addValueToValueSet(value);
        }
    }

    @Override
    public void addValueWithoutValidation(String value) {
        valueSet.add(value);
    }

    @Override
    public void addValueSetWithoutValidation(Set valueSet) {
        this.valueSet.clear();
        this.valueSet.addAll(valueSet);
    }
}
