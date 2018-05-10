/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.ovsdb.rfc.table;

import java.util.Map;
import java.util.Set;

import org.onosproject.ovsdb.rfc.notation.Column;
import org.onosproject.ovsdb.rfc.notation.Row;
import org.onosproject.ovsdb.rfc.schema.DatabaseSchema;
import org.onosproject.ovsdb.rfc.tableservice.AbstractOvsdbTableService;
import org.onosproject.ovsdb.rfc.tableservice.ColumnDescription;

/**
 * This class provides operations of Queue Table.
 */
public class Queue extends AbstractOvsdbTableService {
    /**
     * Queue table column name.
     */
    public enum QueueColumn {
        DSCP("dscp"), OTHERCONFIG("other_config"), EXTERNALIDS("external_ids");

        private final String columnName;

        QueueColumn(String columnName) {
            this.columnName = columnName;
        }

        /**
         * Returns the table column name for QueueColumn.
         * @return the table column name
         */
        public String columnName() {
            return columnName;
        }
    }

    /**
     * Constructs a Queue object. Generate Queue Table Description.
     * @param dbSchema DatabaseSchema
     * @param row Row
     */
    public Queue(DatabaseSchema dbSchema, Row row) {
        super(dbSchema, row, OvsdbTable.QUEUE, VersionNum.VERSION100);
    }

    /**
     * Get the Column entity which column name is "dscp" from the Row entity of
     * attributes.
     * @return the Column entity
     */
    public Column getDscpColumn() {
        ColumnDescription columndesc = new ColumnDescription(QueueColumn.DSCP.columnName(), "getDscpColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "dscp" to the Row entity of
     * attributes.
     * @param dscp the column data which column name is "dscp"
     */
    public void setDscp(Set<Long> dscp) {
        ColumnDescription columndesc = new ColumnDescription(QueueColumn.DSCP.columnName(), "setDscp",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, dscp);
    }

    /**
     * Get the Column entity which column name is "other_config" from the Row
     * entity of attributes.
     * @return the Column entity
     */
    public Column getOtherConfigColumn() {
        ColumnDescription columndesc = new ColumnDescription(QueueColumn.OTHERCONFIG.columnName(),
                                                             "getOtherConfigColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "other_config" to the Row entity
     * of attributes.
     * @param otherConfig the column data which column name is "other_config"
     */
    public void setOtherConfig(Map<String, String> otherConfig) {
        ColumnDescription columndesc = new ColumnDescription(QueueColumn.OTHERCONFIG.columnName(),
                                                             "setOtherConfig", VersionNum.VERSION100);
        super.setDataHandler(columndesc, otherConfig);
    }

    /**
     * Get the Column entity which column name is "external_ids" from the Row
     * entity of attributes.
     * @return the Column entity
     */
    public Column getExternalIdsColumn() {
        ColumnDescription columndesc = new ColumnDescription(QueueColumn.EXTERNALIDS.columnName(),
                                                             "getExternalIdsColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "external_ids" to the Row entity
     * of attributes.
     * @param externalIds the column data which column name is "external_ids"
     */
    public void setExternalIds(Map<String, String> externalIds) {
        ColumnDescription columndesc = new ColumnDescription(QueueColumn.EXTERNALIDS.columnName(),
                                                             "setExternalIds", VersionNum.VERSION100);
        super.setDataHandler(columndesc, externalIds);
    }
}
