/*
 * Copyright 2016 Open Networking Laboratory
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

package org.onosproject.yangutils.translator.tojava.utils;

/**
 * Data Store types.
 */
public enum TempDataStoreTypes {

    /**
     * Getter methods for interfaces.
     */
    GETTER_METHODS,

    /**
     * Getter methods impl for classes.
     */
    GETTER_METHODS_IMPL,

    /**
     * Setter methods for interfaces.
     */
    SETTER_METHODS,

    /**
     * Setter methods impl for classes.
     */
    SETTER_METHODS_IMPL,

    /**
     * Constructor for impl class.
     */
    CONSTRUCTOR,

    /**
     * Attributes.
     */
    ATTRIBUTE,

    /**
     * TypeDef.
     */
    TYPE_DEF,

    /**
     * ToString method.
     */
    TO_STRING,

    /**
     * HashCode method.
     */
    HASH_CODE,

    /**
     * Equals method.
     */
    EQUALS
}
