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

package org.onosproject.yangutils.translator.tojava;

/**
 * Type of files generated.
 */
public final class GeneratedTempFileType {

    /**
     * prevent creating attributes.
     */
    private GeneratedTempFileType() {
    }

    /**
     * attributes definition temporary file.
     */
    public static final int ATTRIBUTES_MASK = 1;

    /**
     * getter methods for interface.
     */
    public static final int GETTER_FOR_INTERFACE_MASK = 2;

    /**
     * getter methods for class.
     */
    public static final int GETTER_FOR_CLASS_MASK = 4;

    /**
     * setter methods for interface.
     */
    public static final int SETTER_FOR_INTERFACE_MASK = 8;

    /**
     * setter methods for class.
     */
    public static final int SETTER_FOR_CLASS_MASK = 16;

    /**
     * constructor method of class.
     */
    public static final int CONSTRUCTOR_IMPL_MASK = 32;

    /**
     * hash code implementation of class.
     */
    public static final int HASH_CODE_IMPL_MASK = 64;

    /**
     * equals implementation of class.
     */
    public static final int EQUALS_IMPL_MASK = 128;

    /**
     * to string implementation of class.
     */
    public static final int TO_STRING_IMPL_MASK = 256;
}
