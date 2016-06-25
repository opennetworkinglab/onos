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
package org.onosproject.ovsdb.rfc.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.onosproject.ovsdb.rfc.exception.AbnormalJsonNodeException;
import org.onosproject.ovsdb.rfc.exception.UnsupportedException;
import org.onosproject.ovsdb.rfc.jsonrpc.Callback;
import org.onosproject.ovsdb.rfc.jsonrpc.JsonRpcResponse;
import org.onosproject.ovsdb.rfc.message.OperationResult;
import org.onosproject.ovsdb.rfc.message.RowUpdate;
import org.onosproject.ovsdb.rfc.message.TableUpdate;
import org.onosproject.ovsdb.rfc.message.TableUpdates;
import org.onosproject.ovsdb.rfc.message.UpdateNotification;
import org.onosproject.ovsdb.rfc.notation.Column;
import org.onosproject.ovsdb.rfc.notation.Row;
import org.onosproject.ovsdb.rfc.notation.Uuid;
import org.onosproject.ovsdb.rfc.operations.Operation;
import org.onosproject.ovsdb.rfc.schema.ColumnSchema;
import org.onosproject.ovsdb.rfc.schema.DatabaseSchema;
import org.onosproject.ovsdb.rfc.schema.TableSchema;
import org.onosproject.ovsdb.rfc.schema.type.ColumnTypeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * JsonNode utility class. convert JsonNode into Object.
 */
public final class FromJsonUtil {

    private static final Logger log = LoggerFactory.getLogger(FromJsonUtil.class);

    /**
     * Constructs a FromJsonUtil object. Utility classes should not have a
     * public or default constructor, otherwise IDE will compile unsuccessfully.
     * This class should not be instantiated.
     */
    private FromJsonUtil() {
    }

    /**
     * Verify whether the jsonNode is normal.
     * @param jsonNode JsonNode
     * @param nodeStr the node name of JsonNode
     */
    private static void validateJsonNode(JsonNode jsonNode, String nodeStr) {
        if (!jsonNode.isObject() || !jsonNode.has(nodeStr)) {
            String message = "Abnormal DatabaseSchema JsonNode, it should contain " + nodeStr
                    + " node but was not found";
            throw new AbnormalJsonNodeException(message);
        }
    }

    /**
     * convert JsonNode into DatabaseSchema.
     * @param dbName database name
     * @param dbJson the JsonNode of get_schema result
     * @return DatabaseSchema
     * @throws AbnormalJsonNodeException this is an abnormal JsonNode exception
     */
    public static DatabaseSchema jsonNodeToDbSchema(String dbName, JsonNode dbJson) {
        validateJsonNode(dbJson, "tables");
        validateJsonNode(dbJson, "version");
        String dbVersion = dbJson.get("version").asText();
        Map<String, TableSchema> tables = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> tablesIter = dbJson.get("tables").fields();
        while (tablesIter.hasNext()) {
            Map.Entry<String, JsonNode> table = tablesIter.next();
            tables.put(table.getKey(), jsonNodeToTableSchema(table.getKey(), table.getValue()));
        }
        return new DatabaseSchema(dbName, dbVersion, tables);
    }

    /**
     * convert JsonNode into TableSchema.
     * @param tableName table name
     * @param tableJson table JsonNode
     * @return TableSchema
     * @throws AbnormalJsonNodeException this is an abnormal JsonNode exception
     */
    private static TableSchema jsonNodeToTableSchema(String tableName, JsonNode tableJson) {
        validateJsonNode(tableJson, "columns");
        Map<String, ColumnSchema> columns = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> columnsIter = tableJson.get("columns").fields();
        while (columnsIter.hasNext()) {
            Map.Entry<String, JsonNode> column = columnsIter.next();
            columns.put(column.getKey(), jsonNodeToColumnSchema(column.getKey(), column.getValue()));
        }
        return new TableSchema(tableName, columns);
    }

