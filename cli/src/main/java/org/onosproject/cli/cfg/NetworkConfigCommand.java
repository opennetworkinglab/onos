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
package org.onosproject.cli.cfg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.SubjectFactory;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Manages network configuration.
 */
@Command(scope = "onos", name = "netcfg",
        description = "Manages network configuration")
public class NetworkConfigCommand extends AbstractShellCommand {

    private static final String E_CLASSKEY_NOT_REGISTERED = " is not a registered SubjectClassKey";

    @Argument(index = 0, name = "subjectClassKey", description = "Subject class key",
            required = false, multiValued = false)
    String subjectClassKey = null;

    @Argument(index = 1, name = "subjectKey", description = "Subject key",
            required = false, multiValued = false)
    String subjectKey = null;

    @Argument(index = 2, name = "configKey", description = "Config key",
            required = false, multiValued = false)
    String configKey = null;

    @Option(name = "--remove",
            description = "Remove specified configuration tree",
            required = false)
    private boolean remove = false;

    private final ObjectMapper mapper = new ObjectMapper();
    private NetworkConfigService service;

    @Override
    protected void execute() {
        service = get(NetworkConfigService.class);
        JsonNode root = mapper.createObjectNode();
        if (isNullOrEmpty(subjectClassKey)) {
            if (remove) {
                service.removeConfig();
            }
            addAll((ObjectNode) root);
        } else {
            SubjectFactory subjectFactory = nullIsIllegal(service.getSubjectFactory(subjectClassKey),
                                subjectClassKey + E_CLASSKEY_NOT_REGISTERED);
            if (isNullOrEmpty(subjectKey)) {
                addSubjectClass((ObjectNode) root, subjectFactory);
            } else {
                Object s = subjectFactory.createSubject(subjectKey);
                if (isNullOrEmpty(configKey)) {
                    if (remove) {
                        service.removeConfig(s);
                    }
                    addSubject((ObjectNode) root, s);
                } else {
                    if (remove) {
                        service.removeConfig(subjectClassKey, s, configKey);
                    }
                    root = getSubjectConfig(getConfig(s, subjectClassKey, configKey));
                }
            }
        }

        try {
            print("%s", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error writing JSON to string", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void addAll(ObjectNode root) {
        service.getSubjectClasses()
                .forEach(sc -> {
                    SubjectFactory sf = service.getSubjectFactory(sc);
                    addSubjectClass(newObject(root, sf.subjectClassKey()), sf);
                });
    }

    @SuppressWarnings("unchecked")
    private void addSubjectClass(ObjectNode root, SubjectFactory sf) {
        service.getSubjects(sf.subjectClass())
                .forEach(s -> addSubject(newObject(root, sf.subjectKey(s)), s));
    }

    private void addSubject(ObjectNode root, Object s) {
        service.getConfigs(s).forEach(c -> root.set(c.key(), c.node()));
    }

    private JsonNode getSubjectConfig(Config config) {
        return config != null ? config.node() : null;
    }

    private Config getConfig(Object s, String subjectKey, String ck) {
        Class<? extends Config> configClass = service.getConfigClass(subjectKey, ck);
        return configClass != null ? service.getConfig(s, configClass) : null;
    }

    private ObjectNode newObject(ObjectNode parent, String key) {
        ObjectNode node = mapper.createObjectNode();
        parent.set(key, node);
        return node;
    }
}
