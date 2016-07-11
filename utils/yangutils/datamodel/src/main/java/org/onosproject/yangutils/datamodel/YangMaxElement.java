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
import static org.onosproject.yangutils.datamodel.utils.YangErrMsgConstants.TOO_MANY_ELEMENTS_ERROR_APP_TAG;

import java.io.Serializable;

/**
 * Represents max element data represented in YANG.
 */
public class YangMaxElement implements YangAppErrorHolder, Serializable {

    private static final long serialVersionUID = 807201694L;

    /**
     * YANG application error information.
     */
    private YangAppErrorInfo yangAppErrorInfo;

    /**
     * Reference:RFC 6020.
     *
     * The "max-elements" statement, which is optional, takes as an argument a
     * positive integer or the string "unbounded", which puts a constraint on
     * valid list entries. A valid leaf-list or list always has at most
     * max-elements entries.
     *
     * If no "max-elements" statement is present, it defaults to "unbounded".
     */
    private int maxElement = Integer.MAX_VALUE;

    /**
     * Creates a YANG maximum element.
     */
    public YangMaxElement() {
        YangAppErrorInfo yangAppErrorInfo = new YangAppErrorInfo();
        yangAppErrorInfo.setErrorTag(OPERATION_FAILED_ERROR_TAG);
        yangAppErrorInfo.setErrorAppTag(TOO_MANY_ELEMENTS_ERROR_APP_TAG);
    }

    /**
     * Returns the maximum element value.
     *
     * @return the maximum element value
     */
    public int getMaxElement() {
        return maxElement;
    }

    /**
     * Sets the maximum element value.
     *
     * @param maxElement the maximum element value
     */
    public void setMaxElement(int maxElement) {
        this.maxElement = maxElement;
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