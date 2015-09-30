/*
 * Copyright 2015 Open Networking Laboratory
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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.util.SharedExecutors;
import org.onosproject.cfg.ComponentConfigService;
import org.slf4j.Logger;

import java.io.File;
import java.util.Set;
import java.util.TimerTask;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Component responsible for automatically loading configuration file from
 * configuration directory.
 */
@Component(immediate = true)
public class ComponentConfigLoader {

    private static final int RETRY_DELAY = 5_000; // millis between retries
    private static final String CFG_JSON = "../config/component-cfg.json";

    static File cfgFile = new File(CFG_JSON);

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService configService;

    private ObjectNode root;
    private final Set<String> pendingComponents = Sets.newHashSet();


    /* TimerTask object that calls the load configuration for each component in the
    pending components set and cancels itself if the set is mpty.
    */
    private final TimerTask loader = new TimerTask() {
        @Override
        public void run() {
            ImmutableSet.copyOf(pendingComponents)
                    .forEach(k -> loadConfig(k, (ObjectNode) root.path(k)));
            if (pendingComponents.isEmpty()) {
                this.cancel();
            }
        }
    };

    @Activate
    protected void activate() {
        this.loadConfigs();
        log.info("Started");
    }
    /* loads the configurations for each component from the file in
    ../config/component-cfg.json, adds them to a set and schedules a task to try
    and load them.
    */
    private void loadConfigs() {
        try {
            if (cfgFile.exists()) {
                root = (ObjectNode) new ObjectMapper().readTree(cfgFile);
                root.fieldNames().forEachRemaining(pendingComponents::add);
                SharedExecutors.getTimer().schedule(loader, 0, RETRY_DELAY);
                log.info("Loaded initial component configuration from {}", cfgFile);
            }
        } catch (Exception e) {
            log.warn("Unable to load initial component configuration from {}",
                     cfgFile, e);
        }
    }
    /*
    * loads a configuration for a single component and removes it from the
    * components set
    */
    private void loadConfig(String component, ObjectNode config) {
        if (configService.getComponentNames().contains(component)) {
            config.fieldNames()
                    .forEachRemaining(k -> configService.setProperty(component, k,
                                                                     config.path(k).asText()));
            pendingComponents.remove(component);
        }
    }
}
