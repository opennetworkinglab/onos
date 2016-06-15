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
package org.onosproject.net.config.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.net.config.BasicNetworkConfigService;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * Component for loading the initial network configuration.
 */
@Component(immediate = true)
public class NetworkConfigLoader {

    private static final File CFG_FILE = new File("../config/network-cfg.json");

    private final Logger log = LoggerFactory.getLogger(getClass());

    // Dependency to ensure the basic subject factories are properly initialized
    // before we start loading configs from file
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected BasicNetworkConfigService basicConfigs;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService networkConfigService;

    // FIXME: Add mutual exclusion to make sure this happens only once per startup.

    private final Map<InnerConfigPosition, JsonNode> jsons = Maps.newConcurrentMap();

    private final NetworkConfigListener configListener = new InnerConfigListener();

    private ObjectNode root;

    @Activate
    public void activate() {
        //TODO Maybe this should be at the bottom to avoid a potential race
        networkConfigService.addListener(configListener);
        try {
            if (CFG_FILE.exists()) {
                root = (ObjectNode) new ObjectMapper().readTree(CFG_FILE);

                populateConfigurations();

                if (applyConfigurations()) {
                    log.info("Loaded initial network configuration from {}", CFG_FILE);
                } else {
                    log.error("Partially loaded initial network configuration from {}", CFG_FILE);
                }
            }
        } catch (Exception e) {
            log.warn("Unable to load initial network configuration from {}", CFG_FILE, e);
        }
    }

    @Deactivate
    public void deactivate() {
        networkConfigService.removeListener(configListener);
    }
    // sweep through pending config jsons and try to add them

    /**
     * Inner class that allows for handling of newly added NetConfig types.
     */
    private final class InnerConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            //TODO should this be done for other types of NetworkConfigEvents?
            if (event.type() == NetworkConfigEvent.Type.CONFIG_REGISTERED ||
                    event.type() == NetworkConfigEvent.Type.CONFIG_ADDED) {
                applyConfigurations();
            }

        }
    }

    /**
     * Inner class that allows for tracking of JSON class configurations.
     */
    private final class InnerConfigPosition {
        private final String subjectKey, subject, configKey;

        private String subjectKey() {
            return subjectKey;
        }

        private String subject() {
            return subject;
        }

        private String configKey() {
            return configKey;
        }

        private InnerConfigPosition(String subjectKey, String subject, String configKey) {
            this.subjectKey = subjectKey;
            this.subject = subject;
            this.configKey = configKey;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof InnerConfigPosition) {
                final InnerConfigPosition that = (InnerConfigPosition) obj;
                return Objects.equals(this.subjectKey, that.subjectKey)
                        && Objects.equals(this.subject, that.subject)
                        && Objects.equals(this.configKey, that.configKey);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(subjectKey, subject, configKey);
        }
    }

    /**
     * Save the JSON leaves associated with a specific subject key.
     *
     * @param sk   the subject key string.
     * @param node the node associated with the subject key.
     */
    private void saveJson(String sk, ObjectNode node) {
        node.fieldNames().forEachRemaining(s ->
                saveSubjectJson(sk, s, (ObjectNode) node.path(s)));
    }

    /**
     * Save the JSON leaves of the tree rooted as the node 'node' with subject key 'sk'.
     *
     * @param sk   the string of the subject key.
     * @param s    the subject name.
     * @param node the node rooting this subtree.
     */
    private void saveSubjectJson(String sk,
                                 String s, ObjectNode node) {
        node.fieldNames().forEachRemaining(c ->
                this.jsons.put(new InnerConfigPosition(sk, s, c), node.path(c)));
    }

    /**
     * Iterate through the JSON and populate a list of the leaf nodes of the structure.
     */
    private void populateConfigurations() {
        root.fieldNames().forEachRemaining(sk ->
                saveJson(sk, (ObjectNode) root.path(sk)));

    }

    /**
     * Apply the configurations associated with all of the config classes that
     * are imported and have not yet been applied.
     *
     * @return false if any of the configuration parsing fails
     */
    private boolean applyConfigurations() {
        Iterator<Map.Entry<InnerConfigPosition, JsonNode>> iter = jsons.entrySet().iterator();

        Map.Entry<InnerConfigPosition, JsonNode> entry;
        InnerConfigPosition key;
        JsonNode node;
        String subjectKey;
        String subjectString;
        String configKey;
        boolean isSuccess = true;
        while (iter.hasNext()) {
            entry = iter.next();
            node = entry.getValue();
            key = entry.getKey();
            subjectKey = key.subjectKey();
            subjectString = key.subject();
            configKey = key.configKey();

            Class<? extends Config> configClass =
                    networkConfigService.getConfigClass(subjectKey, configKey);
            //Check that the config class has been imported
            if (configClass != null) {

                Object subject = networkConfigService.getSubjectFactory(subjectKey).
                        createSubject(subjectString);

                try {
                    //Apply the configuration
                    networkConfigService.applyConfig(subject, configClass, node);
                } catch (IllegalArgumentException e) {
                    log.warn("Error parsing config " + subjectKey + "/" + subject + "/" + configKey);
                    isSuccess = false;
                }

                //Now that it has been applied the corresponding JSON entry is no longer needed
                iter.remove();
            }
        }
        return isSuccess;
   }

}
