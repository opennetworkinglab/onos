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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.incubator.net.config.NetworkConfigService;
import org.onosproject.incubator.net.config.SubjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

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

    // TODO: add a field to track the collection of pending JSONS

    @Activate
    public void activate() {
        // Add listener to net config events
        try {
            if (CFG_FILE.exists()) {
                ObjectNode root = (ObjectNode) new ObjectMapper().readTree(CFG_FILE);
                // Parse this JSON structure and accumulate a collection of all leaf config JSONs

                // Perform initial iteration over all leaf configs and attempt to apply them,
                // but do this only if they are valid.
//                networkConfigService.getConfigClass("foo");

                // This code can be used for building the collection of jsons
                root.fieldNames().forEachRemaining(sk ->
                       consumeJson(networkConfigService, (ObjectNode) root.path(sk),
                                   networkConfigService.getSubjectFactory(sk)));
                log.info("Loaded initial network configuration from {}", CFG_FILE);
            }
        } catch (Exception e) {
            log.warn("Unable to load initial network configuration from {}",
                     CFG_FILE, e);
        }
    }


    // TODO: add deactivate which will remove listener

    // TODO: implement event listener and as each config is registered,
    // sweep through pending config jsons and try to add them

    /**
     * Consumes configuration JSON for the specified subject factory.
     *
     * @param service        network configuration service
     * @param classNode      subject class JSON node
     * @param subjectFactory subject factory
     */
    static void consumeJson(NetworkConfigService service, ObjectNode classNode,
                            SubjectFactory subjectFactory) {
        classNode.fieldNames().forEachRemaining(s ->
                                                        consumeSubjectJson(service, (ObjectNode) classNode.path(s),
                                                                           subjectFactory.createSubject(s),
                                                                           subjectFactory.subjectKey()));
    }

    private static void consumeSubjectJson(NetworkConfigService service,
                                           ObjectNode subjectNode, Object subject, String subjectKey) {
        subjectNode.fieldNames().forEachRemaining(c ->
                                                          service.applyConfig(subject,
                                                                              service.getConfigClass(subjectKey, c),
                                                                              (ObjectNode) subjectNode.path(c)));
    }

}
