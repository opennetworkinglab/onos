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

package org.onosproject.provider.te.utils;

import org.onosproject.yms.ych.YangCompositeEncoding;
import org.onosproject.yms.ych.YangResourceIdentifierType;

/**
 * Represents implementation of YangCompositeEncoding interfaces.
 */
public class YangCompositeEncodingImpl implements YangCompositeEncoding {

    /**
     * Resource identifier for composite encoding.
     */
    private String resourceIdentifier;

    /**
     * Resource information for composite encoding.
     */
    private String resourceInformation;

    /**
     * Resource identifier type.
     */
    private YangResourceIdentifierType resourceIdentifierType;

    /**
     * Creates an instance of YangCompositeEncodingImpl.
     *
     * @param resIdType is URI
     * @param resId     is the URI string
     * @param resInfo   is the JSON body string
     */
    public YangCompositeEncodingImpl(YangResourceIdentifierType resIdType,
                                     String resId,
                                     String resInfo) {
        this.resourceIdentifierType = resIdType;
        this.resourceIdentifier = resId;
        this.resourceInformation = resInfo;
    }

    public String getResourceIdentifier() {
        return resourceIdentifier;
    }

    public YangResourceIdentifierType getResourceIdentifierType() {
        return resourceIdentifierType;
    }

    public String getResourceInformation() {
        return resourceInformation;
    }

    public void setResourceIdentifier(String resourceId) {
        resourceIdentifier = resourceId;
    }

    public void setResourceInformation(String resourceInfo) {
        resourceInformation = resourceInfo;
    }

    public void setResourceIdentifierType(YangResourceIdentifierType idType) {
        resourceIdentifierType = idType;
    }
}

