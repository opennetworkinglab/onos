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
 * Abstraction of textual description for a YANG entity. Abstracted to unify the
 * parsing and translator processing of description.
 */
public interface YangDesc {

    /**
     * Returns the description of YANG entity.
     *
     * @return the description of YANG entity.
     */
    String getDescription();

    /**
     * Set the description of YANG entity.
     *
     * @param description set the description of YANG entity.
     */
    void setDescription(String description);
}
