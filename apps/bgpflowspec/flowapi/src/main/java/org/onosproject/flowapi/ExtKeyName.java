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
package org.onosproject.flowapi;

/**
 * Ext flwo key name class.
 */
public interface ExtKeyName extends ExtFlowTypes {

    /**
     * Returns the ExtType.
     *
     * @return the ExtType
     */
    ExtType type();

    /**
     * Returns the key name identifier for the flow.
     *
     * @return the key name
     */
    String keyName();

    /**
     * Returns whether this key name is an exact match to the key name given
     * in the argument.
     *
     * @param key other key name to match against
     * @return true if the key name are an exact match, otherwise false
     */
    boolean exactMatch(ExtKeyName key);

    /**
     * A key name value list builder..
     */
    interface Builder {

        /**
         * Assigns the ExtType to this object.
         *
         * @param type extended type
         * @return this the builder object
         */
        Builder setType(ExtType type);

        /**
         * Assigns the key name to this object.
         *
         * @param key the key name
         * @return this the builder object
         */
        Builder setKeyName(String key);

        /**
         * Builds a key name object.
         *
         * @return a key name object.
         */
        ExtKeyName build();
    }
}
