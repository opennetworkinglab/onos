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

package org.onosproject.yangutils.translator.tojava;

/**
 * Represents type of temporary files generated.
 */
public final class GeneratedTempFileType {

    /**
     * Attributes definition temporary file.
     */
    public static final int ATTRIBUTES_MASK = 1;

    /**
     * Getter methods for interface.
     */
    public static final int GETTER_FOR_INTERFACE_MASK = 2;

    /**
     * Getter methods for class.
     */
    public static final int GETTER_FOR_CLASS_MASK = 4;

    /**
     * Setter methods for interface.
     */
    public static final int SETTER_FOR_INTERFACE_MASK = 8;

    /**
     * Setter methods for class.
     */
    public static final int SETTER_FOR_CLASS_MASK = 16;

    /**
     * Constructor method of class.
     */
    public static final int CONSTRUCTOR_IMPL_MASK = 32;

    /**
     * Hash code implementation of class.
     */
    public static final int HASH_CODE_IMPL_MASK = 64;

    /**
     * Equals implementation of class.
     */
    public static final int EQUALS_IMPL_MASK = 128;

    /**
     * To string implementation of class.
     */
    public static final int TO_STRING_IMPL_MASK = 256;

    /**
     * Of string implementation of class.
     */
    public static final int OF_STRING_IMPL_MASK = 512;

    /**
     * Constructor for type class like typedef, union.
     */
    public static final int CONSTRUCTOR_FOR_TYPE_MASK = 1024;

    /**
     * From string implementation of class.
     */
    public static final int FROM_STRING_IMPL_MASK = 2048;

    /**
     * Enum implementation of class.
     */
    public static final int ENUM_IMPL_MASK = 4096;

    /**
     * Rpc interface of module / sub module.
     */
    public static final int RPC_INTERFACE_MASK = 8192;

    /**
     * Rpc implementation of module / sub module.
     */
    public static final int RPC_IMPL_MASK = 16384;

    /**
     * Event enum implementation of class.
     */
    public static final int EVENT_ENUM_MASK = 32768;

    /**
     * Event method implementation of class.
     */
    public static final int EVENT_METHOD_MASK = 65536;

    /**
     * Event subject attribute implementation of class.
     */
    public static final int EVENT_SUBJECT_ATTRIBUTE_MASK = 131072;

    /**
     * Event subject getter implementation of class.
     */
    public static final int EVENT_SUBJECT_GETTER_MASK = 262144;

    /**
     * Event subject setter implementation of class.
     */
    public static final int EVENT_SUBJECT_SETTER_MASK = 524288;

    /**
     * Creates an instance of generated temp file type.
     */
    private GeneratedTempFileType() {
    }
}
