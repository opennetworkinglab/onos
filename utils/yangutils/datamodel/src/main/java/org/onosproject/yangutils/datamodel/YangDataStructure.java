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

/**
 * Represents ENUM to identify the YANG data type.
 */
public enum YangDataStructure {

    MAP,

    LIST,

    SET;

    /**
     * Returns YANG data structure type for corresponding data structure name.
     *
     * @param name data structure name from YANG file.
     * @return YANG data structure for corresponding data structure name.
     */
    public static YangDataStructure getType(String name) {
        name = name.replace("\"", "");
        for (YangDataStructure dataStructure : values()) {
            if (dataStructure.name().toLowerCase().equals(name)) {
                return dataStructure;
            }
        }
        return null;
    }
}
