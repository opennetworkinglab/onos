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
 * Represents data model node to maintain information defined in YANG extended name.
 */
public class YangAppExtendedName implements Parsable {

    /**
     * App extended name information.
     */
    private String yangAppExtendedName;

    /**
     * Prefix of extended name.
     */
    private String prefix;

    /**
     * Returns the YANG app extended name information.
     *
     * @return the YANG app extended name information
     */
    public String getYangAppExtendedName() {
        return yangAppExtendedName;
    }

    /**
     * Sets the YANG app extended name information.
     *
     * @param yangAppExtendedName the YANG app extended name to set
     */
    public void setYangAppExtendedName(String yangAppExtendedName) {
        this.yangAppExtendedName = yangAppExtendedName;
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
        return YangConstructType.APP_EXTENDED_NAME_DATA;
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
