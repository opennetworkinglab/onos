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
package org.onosproject.incubator.net.config.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.incubator.net.config.NetworkConfigEvent;
import org.onosproject.incubator.net.config.NetworkConfigListener;
import org.onosproject.incubator.net.config.NetworkConfigService;
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

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService networkConfigService;

    // FIXME: Add mutual exclusion to make sure this happens only once per startup.

    private Map<InnerConfigPosition, ObjectNode> jsons = Maps.newHashMap();

    private final NetworkConfigListener configListener = new InnerConfigListener();

    ObjectNode root;

    @Activate
    public void activate() {
        //TODO Maybe this should be at the bottom to avoid a potential race
        networkConfigService.addListener(configListener);
        try {
            if (CFG_FILE.exists()) {
                root = (ObjectNode) new ObjectMapper().readTree(CFG_FILE);

                populateConfigurations();

                applyConfigurations();

                log.info("Loaded initial network configuration from {}", CFG_FILE);
            }
        } catch (Exception e) {
            log.warn("Unable to load initial network configuration from {}",
                    CFG_FILE, e);
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
        private String subjectKey, subject, classKey;

        private String getSubjectKey() {
            return subjectKey;
        }

        private String getSubject() {
            return subject;
        }

        private String getClassKey() {
            return classKey;
        }

        private InnerConfigPosition(String subjectKey, String subject, String classKey) {
            this.subjectKey = subjectKey;
            this.subject = subject;
            this.classKey = classKey;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof InnerConfigPosition) {
                final InnerConfigPosition that = (InnerConfigPosition) obj;
                return Objects.equals(this.subjectKey, that.subjectKey) && Objects.equals(this.subject, that.subject)
                        && Objects.equals(this.classKey, that.classKey);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(subjectKey, subject, classKey);
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
                this.jsons.put(new InnerConfigPosition(sk, s, c), (ObjectNode) node.path(c)));
    }

    /**
     * Iterate through the JSON and populate a list of the leaf nodes of the structure.
     */
    private void populateConfigurations() {
        root.fieldNames().forEachRemaining(sk ->
                saveJson(sk, (ObjectNode) root.path(sk)));

    }

    /**
     * Apply the configurations associated with all of the config classes that are imported and have not yet been
     * applied.
     */
    protected void applyConfigurations() {
        Iterator<Map.Entry<InnerConfigPosition, ObjectNode>> iter = jsons.entrySet().iterator();

        Map.Entry<InnerConfigPosition, ObjectNode> entry;
        InnerConfigPosition key;
        ObjectNode node;
        String subjectKey;
        String subject;
        String classKey;

        while (iter.hasNext()) {
            entry = iter.next();
            node = entry.getValue();
            key = entry.getKey();
            subjectKey = key.getSubjectKey();
            subject = key.getSubject();
            classKey = key.getClassKey();
            //Check that the config class has been imported
            if (networkConfigService.getConfigClass(subjectKey, subject) != null) {

                //Apply the configuration
                networkConfigService.applyConfig(networkConfigService.getSubjectFactory(subjectKey).
                                createSubject(subject),
                        networkConfigService.getConfigClass(subjectKey, classKey), node);

                //Now that it has been applied the corresponding JSON entry is no longer needed
                jsons.remove(key);
            }

        }
    }

}
