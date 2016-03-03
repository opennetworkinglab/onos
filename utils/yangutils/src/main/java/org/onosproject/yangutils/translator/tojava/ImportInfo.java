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

import java.util.Objects;

import com.google.common.base.MoreObjects;

/**
 * Maintains the information about individual imports in the generated file.
 */
public class ImportInfo implements Comparable {

    /**
     * Package location where the imported class/interface is defined.
     */
    private String pkgInfo;

    /**
     * Class/interface being referenced.
     */
    private String classInfo;

    /**
     * Default constructor.
     */
    public ImportInfo() {
    }

    /**
     * Get the imported package info.
     *
     * @return the imported package info
     */
    public String getPkgInfo() {
        return pkgInfo;
    }

    /**
     * Set the imported package info.
     *
     * @param pkgInfo the imported package info
     */
    public void setPkgInfo(String pkgInfo) {
        this.pkgInfo = pkgInfo;
    }

    /**
     * Get the imported class/interface info.
     *
     * @return the imported class/interface info
     */
    public String getClassInfo() {
        return classInfo;
    }

    /**
     * Set the imported class/interface info.
     *
     * @param classInfo the imported class/interface info
     */
    public void setClassInfo(String classInfo) {
        this.classInfo = classInfo;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pkgInfo, classInfo);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ImportInfo) {
            ImportInfo other = (ImportInfo) obj;
            return Objects.equals(pkgInfo, other.pkgInfo) &&
                    Objects.equals(classInfo, other.classInfo);
        }
        return false;
    }

    /**
     * check if the import info matches.
     *
     * @param importInfo matched import
     * @return if equal or not
     */
    public boolean exactMatch(ImportInfo importInfo) {
        return equals(importInfo)
                && Objects.equals(pkgInfo, importInfo.getPkgInfo())
                && Objects.equals(classInfo, importInfo.getClassInfo());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("pkgInfo", pkgInfo)
                .add("classInfo", classInfo).toString();
    }

    /**
     * Check that there is no 2 objects with the same class name.
     *
     * @param o compared import info.
     */
    @Override
    public int compareTo(Object o) {
        ImportInfo other;
        if (o instanceof ImportInfo) {
            other = (ImportInfo) o;
        } else {
            return -1;
        }
        return getClassInfo().compareTo(other.getClassInfo());
    }

}
