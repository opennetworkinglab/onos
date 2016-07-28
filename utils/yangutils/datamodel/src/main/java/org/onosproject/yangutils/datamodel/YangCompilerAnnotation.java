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

package org.onosproject.yangutils.datamodel;

import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.datamodel.utils.YangConstructType;

/**
 * Represents data model node to maintain information defined in YANG compiler-annotation.
 */
public class YangCompilerAnnotation implements Parsable {

    /**
     * App data structure information.
     */
    private YangAppDataStructure yangAppDataStructure;

    /**
     * App extended name information.
     */
    private YangAppExtendedName yangAppExtendedName;

    /**
     * Prefix of compiler-annotation.
     */
    private String prefix;

    /**
     * Path of compiler-annotation.
     */
    private String path;

    /**
     * Returns the YANG app data structure information.
     *
     * @return the YANG app data structure information
     */
    public YangAppDataStructure getYangAppDataStructure() {
        return yangAppDataStructure;
    }

    /**
     * Sets the YANG app data structure information.
     *
     * @param yangAppDataStructure the YANG app data structure to set
     */
    public void setYangAppDataStructure(YangAppDataStructure yangAppDataStructure) {
        this.yangAppDataStructure = yangAppDataStructure;
    }

    /**
     * Returns the prefix.
     *
     * @return the prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Sets the prefix information.
     *
     * @param prefix the prefix to set
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Returns the path.
     *
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the path.
     *
     * @param path the path to set
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Returns the YANG app extended name information.
     *
     * @return the YANG app extended name information
     */
    public YangAppExtendedName getYangAppExtendedName() {
        return yangAppExtendedName;
    }

    /**
     * Sets the YANG app extended name information.
     *
     * @param yangAppExtendedName the YANG app extended name to set
     */
    public void setYangAppExtendedName(YangAppExtendedName yangAppExtendedName) {
        this.yangAppExtendedName = yangAppExtendedName;
    }

    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.COMPILER_ANNOTATION_DATA;
    }

    @Override
    public void validateDataOnEntry() throws DataModelException {
        // TODO : to be implemented
    }

    @Override
    public void validateDataOnExit() throws DataModelException {
        // TODO : to be implemented
    }
}
