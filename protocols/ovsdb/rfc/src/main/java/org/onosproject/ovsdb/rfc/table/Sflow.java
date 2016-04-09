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
 * This class provides operations of Sflow Table.
 */
public class Sflow extends AbstractOvsdbTableService {
    /**
     * Sflow table column name.
     */
    public enum SflowColumn {
        TARGETS("targets"), AGENT("agent"), EXTERNALIDS("external_ids"), HAEDER("header"),
        POLLING("polling"), SAMPLING("sampling");

        private final String columnName;

        private SflowColumn(String columnName) {
            this.columnName = columnName;
        }

        /**
         * Returns the table column name for SflowColumn.
         * @return the table column name
         */
        public String columnName() {
            return columnName;
        }
    }

    /**
     * Constructs a Sflow object. Generate Sflow Table Description.
     * @param dbSchema DatabaseSchema
     * @param row Row
     */
    public Sflow(DatabaseSchema dbSchema, Row row) {
        super(dbSchema, row, OvsdbTable.SFLOW, VersionNum.VERSION100);
    }

    /**
     * Get the Column entity which column name is "targets" from the Row entity
     * of attributes.
     * @return the Column entity
     */
    public Column getTargetsColumn() {
        ColumnDescription columndesc = new ColumnDescription(SflowColumn.TARGETS.columnName(),
                                                             "getTargetsColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "targets" to the Row entity of
     * attributes.
     * @param targets the column data which column name is "targets"
     */
    public void setTargets(Set<String> targets) {
        ColumnDescription columndesc = new ColumnDescription(SflowColumn.TARGETS.columnName(), "setTargets",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, targets);
    }

    /**
     * Get the Column entity which column name is "agent" from the Row entity of
     * attributes.
     * @return the Column entity
     */
    public Column getAgentColumn() {
        ColumnDescription columndesc = new ColumnDescription(SflowColumn.AGENT.columnName(),
                                                             "getAgentColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "agent" to the Row entity of
     * attributes.
     * @param agent the column data which column name is "agent"
     */
    public void setAgent(Set<String> agent) {
        ColumnDescription columndesc = new ColumnDescription(SflowColumn.AGENT.columnName(), "setAgent",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, agent);
    }

    /**
     * Get the Column entity which column name is "external_ids" from the Row
     * entity of attributes.
     * @return the Column entity
     */
    public Column getExternalIdsColumn() {
        ColumnDescription columndesc = new ColumnDescription(SflowColumn.EXTERNALIDS.columnName(),
                                                             "getExternalIdsColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "external_ids" to the Row entity
     * of attributes.
     * @param externalIds the column data which column name is "external_ids"
     */
    public void setExternalIds(Map<String, String> externalIds) {
        ColumnDescription columndesc = new ColumnDescription(SflowColumn.EXTERNALIDS.columnName(),
                                                             "setExternalIds", VersionNum.VERSION100);
        super.setDataHandler(columndesc, externalIds);
    }

    /**
     * Get the Column entity which column name is "header" from the Row entity
     * of attributes.
     * @return the Column entity
     */
    public Column getHeaderColumn() {
        ColumnDescription columndesc = new ColumnDescription(SflowColumn.HAEDER.columnName(),
                                                             "getHeaderColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "header" to the Row entity of
     * attributes.
     * @param header the column data which column name is "header"
     */
    public void setHeader(Set<Long> header) {
        ColumnDescription columndesc = new ColumnDescription(SflowColumn.HAEDER.columnName(), "setHeader",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, header);
    }

    /**
     * Get the Column entity which column name is "polling" from the Row entity
     * of attributes.
     * @return the Column entity
     */
    public Column getPollingColumn() {
        ColumnDescription columndesc = new ColumnDescription(SflowColumn.POLLING.columnName(),
                                                             "getPollingColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "polling" to the Row entity of
     * attributes.
     * @param polling the column data which column name is "polling"
     */
    public void setPolling(Set<Long> polling) {
        ColumnDescription columndesc = new ColumnDescription(SflowColumn.POLLING.columnName(), "setPolling",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, polling);
    }

    /**
     * Get the Column entity which column name is "sampling" from the Row entity
     * of attributes.
     * @return the Column entity
     */
    public Column getSamplingColumn() {
        ColumnDescription columndesc = new ColumnDescription(SflowColumn.SAMPLING.columnName(),
                                                             "getSamplingColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "sampling" to the Row entity of
     * attributes.
     * @param sampling the column data which column name is "sampling"
     */
    public void setSampling(Set<Long> sampling) {
        ColumnDescription columndesc = new ColumnDescription(SflowColumn.SAMPLING.columnName(),
                                                             "setSampling", VersionNum.VERSION100);
        super.setDataHandler(columndesc, sampling);
    }
}
