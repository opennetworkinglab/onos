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

import org.onosproject.ovsdb.rfc.notation.Column;
import org.onosproject.ovsdb.rfc.notation.Row;
import org.onosproject.ovsdb.rfc.notation.Uuid;
import org.onosproject.ovsdb.rfc.schema.DatabaseSchema;
import org.onosproject.ovsdb.rfc.tableservice.AbstractOvsdbTableService;
import org.onosproject.ovsdb.rfc.tableservice.ColumnDescription;

/**
 * This class provides operations of FlowSampleCollectorSet Table.
 */
public class FlowSampleCollectorSet extends AbstractOvsdbTableService {
    /**
     * FlowSampleCollectorSet table column name.
     */
    public enum FlowSampleCollectorSetColumn {
        ID("id"), BRIDGE("bridge"), IPFIX("ipfix"), EXTERNALIDS("external_ids");

        private final String columnName;

        FlowSampleCollectorSetColumn(String columnName) {
            this.columnName = columnName;
        }

        /**
         * Returns the table column name for FlowSampleCollectorSetColumn.
         * @return the table column name
         */
        public String columnName() {
            return columnName;
        }
    }

    /**
     * Constructs a FlowSampleCollectorSet object. Generate
     * FlowSampleCollectorSet Table Description.
     * @param dbSchema DatabaseSchema
     * @param row Row
     */
    public FlowSampleCollectorSet(DatabaseSchema dbSchema, Row row) {
        super(dbSchema, row, OvsdbTable.FLOWSAMPLECOLLECTORSET, VersionNum.VERSION710);
    }

    /**
     * Get the Column entity which column name is "id" from the Row entity of
     * attributes.
     * @return the Column entity which column name is "id"
     */
    public Column getIdColumn() {
        ColumnDescription columndesc = new ColumnDescription(FlowSampleCollectorSetColumn.ID.columnName(),
                                                             "getIdColumn", VersionNum.VERSION710);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "id" to the Row entity of
     * attributes.
     * @param id the column data which column name is "id"
     */
    public void setId(Long id) {
        ColumnDescription columndesc = new ColumnDescription(FlowSampleCollectorSetColumn.ID.columnName(),
                                                             "setId", VersionNum.VERSION710);
        super.setDataHandler(columndesc, id);
    }

    /**
     * Get the Column entity which column name is "bridge" from the Row entity
     * of attributes.
     * @return the Column entity which column name is "bridge"
     */
    public Column getBridgeColumn() {
        ColumnDescription columndesc = new ColumnDescription(FlowSampleCollectorSetColumn.BRIDGE.columnName(),
                                                             "getBridgeColumn", VersionNum.VERSION710);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "bridge" to the Row entity of
     * attributes.
     * @param bridge the column data which column name is "bridge"
     */
    public void setBridge(Uuid bridge) {
        ColumnDescription columndesc = new ColumnDescription(FlowSampleCollectorSetColumn.BRIDGE.columnName(),
                                                             "setBridge", VersionNum.VERSION710);
        super.setDataHandler(columndesc, bridge);
    }

    /**
     * Get the Column entity which column name is "ipfix" from the Row entity of
     * attributes.
     * @return the Column entity which column name is "ipfix"
     */
    public Column getIpfixColumn() {
        ColumnDescription columndesc = new ColumnDescription(FlowSampleCollectorSetColumn.IPFIX.columnName(),
                                                             "getIpfixColumn", VersionNum.VERSION710);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "ipfix" to the Row entity of
     * attributes.
     * @param ipfix the column data which column name is "ipfix"
     */
    public void setIpfix(Uuid ipfix) {
        ColumnDescription columndesc = new ColumnDescription(FlowSampleCollectorSetColumn.IPFIX.columnName(),
                                                             "setIpfix", VersionNum.VERSION710);
        super.setDataHandler(columndesc, ipfix);
    }

    /**
     * Get the Column entity which column name is "external_ids" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "external_ids"
     */
    public Column getExternalIdsColumn() {
        ColumnDescription columndesc = new ColumnDescription(FlowSampleCollectorSetColumn.EXTERNALIDS
                                                                     .columnName(),
                                                             "getExternalIdsColumn", VersionNum.VERSION710);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "external_ids" to the Row entity
     * of attributes.
     * @param externalIds the column data which column name is "external_ids"
     */
    public void setExternalIds(Map<String, String> externalIds) {
        ColumnDescription columndesc = new ColumnDescription(FlowSampleCollectorSetColumn.EXTERNALIDS
                                                                     .columnName(),
                                                             "setExternalIds", VersionNum.VERSION710);
        super.setDataHandler(columndesc, externalIds);
    }
}
