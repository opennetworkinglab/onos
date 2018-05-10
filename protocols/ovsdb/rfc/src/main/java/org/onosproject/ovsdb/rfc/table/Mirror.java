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
import org.onosproject.ovsdb.rfc.notation.Uuid;
import org.onosproject.ovsdb.rfc.schema.DatabaseSchema;
import org.onosproject.ovsdb.rfc.tableservice.AbstractOvsdbTableService;
import org.onosproject.ovsdb.rfc.tableservice.ColumnDescription;

/**
 * This class provides operations of Mirror Table.
 */
public class Mirror extends AbstractOvsdbTableService {
    /**
     * Mirror table column name.
     */
    public enum MirrorColumn {
        NAME("name"), SELECTSRCPORT("select_src_port"), SELECTDSTPORT("select_dst_port"),
        SELECTVLAN("select_vlan"), OUTPUTPORT("output_port"), EXTERNALIDS("external_ids"),
        OUTPUTVLAN("output_vlan"), STATISTICS("statistics"), SELECTALL("select_all");

        private final String columnName;

        MirrorColumn(String columnName) {
            this.columnName = columnName;
        }

        /**
         * Returns the table column name for MirrorColumn.
         * @return the table column name
         */
        public String columnName() {
            return columnName;
        }
    }

    /**
     * Constructs a Mirror object. Generate Mirror Table Description.
     * @param dbSchema DatabaseSchema
     * @param row Row
     */
    public Mirror(DatabaseSchema dbSchema, Row row) {
        super(dbSchema, row, OvsdbTable.MIRROR, VersionNum.VERSION100);
    }

