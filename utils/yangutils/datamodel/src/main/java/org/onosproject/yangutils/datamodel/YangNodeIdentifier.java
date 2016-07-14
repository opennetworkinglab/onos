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
 * Represents YANG node identifier which is a combination of prefix and name.
 */
public class YangNodeIdentifier implements Serializable {

    private static final long serialVersionUID = 806201648L;

    // Name of the node.
    private String name;

    // Prefix of the node.
    private String prefix;

    /**
     * Creates an instance of YANG node identifier.
     */
    public YangNodeIdentifier() {
    }

    /**
     * Returns name of the node identifier.
     *
     * @return name of the node identifier
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name of the node identifier.
     *
     * @param name name of the node identifier
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns prefix of the node identifier.
     *
     * @return name of the node identifier
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Sets prefix of the node identifier.
     *
     * @param prefix prefix of the node identifier
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
