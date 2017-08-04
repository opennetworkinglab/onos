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

import org.onosproject.ovsdb.rfc.jsonrpc.JsonRpcRequest;
import org.onosproject.ovsdb.rfc.operations.Operation;
import org.onosproject.ovsdb.rfc.schema.DatabaseSchema;

/**
 * RPC Methods request utility class. Refer to RFC7047's Section 4.1.
 */
public final class JsonRpcWriterUtil {

    /**
     * Constructs a JsonRpcWriterUtil object. Utility classes should not have a
     * public or default constructor, otherwise IDE will compile unsuccessfully.
     * This class should not be instantiated.
     */
    private JsonRpcWriterUtil() {
    }

    /**
     * Returns string of RPC request.
     * @param uuid id of request object
     * @param methodName method of request object
     * @param params params of request object
     * @return RPC Request String
     */
    private static String getRequestStr(String uuid, String methodName,
                                        List params) {
        JsonRpcRequest request;
        if (params != null) {
            request = new JsonRpcRequest(uuid, methodName, params);
        } else {
            request = new JsonRpcRequest(uuid, methodName);
        }
        String str = ObjectMapperUtil.convertToString(request);
        return str;
    }

    /**
     * Returns string of get_schema request.
     * @param uuid id of get_schema request
     * @param dbnames params of get_schema request
     * @return get_schema Request String
     */
    public static String getSchemaStr(String uuid, List<String> dbnames) {
        String methodName = "get_schema";
        return getRequestStr(uuid, methodName, dbnames);
    }

    /**
     * Returns string of echo request.
     * @param uuid id of echo request
     * @return echo Request String
     */
    public static String echoStr(String uuid) {
        String methodName = "echo";
        return getRequestStr(uuid, methodName, null);
    }

    /**
     * Returns string of monitor request.
     * @param uuid id of monitor request
     * @param monotorId json-value in params of monitor request
     * @param dbSchema DatabaseSchema entity
     * @return monitor Request String
     */
    public static String monitorStr(String uuid, String monotorId,
                                    DatabaseSchema dbSchema) {
        String methodName = "monitor";
        return getRequestStr(uuid, methodName,
                             ParamUtil.getMonitorParams(monotorId, dbSchema));
    }

    /**
     * Returns string of list_dbs request.
     * @param uuid id of list_dbs request
     * @return list_dbs Request String
     */
    public static String listDbsStr(String uuid) {
        String methodName = "list_dbs";
        return getRequestStr(uuid, methodName, null);
    }

    /**
     * Returns string of transact request.
     * @param uuid id of transact request
     * @param dbSchema DatabaseSchema entity
     * @param operations operation* in params of transact request
     * @return transact Request String
     */
    public static String transactStr(String uuid, DatabaseSchema dbSchema,
                                     List<Operation> operations) {
        String methodName = "transact";
        return getRequestStr(uuid, methodName,
                             ParamUtil.getTransactParams(dbSchema, operations));
    }
}
