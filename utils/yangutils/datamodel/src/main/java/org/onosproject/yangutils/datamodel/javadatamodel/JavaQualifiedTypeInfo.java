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

package org.onosproject.yangutils.datamodel.javadatamodel;

import java.io.Serializable;

/**
 * Represents the information about individual imports in the generated file.
 */
public class JavaQualifiedTypeInfo
        implements Serializable {

    private static final long serialVersionUID = 806201634L;

    /**
     * Package location where the imported class/interface is defined.
     */
    protected String pkgInfo;

    /**
     * Class/interface being referenced.
     */
    protected String classInfo;

    /**
     * Returns class info.
     *
     * @return class info
     */
    public String getClassInfo() {
        return classInfo;
    }

    /**
     * Returns package info.
     *
     * @return package info
     */
    public String getPkgInfo() {
        return pkgInfo;
    }

}
