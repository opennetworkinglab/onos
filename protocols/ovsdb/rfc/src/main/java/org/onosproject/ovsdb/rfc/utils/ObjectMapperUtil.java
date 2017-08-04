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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * ObjectMapper utility class.
 */
public final class ObjectMapperUtil {

    private static final Logger log = LoggerFactory
            .getLogger(ObjectMapperUtil.class);

    /**
     * Constructs a ObjectMapperUtil object. Utility classes should not have a
     * public or default constructor, otherwise IDE will compile unsuccessfully. This
     * class should not be instantiated.
     */
    private ObjectMapperUtil() {
    }

    /**
     * get ObjectMapper entity.
     * @return ObjectMapper entity
     */
    public static ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                           false);
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        return objectMapper;
    }

    /**
     * get ObjectMapper entity.
     * @param flag configure
     * @return ObjectMapper entity
     */
    public static ObjectMapper getObjectMapper(boolean flag) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                           flag);
        return objectMapper;
    }

    /**
     * get ObjectMapper entity.
     * @param flag configure
     * @param incl setSerializationInclusion
     * @return ObjectMapper entity
     */
    public static ObjectMapper getObjectMapper(boolean flag, Include incl) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                           flag);
        objectMapper.setSerializationInclusion(incl);
        return objectMapper;
    }

    /**
     * convert Object into String.
     * @param obj Object
     * @return String
     */
    public static String convertToString(Object obj) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JsonProcessingException while converting Entity into string", e);
        }
        return null;
    }

}
