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
 * Represents type of java files generated.
 */
public final class GeneratedJavaFileType {

    /**
     * Interface file.
     */
    public static final int INTERFACE_MASK = 1;

    /**
     * Builder interface file.
     */
    public static final int BUILDER_INTERFACE_MASK = 2;

    /**
     * Builder class file.
     */
    public static final int BUILDER_CLASS_MASK = 4;

    /**
     * Impl class file.
     */
    public static final int IMPL_CLASS_MASK = 8;

    /**
     * Interface and class file.
     */
    public static final int GENERATE_INTERFACE_WITH_BUILDER = INTERFACE_MASK
            | BUILDER_INTERFACE_MASK | BUILDER_CLASS_MASK | IMPL_CLASS_MASK;

    /**
     * Java interface corresponding to rpc.
     */
    public static final int GENERATE_SERVICE_AND_MANAGER = 16;

    /**
     * Java class corresponding to YANG enumeration.
     */
    public static final int GENERATE_ENUM_CLASS = 32;

    /**
     * Java class corresponding to typedef.
     */
    public static final int GENERATE_TYPEDEF_CLASS = 64;

    /**
     * Java class corresponding to union.
     */
    public static final int GENERATE_UNION_CLASS = 128;

    /**
     * Java class corresponding to typedef.
     */
    public static final int GENERATE_TYPE_CLASS = GENERATE_TYPEDEF_CLASS
            | GENERATE_UNION_CLASS;

    /**
     * Event class.
     */
    public static final int GENERATE_EVENT_CLASS = 256;

    /**
     * Event listener class.
     */
    public static final int GENERATE_EVENT_LISTENER_INTERFACE = 512;

    /**
     * Event listener class.
     */
    public static final int GENERATE_EVENT_SUBJECT_CLASS = 1024;

    /**
     * Creates an instance of generate java file type.
     */
    private GeneratedJavaFileType() {
    }
}
