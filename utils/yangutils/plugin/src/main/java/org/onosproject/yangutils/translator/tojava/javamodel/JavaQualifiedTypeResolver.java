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
package org.onosproject.yangutils.translator.tojava.javamodel;

import org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfoContainer;
import org.onosproject.yangutils.utils.io.impl.YangToJavaNamingConflictUtil;

/**
 * Represent java based identification of the YANG leaves.
 */
public interface JavaQualifiedTypeResolver
        extends JavaQualifiedTypeInfoContainer {

    /**
     * updates the qualified access details of the type.
     *
     * @param confilictResolver plugin configurations
     */
    void updateJavaQualifiedInfo(YangToJavaNamingConflictUtil confilictResolver);
}
