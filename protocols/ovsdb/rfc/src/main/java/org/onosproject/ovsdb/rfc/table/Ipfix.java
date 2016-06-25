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
 * This class provides operations of Ipfix Table.
 */
public class Ipfix extends AbstractOvsdbTableService {
    /**
     * Ipfix table column name.
     */
    public enum IpfixColumn {
        TARGETS("targets"), SAMPLING("sampling"), OBSDOMAINID("obs_domain_id"), OBSPOINTID("obs_point_id"),
        CACHEACTIVETIMEOUT("cache_active_timeout"), EXTERNALIDS("external_ids"),
        CACHEMAXFLOWS("cache_max_flows");

        private final String columnName;

        private IpfixColumn(String columnName) {
            this.columnName = columnName;
        }

        /**
         * Returns the table column name for IpfixColumn.
         * @return the table column name
         */
        public String columnName() {
            return columnName;
        }
    }

    /**
     * Constructs a Ipfix object. Generate Ipfix Table Description.
     * @param dbSchema DatabaseSchema
     * @param row Row
     */
    public Ipfix(DatabaseSchema dbSchema, Row row) {
        super(dbSchema, row, OvsdbTable.IPFIX, VersionNum.VERSION710);
    }

    /**
     * Get the Column entity which column name is "targets" from the Row entity
     * of attributes.
     * @return the Column entity which column name is "targets"
     */
    public Column getTargetsColumn() {
        ColumnDescription columndesc = new ColumnDescription(IpfixColumn.TARGETS.columnName(),
                                                             "getTargetsColumn", VersionNum.VERSION710);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "targets" to the Row entity of
     * attributes.
     * @param targets the column data which column name is "targets"
     */
    public void setTargets(Set<String> targets) {
        ColumnDescription columndesc = new ColumnDescription(IpfixColumn.TARGETS.columnName(),
                                                             "setTargets",
                                                             VersionNum.VERSION710);
        super.setDataHandler(columndesc, targets);
    }

    /**
     * Get the Column entity which column name is "sampling" from the Row entity
     * of attributes.
     * @return the Column entity which column name is "sampling"
     */
    public Column getSamplingColumn() {
        ColumnDescription columndesc = new ColumnDescription(IpfixColumn.SAMPLING.columnName(),
                                                             "getSamplingColumn", VersionNum.VERSION710);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "sampling" to the Row entity of
     * attributes.
     * @param sampling the column data which column name is "sampling"
     */
    public void setSampling(Set<Long> sampling) {
        ColumnDescription columndesc = new ColumnDescription(IpfixColumn.SAMPLING.columnName(),
                                                             "setSampling", VersionNum.VERSION710);
        super.setDataHandler(columndesc, sampling);
    }

    /**
     * Get the Column entity which column name is "obs_domain_id" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "obs_domain_id"
     */
    public Column getObsDomainIdColumn() {
        ColumnDescription columndesc = new ColumnDescription(IpfixColumn.OBSDOMAINID.columnName(),
                                                             "getObsDomainIdColumn", VersionNum.VERSION710);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "obs_domain_id" to the Row
     * entity of attributes.
     * @param obsdomainid the column data which column name is "obs_domain_id"
     */
    public void setObsDomainId(Set<Long> obsdomainid) {
        ColumnDescription columndesc = new ColumnDescription(IpfixColumn.OBSDOMAINID.columnName(),
                                                             "setObsDomainId", VersionNum.VERSION710);
        super.setDataHandler(columndesc, obsdomainid);
    }

    /**
     * Get the Column entity which column name is "obs_point_id" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "obs_point_id"
     */
    public Column getObsPointIdColumn() {
        ColumnDescription columndesc = new ColumnDescription(IpfixColumn.OBSPOINTID.columnName(),
                                                             "getObsPointIdColumn", VersionNum.VERSION710);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "obs_point_id" to the Row entity
     * of attributes.
     * @param obsPointId the column data which column name is "obs_point_id"
     */
    public void setObsPointId(Set<Long> obsPointId) {
        ColumnDescription columndesc = new ColumnDescription(IpfixColumn.OBSPOINTID.columnName(),
                                                             "setObsPointId", VersionNum.VERSION710);
        super.setDataHandler(columndesc, obsPointId);
    }

    /**
     * Get the Column entity which column name is "cache_active_timeout" from
     * the Row entity of attributes.
     * @return the Column entity which column name is "cache_active_timeout"
     */
    public Column getCacheActiveTimeoutColumn() {
        ColumnDescription columndesc = new ColumnDescription(IpfixColumn.CACHEACTIVETIMEOUT.columnName(),
                                                             "getCacheActiveTimeoutColumn",
                                                             VersionNum.VERSION730);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "cache_active_timeout" to the
     * Row entity of attributes.
     * @param cacheActiveTimeout the column data which column name is
     *            "cache_active_timeout"
     */
    public void setCacheActiveTimeout(Set<Long> cacheActiveTimeout) {
        ColumnDescription columndesc = new ColumnDescription(IpfixColumn.CACHEACTIVETIMEOUT.columnName(),
                                                             "setCacheActiveTimeout", VersionNum.VERSION730);
        super.setDataHandler(columndesc, cacheActiveTimeout);
    }

    /**
     * Get the Column entity which column name is "cache_max_flows" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "cache_max_flows"
     */
    public Column getCacheMaxFlowsColumn() {
        ColumnDescription columndesc = new ColumnDescription(IpfixColumn.CACHEMAXFLOWS.columnName(),
                                                             "getCacheMaxFlowsColumn", VersionNum.VERSION730);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "cache_max_flows" to the Row
     * entity of attributes.
     * @param cacheMaxFlows the column data which column name is
     *            "cache_max_flows"
     */
    public void setCacheMaxFlows(Set<Long> cacheMaxFlows) {
        ColumnDescription columndesc = new ColumnDescription(IpfixColumn.CACHEMAXFLOWS.columnName(),
                                                             "setCacheMaxFlows", VersionNum.VERSION730);
        super.setDataHandler(columndesc, cacheMaxFlows);
    }

    /**
     * Get the Column entity which column name is "external_ids" from the Row
     * entity of attributes.
     * @return the Column entity which column name is "external_ids"
     */
    public Column getExternalIdsColumn() {
        ColumnDescription columndesc = new ColumnDescription(IpfixColumn.EXTERNALIDS.columnName(),
                                                             "getExternalIdsColumn", VersionNum.VERSION710);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "external_ids" to the Row entity
     * of attributes.
     * @param externalIds the column data which column name is "external_ids"
     */
    public void setExternalIds(Map<String, String> externalIds) {
        ColumnDescription columndesc = new ColumnDescription(IpfixColumn.EXTERNALIDS.columnName(),
                                                             "setExternalIds", VersionNum.VERSION710);
        super.setDataHandler(columndesc, externalIds);
    }
}