    /**
     * Get the Column entity which column name is "name" from the Row entity of
     * attributes.
     * @return the Column entity which column name is "name"
     */
    public Column getNameColumn() {
        ColumnDescription columndesc = new ColumnDescription(MirrorColumn.NAME.columnName(),
                                                             "getNameColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "name" to the Row entity of
     * attributes.
     * @param name the column data which column name is "name"
     */
    public void setName(String name) {
        ColumnDescription columndesc = new ColumnDescription(MirrorColumn.NAME.columnName(),
                                                             "setName",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, name);
    }

    /**
     * Get the column data which column name is "name" from the Row entity of
     * attributes.
     * @return the column data which column name is "name"
     */
    public String getName() {
        ColumnDescription columndesc = new ColumnDescription(MirrorColumn.NAME.columnName(),
                                                             "getName",
                                                             VersionNum.VERSION100);
        return (String) super.getDataHandler(columndesc);
    }

    /**
     * Get the Column entity which column name is "select_src_port" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "select_src_port"
     */
    public Column getSelectSrcPortColumn() {
        ColumnDescription columndesc = new ColumnDescription(MirrorColumn.SELECTSRCPORT.columnName(),
                                                             "getSelectSrcPortColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "select_src_port" to the Row
     * entity of attributes.
     * @param selectSrcPort the column data which column name is
     *            "select_src_port"
     */
    public void setSelectSrcPort(Set<Uuid> selectSrcPort) {
        ColumnDescription columndesc = new ColumnDescription(MirrorColumn.SELECTSRCPORT.columnName(),
                                                             "setSelectSrcPort", VersionNum.VERSION100);
        super.setDataHandler(columndesc, selectSrcPort);
    }

    /**
     * Get the Column entity which column name is "select_dst_port" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "select_dst_port"
     */
    public Column getSelectDstPortColumn() {
        ColumnDescription columndesc = new ColumnDescription(MirrorColumn.SELECTDSTPORT.columnName(),
                                                             "getSelectDstPortColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "select_dst_port" to the Row
     * entity of attributes.
     * @param selectDstPrt the column data which column name is
     *            "select_dst_port"
     */
    public void setSelectDstPort(Set<Uuid> selectDstPrt) {
        ColumnDescription columndesc = new ColumnDescription(MirrorColumn.SELECTDSTPORT.columnName(),
                                                             "setSelectDstPort", VersionNum.VERSION100);
        super.setDataHandler(columndesc, selectDstPrt);
    }

    /**
     * Get the Column entity which column name is "select_vlan" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "select_vlan"
     */
    public Column getSelectVlanColumn() {
        ColumnDescription columndesc = new ColumnDescription(MirrorColumn.SELECTVLAN.columnName(),
                                                             "getSelectVlanColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "select_vlan" to the Row entity
     * of attributes.
     * @param selectVlan the column data which column name is "select_vlan"
     */
    public void setSelectVlan(Set<Short> selectVlan) {
        ColumnDescription columndesc = new ColumnDescription(MirrorColumn.SELECTVLAN.columnName(),
                                                             "setSelectVlan", VersionNum.VERSION100);
        super.setDataHandler(columndesc, selectVlan);
    }

    /**
     * Get the Column entity which column name is "output_port" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "output_port"
     */
    public Column getOutputPortColumn() {
        ColumnDescription columndesc = new ColumnDescription(MirrorColumn.OUTPUTPORT.columnName(),
                                                             "getOutputPortColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "output_port" to the Row entity
     * of attributes.
     * @param outputPort the column data which column name is "output_port"
     */
    public void setOutputPort(Uuid outputPort) {
        ColumnDescription columndesc = new ColumnDescription(MirrorColumn.OUTPUTPORT.columnName(),
                                                             "setOutputPort", VersionNum.VERSION100);
        super.setDataHandler(columndesc, outputPort);
    }

    /**
     * Get the Column entity which column name is "output_vlan" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "output_vlan"
     */
    public Column getOutputVlanColumn() {
        ColumnDescription columndesc = new ColumnDescription(MirrorColumn.OUTPUTVLAN.columnName(),
                                                             "getOutputVlanColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "output_vlan" to the Row entity
     * of attributes.
     * @param outputVlan the column data which column name is "output_vlan"
     */
    public void setOutputVlan(Short outputVlan) {
        ColumnDescription columndesc = new ColumnDescription(MirrorColumn.OUTPUTVLAN.columnName(),
                                                             "setOutputVlan", VersionNum.VERSION100);
        super.setDataHandler(columndesc, outputVlan);
    }

    /**
     * Get the Column entity which column name is "statistics" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "statistics"
     */
    public Column getStatisticsColumn() {
        ColumnDescription columndesc = new ColumnDescription(MirrorColumn.STATISTICS.columnName(),
                                                             "getStatisticsColumn", VersionNum.VERSION640);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "statistics" to the Row entity
     * of attributes.
     * @param statistics the column data which column name is "statistics"
     */
    public void setStatistics(Map<String, Long> statistics) {
        ColumnDescription columndesc = new ColumnDescription(MirrorColumn.STATISTICS.columnName(),
                                                             "setStatistics", VersionNum.VERSION640);
        super.setDataHandler(columndesc, statistics);
    }

    /**
     * Get the Column entity which column name is "external_ids" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "external_ids"
     */
    public Column getExternalIdsColumn() {
        ColumnDescription columndesc = new ColumnDescription(MirrorColumn.EXTERNALIDS.columnName(),
                                                             "getExternalIdsColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "external_ids" to the Row entity
     * of attributes.
     * @param externalIds the column data which column name is "external_ids"
     */
    public void setExternalIds(Map<String, String> externalIds) {
        ColumnDescription columndesc = new ColumnDescription(MirrorColumn.EXTERNALIDS.columnName(),
                                                             "setExternalIds", VersionNum.VERSION100);
        super.setDataHandler(columndesc, externalIds);
    }

    /**
     * Get the Column entity which column name is "select_all" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "select_all"
     */
    public Column getSelectAllColumn() {
        ColumnDescription columndesc = new ColumnDescription(MirrorColumn.SELECTALL.columnName(),
                                                             "getSelectAllColumn", VersionNum.VERSION620);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "select_all" to the Row entity
     * of attributes.
     * @param selectAll the column data which column name is "select_all"
     */
    public void setSelectAll(Boolean selectAll) {
        ColumnDescription columndesc = new ColumnDescription(MirrorColumn.SELECTALL.columnName(),
                                                             "setSelectAll", VersionNum.VERSION620);
        super.setDataHandler(columndesc, selectAll);
    }
}
