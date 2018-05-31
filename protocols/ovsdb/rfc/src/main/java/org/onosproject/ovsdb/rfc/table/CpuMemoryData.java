/*
 * Copyright 2018-present Open Networking Foundation
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


import org.onosproject.ovsdb.rfc.notation.Column;
import org.onosproject.ovsdb.rfc.notation.Row;
import org.onosproject.ovsdb.rfc.schema.DatabaseSchema;
import org.onosproject.ovsdb.rfc.tableservice.AbstractOvsdbTableService;
import org.onosproject.ovsdb.rfc.tableservice.ColumnDescription;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides operations of Cpu Memory Table.
 */
public class CpuMemoryData extends AbstractOvsdbTableService {

    private static final Pattern PATTERN_MEMORY = Pattern.compile(
            "Mem:\\s*(\\d+) total,\\s*(\\d+) used,\\s*(\\d+) free,\\s*(\\d+) buffers");
    private static final Pattern PATTERN_CPU = Pattern.compile(
            ":\\s*(\\d*\\.\\d+) us,\\s*(\\d*\\.\\d+) sy,\\s*(\\d*\\.\\d+) ni," +
                    "\\s*(\\d*\\.\\d+) id,\\s*(\\d*\\.\\d+) wa," +
                    "\\s*(\\d*\\.\\d+) hi,\\s*(\\d*\\.\\d+) si,\\s*(\\d*\\.\\d+) st");
    private static final long BYTES_IN_KILOBYTE = 1024L;
    /**
     * Cpu Memory Data table column name.
     */
    public enum CpuMemoryDataColumn {
        CPU("cpu"), UPTIME("uptime"),
        TASKS("tasks"), MEMORY("memory");

        private final String columnName;

        private CpuMemoryDataColumn(String columnName) {
            this.columnName = columnName;
        }

        /**
         * Returns the table column name for CpuMemoryDataColumn.
         * @return the table column name
         */
        public String columnName() {
            return columnName;
        }
    }

    /**
     * Constructs a CpuMemoryData object. Generate Cpu Memory Data Table Description.
     * @param dbSchema DatabaseSchema
     * @param row Row
     */
    public CpuMemoryData(DatabaseSchema dbSchema, Row row) {
       super(dbSchema, row, OvsdbTable.CPUMEMORYDATA, VersionNum.VERSION010, VersionNum.VERSION010);
    }

    /**
     * Get the Column entity with column name "memory" from the Row entity of
     * attributes.
     * @return the Column entity with column name "memory"
     */
    public Column getMemoryColumn() {
        ColumnDescription columnDescription = new ColumnDescription(
                CpuMemoryDataColumn.MEMORY
                        .columnName(),
                "getMemoryColumn",
                VersionNum.VERSION010);
        return super.getColumnHandler(columnDescription);
    }

    /**
     * Get the Column entity with column name "cpu" from the Row entity of
     * attributes.
     * @return the Column entity with column name "cpu"
     */
    public Column getCpuColumn() {
        ColumnDescription columnDescription = new ColumnDescription(
                CpuMemoryDataColumn.CPU
                        .columnName(),
                "getCpuColumn",
                VersionNum.VERSION010);
        return super.getColumnHandler(columnDescription);
    }

    /**
     * Get the total memory avaliable in KB.
     * @return the total memory (in KB)
     */
    public long getTotalMemoryStats() {

        String memoryStats  = getMemoryColumn().data().toString();
        Matcher matcher = PATTERN_MEMORY.matcher(memoryStats);
        if (!matcher.find()) {
            return 0;
        }
        return Long.parseLong(matcher.group(1)) * BYTES_IN_KILOBYTE;
    }
    /**
     * Get used memory stats in KB.
     * @return used memory in device (in KB)
     */
    public long getUsedMemoryStats() {

        String memoryStats  = getMemoryColumn().data().toString();
        Matcher matcher = PATTERN_MEMORY.matcher(memoryStats);
        if (!matcher.find()) {
            return 0;
        }
        return Long.parseLong(matcher.group(2)) * BYTES_IN_KILOBYTE;
    }

    /**
     * Get free memory stats in KB.
     * @return free memory in device (in KB)
     */
    public long getFreeMemoryStats() {

        String memoryStats  = getMemoryColumn().data().toString();
        Matcher matcher = PATTERN_MEMORY.matcher(memoryStats);
        if (!matcher.find()) {
            return 0;
        }
        return Long.parseLong(matcher.group(3)) * BYTES_IN_KILOBYTE;
    }

    /**
     * Get free cpu usage stats.
     * @return free cpu stats
     */
    public float getFreeCpuStats() {

        System.out.println("COLUMN RECIVED IS {}" + getCpuColumn());
        String cpuStatsStr  = getCpuColumn().data().toString();
        Matcher matcher = PATTERN_CPU.matcher(cpuStatsStr);
        if (!matcher.find()) {
            return 0;
        }
        return Float.parseFloat(matcher.group(4));
    }
}
