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
package org.onosproject.ovsdb.rfc.table;

import java.util.Map;
import java.util.Set;

import org.onosproject.ovsdb.rfc.notation.Column;
import org.onosproject.ovsdb.rfc.notation.Row;
import org.onosproject.ovsdb.rfc.notation.Uuid;
import org.onosproject.ovsdb.rfc.schema.DatabaseSchema;
import org.onosproject.ovsdb.rfc.tableservice.AbstractOvsdbTableService;
import org.onosproject.ovsdb.rfc.tableservice.ColumnDescription;

/**
 * This class provides operations of Qos Table.
 */
public class Qos extends AbstractOvsdbTableService {
    /**
     * Qos table column name.
     */
    public enum QosColumn {
        QUEUES("queues"), TYPE("type"), OTHERCONFIG("other_config"), EXTERNALIDS("external_ids");

        private final String columnName;

        private QosColumn(String columnName) {
            this.columnName = columnName;
        }

        /**
         * Returns the table column name for QosColumn.
         * @return the table column name
         */
        public String columnName() {
            return columnName;
        }
    }

    /**
     * Constructs a Qos object. Generate Qos Table Description.
     * @param dbSchema DatabaseSchema
     * @param row Row
     */
    public Qos(DatabaseSchema dbSchema, Row row) {
        super(dbSchema, row, OvsdbTable.QOS, VersionNum.VERSION100);
    }

    /**
     * Get the Column entity which column name is "queues" from the Row entity
     * of attributes.
     * @return the Column entity
     */
    public Column getQueuesColumn() {
        ColumnDescription columndesc = new ColumnDescription(QosColumn.QUEUES.columnName(),
                                                             "getQueuesColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "queues" to the Row entity of
     * attributes.
     * @param queues the column data which column name is "queues"
     */
    public void setQueues(Map<Long, Uuid> queues) {
        ColumnDescription columndesc = new ColumnDescription(QosColumn.QUEUES.columnName(), "setQueues",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, queues);
    }

    /**
     * Get the Column entity which column name is "type" from the Row entity of
     * attributes.
     * @return the Column entity
     */
    public Column getTypeColumn() {
        ColumnDescription columndesc = new ColumnDescription(QosColumn.TYPE.columnName(), "getTypeColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "type" to the Row entity of
     * attributes.
     * @param type the column data which column name is "type"
     */
    public void setType(Set<String> type) {
        ColumnDescription columndesc = new ColumnDescription(QosColumn.TYPE.columnName(), "setType",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, type);
    }

    /**
     * Get the Column entity which column name is "other_config" from the Row
     * entity of attributes.
     * @return the Column entity
     */
    public Column getOtherConfigColumn() {
        ColumnDescription columndesc = new ColumnDescription(QosColumn.OTHERCONFIG.columnName(),
                                                             "getOtherConfigColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "other_config" to the Row entity
     * of attributes.
     * @param otherConfig the column data which column name is "other_config"
     */
    public void setOtherConfig(Map<String, String> otherConfig) {
        ColumnDescription columndesc = new ColumnDescription(QosColumn.OTHERCONFIG.columnName(),
                                                             "setOtherConfig", VersionNum.VERSION100);
        super.setDataHandler(columndesc, otherConfig);
    }

    /**
     * Get the Column entity which column name is "external_ids" from the Row
     * entity of attributes.
     * @return the Column entity
     */
    public Column getExternalIdsColumn() {
        ColumnDescription columndesc = new ColumnDescription(QosColumn.EXTERNALIDS.columnName(),
                                                             "getExternalIdsColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "external_ids" to the Row entity
     * of attributes.
     * @param externalIds the column data which column name is "external_ids"
     */
    public void setExternalIds(Map<String, String> externalIds) {
        ColumnDescription columndesc = new ColumnDescription(QosColumn.EXTERNALIDS.columnName(),
                                                             "setExternalIds", VersionNum.VERSION100);
        super.setDataHandler(columndesc, externalIds);
    }
}
