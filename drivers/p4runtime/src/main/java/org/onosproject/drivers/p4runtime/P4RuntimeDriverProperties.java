/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.drivers.p4runtime;

/**
 * Driver properties for P4Runtime.
 */
public final class P4RuntimeDriverProperties {

    // hide default constructor
    private P4RuntimeDriverProperties() {
    }

    // When updating an existing rule, if true, we issue a DELETE operation
    // before inserting the new one, otherwise we issue a MODIFY operation. This
    // is useful fore devices that do not support MODIFY operations for table
    // entries.
    public static final String DELETE_BEFORE_UPDATE = "tableDeleteBeforeUpdate";
    public static final boolean DEFAULT_DELETE_BEFORE_UPDATE = false;

    // If true, we avoid querying the device and return what's already known by
    // the ONOS store.
    public static final String READ_FROM_MIRROR = "tableReadFromMirror";
    public static final boolean DEFAULT_READ_FROM_MIRROR = false;

    // If true, we read counters when reading table entries (if table has
    // counters). Otherwise, we don't.
    public static final String SUPPORT_TABLE_COUNTERS = "supportTableCounters";
    public static final boolean DEFAULT_SUPPORT_TABLE_COUNTERS = true;

    // If true, assumes that the device returns table entry message populated
    // with direct counter values. If false, we issue a second P4Runtime request
    // to read the direct counter values.
    public static final String READ_COUNTERS_WITH_TABLE_ENTRIES = "tableReadCountersWithTableEntries";
    public static final boolean DEFAULT_READ_COUNTERS_WITH_TABLE_ENTRIES = true;

    // True if target supports reading and writing table entries.
    public static final String SUPPORT_DEFAULT_TABLE_ENTRY = "supportDefaultTableEntry";
    public static final boolean DEFAULT_SUPPORT_DEFAULT_TABLE_ENTRY = true;

    // If true we read table entries from all tables with a single wildcard read.
    // Otherwise, we submit a read request with wildcard read on a table basis.
    public static final String TABLE_WILCARD_READS = "tableWildcardReads";
    public static final boolean DEFAULT_TABLE_WILCARD_READS = false;
}