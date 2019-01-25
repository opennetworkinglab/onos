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
package org.onosproject.k8snetworking.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.onosproject.k8snetworking.api.Constants.PORT_NAME_PREFIX_CONTAINER;

/**
 * An utility that used in kubernetes networking app.
 */
public final class K8sNetworkingUtil {

    private static final Logger log = LoggerFactory.getLogger(K8sNetworkingUtil.class);

    private K8sNetworkingUtil() {
    }

    /**
     * Checks that whether the port is associated with container interface.
     *
     * @param portName      port name
     * @return true if the port is associated with container; false otherwise
     */
    public static boolean isContainer(String portName) {
        return PORT_NAME_PREFIX_CONTAINER.equals(portName);
    }

    /**
     * Prints out the JSON string in pretty format.
     *
     * @param mapper        Object mapper
     * @param jsonString    JSON string
     * @return pretty formatted JSON string
     */
    public static String prettyJson(ObjectMapper mapper, String jsonString) {
        try {
            Object jsonObject = mapper.readValue(jsonString, Object.class);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
        } catch (JsonParseException e) {
            log.debug("JsonParseException caused by {}", e);
        } catch (JsonMappingException e) {
            log.debug("JsonMappingException caused by {}", e);
        } catch (JsonProcessingException e) {
            log.debug("JsonProcessingException caused by {}", e);
        } catch (IOException e) {
            log.debug("IOException caused by {}", e);
        }
        return null;
    }
}
