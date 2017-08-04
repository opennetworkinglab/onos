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
package org.onosproject.ovsdb.rfc.utils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.onosproject.ovsdb.rfc.message.MonitorRequest;
import org.onosproject.ovsdb.rfc.message.MonitorSelect;
import org.onosproject.ovsdb.rfc.operations.Operation;
import org.onosproject.ovsdb.rfc.schema.DatabaseSchema;
import org.onosproject.ovsdb.rfc.schema.TableSchema;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Params utility class. Params of the request object, refer to RFC7047's
 * Section 4.1.
 */
public final class ParamUtil {

    /**
     * Constructs a ParamUtil object. Utility classes should not have a public
     * or default constructor, otherwise IDE will compile unsuccessfully. This
     * class should not be instantiated.
     */
    private ParamUtil() {
    }

    /**
     * Returns MonitorRequest, refer to RFC7047's Section 4.1.5.
     * @param tableSchema entity
     * @return MonitorRequest
     */
    private static MonitorRequest getAllColumnsMonitorRequest(TableSchema tableSchema) {
        String tableName = tableSchema.name();
        Set<String> columns = tableSchema.getColumnNames();
        MonitorSelect select = new MonitorSelect(true, true, true, true);
        MonitorRequest monitorRequest = new MonitorRequest(tableName, columns, select);
        return monitorRequest;
    }

    /**
     * Returns params of monitor method, refer to RFC7047's Section 4.1.5.
     * @param monotorId json-value, refer to RFC7047's Section 4.1.5.
     * @param dbSchema DatabaseSchema entity
     * @return List of Object, the params of monitor request
     */
    public static List<Object> getMonitorParams(String monotorId, DatabaseSchema dbSchema) {
        Set<String> tables = dbSchema.getTableNames();
        Map<String, MonitorRequest> mrMap = Maps.newHashMap();
        for (String tableName : tables) {
            TableSchema tableSchema = dbSchema.getTableSchema(tableName);
            MonitorRequest monitorRequest = getAllColumnsMonitorRequest(tableSchema);
            mrMap.put(tableName, monitorRequest);
        }
        return Lists.newArrayList(dbSchema.name(), monotorId, mrMap);
    }

    /**
     * Returns params of transact method, refer to RFC7047's Section 4.1.3.
     * @param dbSchema DatabaseSchema entity
     * @param operations operation*, refer to RFC7047's Section 4.1.3.
     * @return List of Object, the params of transact request
     */
    public static List<Object> getTransactParams(DatabaseSchema dbSchema, List<Operation> operations) {
        List<Object> lists = Lists.newArrayList(dbSchema.name());
        lists.addAll(operations);
        return lists;
    }
}
