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

package org.onosproject.yangutils.translator;

/**
 * Type of files generated.
 */
public final class GeneratedFileType {

    /**
     * prevent creating attributes.
     */
    private GeneratedFileType() {
    }

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
    public static final int GENERATE_INTERFACE_WITH_BUILDER = 15;

    /**
     * Java class corresponding to typedef.
     */
    public static final int GENERATE_TYPEDEF_CLASS = 16;
}
