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
import org.onosproject.ovsdb.rfc.schema.DatabaseSchema;
import org.onosproject.ovsdb.rfc.tableservice.AbstractOvsdbTableService;
import org.onosproject.ovsdb.rfc.tableservice.ColumnDescription;

/**
 * This class provides operations of FlowTable Table.
 */
public class FlowTable extends AbstractOvsdbTableService {
    /**
     * FlowTable table column name.
     */
    public enum FlowTableColumn {
        FLOWLIMIT("flow_limit"), OVERFLOWPOLICY("overflow_policy"), GROUPS("groups"), NAME("name"),
        PREFIXES("prefixes"), EXTERNALIDS("external_ids");

        private final String columnName;

        private FlowTableColumn(String columnName) {
            this.columnName = columnName;
        }

        /**
         * Returns the table column name for FlowTableColumn.
         * @return the table column name
         */
        public String columnName() {
            return columnName;
        }
    }

    /**
     * Constructs a FlowTable object. Generate FlowTable Table Description.
     * @param dbSchema DatabaseSchema
     * @param row Row
     */
    public FlowTable(DatabaseSchema dbSchema, Row row) {
        super(dbSchema, row, OvsdbTable.FLWTABLE, VersionNum.VERSION650);
    }

    /**
     * Get the Column entity which column name is "flow_limit" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "flow_limit"
     */
    public Column getFlowLimitColumn() {
        ColumnDescription columndesc = new ColumnDescription(FlowTableColumn.FLOWLIMIT.columnName(),
                                                             "getFlowLimitColumn", VersionNum.VERSION650);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "flow_limit" to the Row entity
     * of attributes.
     * @param flowLimit the column data which column name is "flow_limit"
     */
    public void setFlowLimit(Set<Long> flowLimit) {
        ColumnDescription columndesc = new ColumnDescription(FlowTableColumn.FLOWLIMIT.columnName(),
                                                             "setFlowLimit", VersionNum.VERSION650);
        super.setDataHandler(columndesc, flowLimit);
    }

    /**
     * Get the Column entity which column name is "overflow_policy" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "overflow_policy"
     */
    public Column getOverflowPolicyColumn() {
        ColumnDescription columndesc = new ColumnDescription(FlowTableColumn.OVERFLOWPOLICY.columnName(),
                                                             "getOverflowPolicyColumn", VersionNum.VERSION650);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "overflow_policy" to the Row
     * entity of attributes.
     * @param overflowPolicy the column data which column name is
     *            "overflow_policy"
     */
    public void setOverflowPolicy(Set<String> overflowPolicy) {
        ColumnDescription columndesc = new ColumnDescription(FlowTableColumn.OVERFLOWPOLICY.columnName(),
                                                             "setOverflowPolicy", VersionNum.VERSION650);
        super.setDataHandler(columndesc, overflowPolicy);
    }

    /**
     * Get the Column entity which column name is "groups" from the Row entity
     * of attributes.
     * @return the Column entity which column name is "groups"
     */
    public Column getGroupsColumn() {
        ColumnDescription columndesc = new ColumnDescription(FlowTableColumn.GROUPS.columnName(),
                                                             "getGroupsColumn", VersionNum.VERSION650);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "groups" to the Row entity of
     * attributes.
     * @param groups the column data which column name is "groups"
     */
    public void setGroups(Set<String> groups) {
        ColumnDescription columndesc = new ColumnDescription(FlowTableColumn.GROUPS.columnName(),
                                                             "setGroups", VersionNum.VERSION650);
        super.setDataHandler(columndesc, groups);
    }

    /**
     * Get the Column entity which column name is "name" from the Row entity of
     * attributes.
     * @return the Column entity which column name is "name"
     */
    public Column getNameColumn() {
        ColumnDescription columndesc = new ColumnDescription(FlowTableColumn.NAME.columnName(),
                                                             "getNameColumn", VersionNum.VERSION650);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "name" to the Row entity of
     * attributes.
     * @param name the column data which column name is "name"
     */
    public void setName(Set<String> name) {
        ColumnDescription columndesc = new ColumnDescription(FlowTableColumn.NAME.columnName(),
                                                             "setName",
                                                             VersionNum.VERSION650);
        super.setDataHandler(columndesc, name);
    }

    /**
     * Get the Column entity which column name is "prefixes" from the Row entity
     * of attributes.
     * @return the Column entity which column name is "prefixes"
     */
    public Column getPrefixesColumn() {
        ColumnDescription columndesc = new ColumnDescription(FlowTableColumn.PREFIXES.columnName(),
                                                             "getPrefixesColumn", VersionNum.VERSION740);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "prefixes" to the Row entity of
     * attributes.
     * @param prefixes the column data which column name is "prefixes"
     */
    public void setPrefixes(Set<String> prefixes) {
        ColumnDescription columndesc = new ColumnDescription(FlowTableColumn.PREFIXES.columnName(),
                                                             "setPrefixes", VersionNum.VERSION740);
        super.setDataHandler(columndesc, prefixes);
    }

    /**
     * Get the Column entity which column name is "external_ids" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "external_ids"
     */
    public Column getExternalIdsColumn() {
        ColumnDescription columndesc = new ColumnDescription(FlowTableColumn.EXTERNALIDS.columnName(),
                                                             "getExternalIdsColumn", VersionNum.VERSION750);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "external_ids" to the Row entity
     * of attributes.
     * @param externalIds the column data which column name is "external_ids"
     */
    public void setExternalIds(Map<String, String> externalIds) {
        ColumnDescription columndesc = new ColumnDescription(FlowTableColumn.EXTERNALIDS.columnName(),
                                                             "setExternalIds", VersionNum.VERSION750);
        super.setDataHandler(columndesc, externalIds);
    }

}
