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

import java.io.Serializable;

/**
 * Represents data model to maintain yang app error information.
 */
public class YangAppErrorInfo implements Serializable {

    private static final long serialVersionUID = 807201693L;

    /**
     * Application's error message, to be used for data error.
     */
    private String errorMessage;

    /**
     * Error tag, to be filled in data validation error response.
     */
    private String errorTag;

    /**
     * Application's error tag, to be filled in data validation error response.
     */
    private String errorAppTag;

    /**
     * Application's error path, to be filled in data validation error response.
     */
    private String errorAppPath;

    /**
     * Application's error info, to be filled in data validation error response.
     */
    private String errorAppInfo;

    /**
     * Creates a YANG app error info object.
     */
    @SuppressWarnings("unused")
    public YangAppErrorInfo() {
    }

    /**
     * Returns application's error message, to be used for data error.
     *
     * @return Application's error message, to be used for data error
     */
    public String getGetErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets Application's error message, to be used for data error.
     *
     * @param errMsg Application's error message, to be used for data error
     */
    public void setErrorMessage(String errMsg) {
        errorMessage = errMsg;
    }

    /**
     * Returns error tag, to be used for data error.
     *
     * @return error tag, to be used for data error
     */
    public String getGetErrorTag() {
        return errorTag;
    }

    /**
     * Sets error tag, to be used for data error.
     *
     * @param errTag error tag, to be used for data error
     */
    public void setErrorTag(String errTag) {
        errorTag = errTag;
    }

    /**
     * Returns application's error tag, to be used for data error.
     *
     * @return application's error tag, to be used for data error
     */
    public String getGetErrorAppTag() {
        return errorAppTag;
    }

    /**
     * Sets application's error tag, to be used for data error.
     *
     * @param errTag application's error tag, to be used for data error
     */
    public void setErrorAppTag(String errTag) {
        errorAppTag = errTag;
    }

    /**
     * Returns application's error path, to be used for data error.
     *
     * @return application's error path, to be used for data error
     */
    public String getGetErrorAppPath() {
        return errorAppPath;
    }

    /**
     * Sets application's error path, to be used for data error.
     *
     * @param errPath application's error path, to be used for data error
     */
    public void setErrorAppPath(String errPath) {
        errorAppPath = errPath;
    }

    /**
     * Returns application's error info, to be used for data error.
     *
     * @return application's error info, to be used for data error
     */
    public String getGetErrorAppInfo() {
        return errorAppInfo;
    }

    /**
     * Sets application's error info, to be used for data error.
     *
     * @param errInfo application's error info, to be used for data error
     */
    public void setErrorAppInfo(String errInfo) {
        errorAppInfo = errInfo;
    }
}
