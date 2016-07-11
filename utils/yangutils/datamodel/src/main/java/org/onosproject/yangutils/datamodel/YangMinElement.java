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

import static org.onosproject.yangutils.datamodel.utils.YangErrMsgConstants.OPERATION_FAILED_ERROR_TAG;
import static org.onosproject.yangutils.datamodel.utils.YangErrMsgConstants.TOO_FEW_ELEMENTS_ERROR_APP_TAG;

import java.io.Serializable;

/**
 * Represents minimum element data represented in YANG.
 */
public class YangMinElement implements YangAppErrorHolder, Serializable {

    private static final long serialVersionUID = 807201695L;

    /**
     * YANG application error information.
     */
    private YangAppErrorInfo yangAppErrorInfo;

    /**
     * Reference:RFC 6020.
     *
     * The "min-elements" statement, which is optional, takes as an argument a
     * non-negative integer that puts a constraint on valid list entries. A
     * valid leaf-list or list MUST have at least min-elements entries.
     *
     * If no "min-elements" statement is present, it defaults to zero.
     *
     * The behavior of the constraint depends on the type of the leaf-list's or
     * list's closest ancestor node in the schema tree that is not a non-
     * presence container:
     *
     * If this ancestor is a case node, the constraint is enforced if any
     * other node from the case exists.
     *
     * Otherwise, it is enforced if the ancestor node exists.
     */
    private int minElement = 0;

    /**
     * Creates a YANG minimum element.
     */
    public YangMinElement() {
        YangAppErrorInfo yangAppErrorInfo = new YangAppErrorInfo();
        yangAppErrorInfo.setErrorTag(OPERATION_FAILED_ERROR_TAG);
        yangAppErrorInfo.setErrorAppTag(TOO_FEW_ELEMENTS_ERROR_APP_TAG);
    }

    /**
     * Returns the minimum element value.
     *
     * @return the minimum element value
     */
    public int getMinElement() {
        return minElement;
    }

    /**
     * Sets the minimum element value.
     *
     * @param minElement the minimum element value
     */
    public void setMinElement(int minElement) {
        this.minElement = minElement;
    }

    @Override
    public void setAppErrorInfo(YangAppErrorInfo yangAppErrorInfo) {
        this.yangAppErrorInfo = yangAppErrorInfo;
    }

    @Override
    public YangAppErrorInfo getAppErrorInfo() {
        return yangAppErrorInfo;
    }
}
