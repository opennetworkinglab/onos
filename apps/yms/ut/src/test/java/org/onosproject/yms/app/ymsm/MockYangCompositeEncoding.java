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

package org.onosproject.yms.app.ymsm;

import org.onosproject.yms.ych.YangCompositeEncoding;
import org.onosproject.yms.ych.YangResourceIdentifierType;

/**
 * Represents implementation of YangCompositeEncoding interfaces.
 */
public class MockYangCompositeEncoding implements YangCompositeEncoding {
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
     * Sets resource identifier.
     *
     * @param resIdentifier resource identifier
     */
    public void setResourceIdentifier(String resIdentifier) {
        resourceIdentifier = resIdentifier;
    }

    /**
     * Sets the resource information.
     *
     * @param resInformation resource information
     */
    public void setResourceInformation(String resInformation) {
        resourceInformation = resInformation;
    }

    /**
     * Sets the resource identifier type.
     *
     * @param resIdent resource identifier
     */
    public void setResourceIdentifierType(YangResourceIdentifierType resIdent) {
        resourceIdentifierType = resIdent;
    }

    @Override
    public String getResourceIdentifier() {
        return resourceIdentifier;
    }

    @Override
    public YangResourceIdentifierType getResourceIdentifierType() {
        return resourceIdentifierType;
    }

    @Override
    public String getResourceInformation() {
        return resourceInformation;
    }
}
