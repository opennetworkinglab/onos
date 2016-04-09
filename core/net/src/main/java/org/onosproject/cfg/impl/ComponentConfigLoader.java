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

package org.onosproject.cfg.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.cfg.ComponentConfigService;
import org.slf4j.Logger;

import java.io.File;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Component responsible for automatically loading configuration file from
 * configuration directory.
 */
@Component(immediate = true)
public class ComponentConfigLoader {

    private static final String CFG_JSON = "../config/component-cfg.json";
    static File cfgFile = new File(CFG_JSON);

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService configService;

    private ObjectNode root;

    @Activate
    protected void activate() {
        this.loadConfigs();
        log.info("Started");
    }

    // Loads the configurations for each component from the file in
    // ../config/component-cfg.json, using the preSetProperty method.
    private void loadConfigs() {
        try {
            if (cfgFile.exists()) {
                root = (ObjectNode) new ObjectMapper().readTree(cfgFile);
                root.fieldNames().
                        forEachRemaining(component -> root.path(component).fieldNames()
                                .forEachRemaining(k -> configService
                                        .preSetProperty(component, k,
                                                        root.path(component).path(k)
                                                                .asText())));
                log.info("Loaded initial component configuration from {}", cfgFile);
            }
        } catch (Exception e) {
            log.warn("Unable to load initial component configuration from {}",
                     cfgFile, e);
        }
    }
}