    /**
     * convert JsonNode into ColumnSchema.
     * @param name column name
     * @param columnJson column JsonNode
     * @return ColumnSchema
     * @throws AbnormalJsonNodeException this is an abnormal JsonNode exception
     */
    private static ColumnSchema jsonNodeToColumnSchema(String name, JsonNode columnJson) {
        validateJsonNode(columnJson, "type");
        return new ColumnSchema(name, ColumnTypeFactory.getColumnTypeFromJson(columnJson
                .get("type")));
    }

    /**
     * convert JsonNode into the returnType of methods in OvsdbRPC class.
     * @param resultJsonNode the result JsonNode
     * @param methodName the method name of methods in OvsdbRPC class
     * @param objectMapper ObjectMapper entity
     * @return Object
     * @throws UnsupportedException this is an unsupported exception
     */
    private static Object convertResultType(JsonNode resultJsonNode, String methodName,
                                            ObjectMapper objectMapper) {
        switch (methodName) {
        case "getSchema":
        case "monitor":
            return resultJsonNode;
        case "echo":
        case "listDbs":
            return objectMapper.convertValue(resultJsonNode, objectMapper.getTypeFactory()
                    .constructParametricType(List.class, String.class));
        case "transact":
            return objectMapper.convertValue(resultJsonNode, objectMapper.getTypeFactory()
                    .constructParametricType(List.class, JsonNode.class));
        default:
            throw new UnsupportedException("does not support this rpc method" + methodName);
        }
    }

    /**
     * convert JsonNode into the returnType of methods in OvsdbRPC class.
     * @param jsonNode the result JsonNode
     * @param methodName the method name of methods in OvsdbRPC class
     * @return Object
     */
    public static Object jsonResultParser(JsonNode jsonNode, String methodName) {
        ObjectMapper objectMapper = ObjectMapperUtil.getObjectMapper();
        JsonNode error = jsonNode.get("error");
        if (error != null && !error.isNull()) {
            log.error("jsonRpcResponse error : {}", error.toString());
        }
        JsonNode resultJsonNode = jsonNode.get("result");
        Object result = convertResultType(resultJsonNode, methodName, objectMapper);
        return result;
    }

    /**
     * When monitor the ovsdb tables, if a table update, ovs send update
     * notification, then call callback function.
     * @param jsonNode the result JsonNode
     * @param callback the callback function
     * @throws UnsupportedException this is an unsupported exception
     */
    public static void jsonCallbackRequestParser(JsonNode jsonNode, Callback callback) {
        ObjectMapper objectMapper = ObjectMapperUtil.getObjectMapper();
        JsonNode params = jsonNode.get("params");
        Object param = null;
        String methodName = jsonNode.get("method").asText();
        switch (methodName) {
        case "update":
            param = objectMapper.convertValue(params, UpdateNotification.class);
            callback.update((UpdateNotification) param);
            break;
        default:
            throw new UnsupportedException("does not support this callback method: " + methodName);
        }
    }

    /**
     * Ovs send echo request to keep the heart, need we return echo result.
     * @param jsonNode the result JsonNode
     * @return JsonRpcResponse String
     */
    public static String getEchoRequestStr(JsonNode jsonNode) {
        ObjectMapper objectMapper = ObjectMapperUtil.getObjectMapper();
        String str = null;
        if (jsonNode.get("method").asText().equals("echo")) {
            JsonRpcResponse response = new JsonRpcResponse(jsonNode.get("id").asText());
            try {
                str = objectMapper.writeValueAsString(response);
            } catch (JsonProcessingException e) {
                log.error("JsonProcessingException while converting JsonNode into string: ", e);
            }
        }
        return str;
    }

    /**
     * Convert the List of Operation result into List of OperationResult .
     * @param input the List of JsonNode
     * @param operations the List of Operation
     * @return the List of OperationResult
     */
    public static List<OperationResult> jsonNodeToOperationResult(List<JsonNode> input,
                                                                  List<Operation> operations) {
        ObjectMapper objectMapper = ObjectMapperUtil.getObjectMapper(false);
        List<OperationResult> operationResults = new ArrayList<OperationResult>();
        for (int i = 0; i < input.size(); i++) {
            JsonNode jsonNode = input.get(i);
            Operation operation = operations.get(i);
            if (jsonNode != null && jsonNode.size() > 0) {
                if (i >= operations.size() || !operation.getOp().equals("select")) {
                    OperationResult or = objectMapper.convertValue(jsonNode, OperationResult.class);
                    operationResults.add(or);
                } else {
                    List<Row> rows = createRows(operation.getTableSchema(), jsonNode);
                    OperationResult or = new OperationResult(rows);
                    operationResults.add(or);
                }
            }
        }
        return operationResults;
    }

