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
package org.onosproject.xosclient.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.onosproject.xosclient.api.VtnServiceApi;
import org.onosproject.xosclient.api.XosAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides CORD VTN service and service dependency APIs.
 */
public final class DefaultVtnServiceApi extends XosApi implements VtnServiceApi {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static DefaultVtnServiceApi instance = null;

    /**
     * Default constructor.
     */
    private DefaultVtnServiceApi(String baseUrl, XosAccess access) {
        super(baseUrl, access);
    }

    /**
     * Returns VTN service API instance. Creates a new instance only if base url or
     * access has been changed.
     *
     * @param baseUrl base url
     * @param access access
     * @return vtn service api
     */
    public static synchronized DefaultVtnServiceApi getInstance(String baseUrl, XosAccess access) {
        checkNotNull(access, "XOS access information is null");
        checkArgument(!Strings.isNullOrEmpty(baseUrl), "VTN service API base url is null or empty");

        if (instance == null ||
                !instance.baseUrl.equals(baseUrl) ||
                !instance.access.equals(access)) {
            instance = new DefaultVtnServiceApi(baseUrl, access);
        }
        return instance;
    }

    @Override
    public Set<String> services() {
        String response = restGet(EMPTY_STRING);
        log.trace("Get services {}", response);

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode nodes = mapper.readTree(response);
            return Sets.newHashSet(nodes.fieldNames());
        } catch (IOException e) {
            log.warn("Failed to get service list");
            return Sets.newHashSet();
        }
    }

    @Override
    public Set<String> getProviderServices(String tServiceId) {
        checkNotNull(tServiceId);

        String response = restGet(tServiceId);
        log.trace("Get provider services {}", response);

        ObjectMapper mapper = new ObjectMapper();
        Set<String> pServices = Sets.newHashSet();

        try {
            JsonNode nodes = mapper.readTree(response);
            nodes.forEach(node -> pServices.add(node.asText()));
        } catch (IOException e) {
            log.warn("Failed to get service dependency");
        }
        return pServices;
    }

    @Override
    public Set<String> getTenantServices(String tServiceId) {
        checkNotNull(tServiceId);

        String response = restGet(EMPTY_STRING);
        log.trace("Get tenant services {}", response);

        ObjectMapper mapper = new ObjectMapper();
        Set<String> tServices = Sets.newHashSet();
        try {
            JsonNode nodes = mapper.readTree(response);
            Iterator<Map.Entry<String, JsonNode>> iterator = nodes.fields();

            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                entry.getValue().forEach(pService -> {
                    if (pService.asText().equals(tServiceId)) {
                        tServices.add(entry.getKey());
                    }
                });
            }
        } catch (IOException e) {
            log.warn("Failed to get service list");
        }
        return tServices;
    }
}
