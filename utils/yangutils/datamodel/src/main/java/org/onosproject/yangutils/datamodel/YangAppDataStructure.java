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

import java.util.LinkedList;
import java.util.List;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.datamodel.utils.YangConstructType;

/**
 * Represents data model node to maintain information defined in YANG app-data-structure.
 */
public class YangAppDataStructure implements Parsable {

    /**
     * Data structure information.
     */
    private YangDataStructure dataStructure;

    /**
     * List of key names.
     */
    private List<String> keyList;

    /**
     * Prefix of app-data-structure.
     */
    private String prefix;

    /**
     * Returns the YANG data structure information.
     *
     * @return the YANG data structure information
     */
    public YangDataStructure getDataStructure() {
        return dataStructure;
    }

    /**
     * Sets the YANG data structure information.
     *
     * @param dataStructure the YANG data structure to set
     */
    public void setDataStructure(YangDataStructure dataStructure) {
        this.dataStructure = dataStructure;
    }

    /**
     * Returns the list of key field names.
     *
     * @return the list of key field names
     */
    public List<String> getKeyList() {
        return keyList;
    }

    /**
     * Sets the list of key field names.
     *
     * @param keyList the list of key field names
     */
    public void setKeyList(List<String> keyList) {
        this.keyList = keyList;
    }

    /**
     * Adds a key field name.
     *
     * @param key key field name
     */
    public void addKey(String key) {
        if (getKeyList() == null) {
            setKeyList(new LinkedList<>());
        }
        getKeyList().add(key);
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

    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.APP_DATA_STRUCTURE;
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