    /**
     * Convert Operation JsonNode into Rows.
     * @param tableSchema TableSchema entity
     * @param rowsNode JsonNode
     * @return ArrayList<Row> the List of Row
     */
    private static ArrayList<Row> createRows(TableSchema tableSchema, JsonNode rowsNode) {
        validateJsonNode(rowsNode, "rows");
        ArrayList<Row> rows = Lists.newArrayList();
        for (JsonNode rowNode : rowsNode.get("rows")) {
            rows.add(createRow(tableSchema, null, rowNode)); //FIXME null will throw exception
        }
        return rows;
    }

    /**
     * convert the params of Update Notification into TableUpdates.
     * @param updatesJson the params of Update Notification
     * @param dbSchema DatabaseSchema entity
     * @return TableUpdates
     */
    public static TableUpdates jsonNodeToTableUpdates(JsonNode updatesJson, DatabaseSchema dbSchema) {
        Map<String, TableUpdate> tableUpdateMap = Maps.newHashMap();
        Iterator<Map.Entry<String, JsonNode>> tableUpdatesItr = updatesJson.fields();
        while (tableUpdatesItr.hasNext()) {
            Map.Entry<String, JsonNode> entry = tableUpdatesItr.next();
            TableSchema tableSchema = dbSchema.getTableSchema(entry.getKey());
            TableUpdate tableUpdate = jsonNodeToTableUpdate(tableSchema, entry.getValue());
            tableUpdateMap.put(entry.getKey(), tableUpdate);
        }
        return TableUpdates.tableUpdates(tableUpdateMap);
    }

    /**
     * convert the params of Update Notification into TableUpdate.
     * @param tableSchema TableSchema entity
     * @param updateJson the table-update in params of Update Notification
     * @return TableUpdate
     */
    public static TableUpdate jsonNodeToTableUpdate(TableSchema tableSchema, JsonNode updateJson) {
        Map<Uuid, RowUpdate> rows = Maps.newHashMap();
        Iterator<Map.Entry<String, JsonNode>> tableUpdateItr = updateJson.fields();
        while (tableUpdateItr.hasNext()) {
            Map.Entry<String, JsonNode> oldNewRow = tableUpdateItr.next();
            String uuidStr = oldNewRow.getKey();
            Uuid uuid = Uuid.uuid(uuidStr);
            JsonNode newR = oldNewRow.getValue().get("new");
            JsonNode oldR = oldNewRow.getValue().get("old");
            Row newRow = newR != null ? createRow(tableSchema, uuid, newR) : null;
            Row oldRow = oldR != null ? createRow(tableSchema, uuid, oldR) : null;
            RowUpdate rowUpdate = new RowUpdate(uuid, oldRow, newRow);
            rows.put(uuid, rowUpdate);
        }
        return TableUpdate.tableUpdate(rows);
    }

    /**
     * Convert Operation JsonNode into Row.
     * @param tableSchema TableSchema entity
     * @param rowNode JsonNode
     * @return Row
     */
    private static Row createRow(TableSchema tableSchema, Uuid uuid, JsonNode rowNode) {
        if (tableSchema == null) {
            return null;
        }
        Map<String, Column> columns = Maps.newHashMap();
        Iterator<Map.Entry<String, JsonNode>> rowIter = rowNode.fields();
        while (rowIter.hasNext()) {
            Map.Entry<String, JsonNode> next = rowIter.next();
            ColumnSchema columnSchema = tableSchema.getColumnSchema(next.getKey());
            if (columnSchema != null) {
                String columnName = columnSchema.name();
                Object obj = TransValueUtil.getValueFromJson(next.getValue(), columnSchema.type());
                columns.put(columnName, new Column(columnName, obj));
            }
        }
        return new Row(tableSchema.name(), uuid, columns);
    }

}
