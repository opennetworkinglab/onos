/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.provider.te.utils;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;

import static org.slf4j.LoggerFactory.getLogger;


/**
 * Convert utility methods for IETF SB.
 */
public final class CodecTools {
    private static final Logger log = getLogger(CodecTools.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    //no instantiation
    private CodecTools() {
    }

    /**
     * Returns an object node from a InputStream type input which usually comes
     * from the HTTP response.
     *
     * @param stream stream data comes from a HTTP response
     * @return object node
     */
    public static ObjectNode toJson(InputStream stream) {
        ObjectNode response = null;
        try {
            response = (ObjectNode) MAPPER.readTree(stream);
        } catch (IOException e) {
            log.error("Parse json string failed {}", e.getMessage());
        }

        return response;
    }

    /**
     * Returns an object node from a string.
     *
     * @param jsonString string with JSON format
     * @return object node
     */
    public static ObjectNode toJson(String jsonString) {
        ObjectNode response = null;
        try {
            response = (ObjectNode) MAPPER.readTree(jsonString);
        } catch (IOException e) {
            log.error("Parse json string failed {}", e.getMessage());
        }

        return response;
    }

    /**
     * Returns a JSON format string from a Jackson object node.
     *
     * @param node JSON object node
     * @return string with JSON format
     */
    public static String jsonToString(ObjectNode node) {
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String jsonString = null;
        try {
            jsonString = ow.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            log.error("Parse json to string failed {}", e.getMessage());
        }

        return jsonString;
    }
}
