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

import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfoContainer;
import org.onosproject.yangutils.utils.io.impl.YangToJavaNamingConflictUtil;

/**
 * Represent java based identification of the YANG leaves.
 */
public interface JavaLeafInfoContainer
        extends JavaQualifiedTypeInfoContainer {
    /**
     * Retreives the data type of the leaf.
     *
     * @return data type of the leaf
     */
    YangType<?> getDataType();

    /**
     * Retreives the name of the leaf.
     *
     * @return name of the leaf
     */
    String getName();

    /**
     * Retreives the java name of the leaf.
     *
     * @param conflictResolveConfig user config to resolve conflicts
     * @return name of the leaf
     */
    String getJavaName(YangToJavaNamingConflictUtil conflictResolveConfig);

    /**
     * Identifies if object is a leaf-list.
     *
     * @return true if leaf-list false otherwise
     */
    boolean isLeafList();

    /**
     * updates the qualified info.
     */
    void updateJavaQualifiedInfo();

    /**
     * Returns java naming conflict resolver.
     *
     * @return  java naming conflict resolver
     */
    YangToJavaNamingConflictUtil getConflictResolveConfig();

    /**
     * Sets  java naming conflict resolver.
     *
     * @param conflictResolveConfig  java naming conflict resolver
     */
    void setConflictResolveConfig(YangToJavaNamingConflictUtil conflictResolveConfig);

}
