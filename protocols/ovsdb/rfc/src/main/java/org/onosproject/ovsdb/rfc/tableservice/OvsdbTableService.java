/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.ovsdb.rfc.tableservice;

import org.onosproject.ovsdb.rfc.notation.Column;
import org.onosproject.ovsdb.rfc.notation.Uuid;

/**
 * Representation of conversion between Ovsdb table and Row.
 */
public interface OvsdbTableService {

    /**
     * Get Column from row.
     * @param columndesc Column description
     * @return Column
     */
    public Column getColumnHandler(ColumnDescription columndesc);

    /**
     * Get Data from row.
     * @param columndesc Column description
     * @return Object column data
     */
    public Object getDataHandler(ColumnDescription columndesc);

    /**
     * Set column data of row.
     * @param columndesc Column description
     * @param obj column data
     */
    public void setDataHandler(ColumnDescription columndesc, Object obj);

    /**
     * Returns UUID which column name is _uuid.
     * @return UUID
     */
    public Uuid getTableUuid();

    /**
     * Returns UUID Column which column name is _uuid.
     * @return UUID Column
     */
    public Column getTableUuidColumn();

    /**
     * Returns UUID which column name is _version.
     * @return UUID
     */
    public Uuid getTableVersion();

    /**
     * Returns UUID Column which column name is _version.
     * @return UUID Column
     */
    public Column getTableVersionColumn();
}
