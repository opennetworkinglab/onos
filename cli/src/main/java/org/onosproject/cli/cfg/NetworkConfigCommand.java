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
package org.onosproject.cli.cfg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.config.Config;
import org.onosproject.incubator.net.config.NetworkConfigService;
import org.onosproject.incubator.net.config.SubjectFactory;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Manages network configuration.
 */
@Command(scope = "onos", name = "netcfg",
        description = "Manages network configuration")
public class NetworkConfigCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "subjectKey", description = "Subject key",
            required = false, multiValued = false)
    String subjectKey = null;

    @Argument(index = 1, name = "subject", description = "Subject",
            required = false, multiValued = false)
    String subject = null;

    @Argument(index = 2, name = "configKey", description = "Config key",
            required = false, multiValued = false)
    String configKey = null;

    private final ObjectMapper mapper = new ObjectMapper();
    private NetworkConfigService service;

    @Override
    protected void execute() {
        service = get(NetworkConfigService.class);
        ObjectNode root = new ObjectMapper().createObjectNode();
        if (isNullOrEmpty(subjectKey)) {
            addAll(root);
        } else {
            SubjectFactory subjectFactory = service.getSubjectFactory(subjectKey);
            if (isNullOrEmpty(subject)) {
                addSubjectClass(root, subjectFactory);
            } else {
                Object s = subjectFactory.createSubject(subject);
                if (isNullOrEmpty(configKey)) {
                    addSubject(root, s);
                } else {
                    root = getSubjectConfig(getConfig(s, subjectKey, configKey));
                }
            }
        }
        print("%s", root.toString());
    }

    @SuppressWarnings("unchecked")
    private void addAll(ObjectNode root) {
        service.getSubjectClasses()
                .forEach(sc -> {
                    SubjectFactory sf = service.getSubjectFactory((Class) sc);
                    addSubjectClass(newObject(root, sf.subjectKey()), sf);
                });
    }

    @SuppressWarnings("unchecked")
    private void addSubjectClass(ObjectNode root, SubjectFactory sf) {
        service.getSubjects(sf.subjectClass())
                .forEach(s -> addSubject(newObject(root, s.toString()), s));
    }

    private void addSubject(ObjectNode root, Object s) {
        service.getConfigs(s).forEach(c -> root.set(c.key(), c.node()));
    }

    private ObjectNode getSubjectConfig(Config config) {
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
