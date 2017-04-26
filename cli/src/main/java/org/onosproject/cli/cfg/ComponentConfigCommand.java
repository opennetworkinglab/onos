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

import java.util.Optional;
import java.util.Set;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cfg.ConfigProperty;
import org.onosproject.cli.AbstractShellCommand;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Manages component configuration.
 */
@Command(scope = "onos", name = "cfg",
        description = "Manages component configuration")
public class ComponentConfigCommand extends AbstractShellCommand {

    static final String GET = "get";
    static final String SET = "set";

    private static final String FMT = "    name=%s, type=%s, value=%s, defaultValue=%s, description=%s";
    private static final String SHORT_FMT = "    %s=%s";

    @Option(name = "-s", aliases = "--short", description = "Show short output only",
            required = false, multiValued = false)
    private boolean shortOnly = false;


    @Argument(index = 0, name = "command",
            description = "Command name (get|set)",
            required = false, multiValued = false)
    String command = null;

    @Argument(index = 1, name = "component", description = "Component name",
            required = false, multiValued = false)
    String component = null;

    @Argument(index = 2, name = "name", description = "Property name",
            required = false, multiValued = false)
    String name = null;

    @Argument(index = 3, name = "value", description = "Property value",
            required = false, multiValued = false)
    String value = null;

    ComponentConfigService service;

    @Override
    protected void execute() {
        service = get(ComponentConfigService.class);
        try {
            if (isNullOrEmpty(command)) {
                listComponents();
            } else if (command.equals(GET) && isNullOrEmpty(component)) {
                listAllComponentsProperties();
            } else if (command.equals(GET) && isNullOrEmpty(name)) {
                listComponentProperties(component);
            } else if (command.equals(GET)) {
                listComponentProperty(component, name);
            } else if (command.equals(SET) && isNullOrEmpty(value)) {
                service.unsetProperty(component, name);
            } else if (command.equals(SET)) {
                service.setProperty(component, name, value);
            } else {
                error("Illegal usage");
            }
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
        }
    }

    private void listAllComponentsProperties() {
        if (outputJson()) {
            print("%s", jsonComponentProperties());
        } else {
            service.getComponentNames().forEach(this::listComponentProperties);
        }
    }

    private JsonNode jsonProperty(ConfigProperty configProperty, ObjectMapper mapper) {
        return mapper.createObjectNode()
                .put("name", configProperty.name())
                .put("type", configProperty.type().toString().toLowerCase())
                .put("value", configProperty.value())
                .put("defaultValue", configProperty.defaultValue())
                .put("description", configProperty.description());
    }

    private JsonNode jsonComponent(String component, ObjectMapper mapper) {
        ObjectNode node = mapper.createObjectNode()
                .put("componentName", component);
        final ArrayNode propertiesJson = node.putArray("properties");
        Set<ConfigProperty> properties = service.getProperties(component);
        if (properties != null) {
            properties.forEach(configProperty -> propertiesJson.add(
                    jsonProperty(configProperty, mapper)));
        }
        return node;
    }

    private JsonNode jsonComponentProperties() {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        service.getComponentNames()
                .forEach(component -> result.add(jsonComponent(component, mapper)));

        return result;
    }

    private void listComponents() {
        if (outputJson()) {
            ArrayNode node = new ObjectMapper().createArrayNode();
            service.getComponentNames().forEach(node::add);
            print("%s", node);
        } else {
            service.getComponentNames().forEach(n -> print("%s", n));
        }
    }

    private void listComponentProperties(String component) {
        if (outputJson()) {
            print("%s", jsonComponent(component, new ObjectMapper()));
        } else {
            Set<ConfigProperty> props = service.getProperties(component);
            print("%s", component);
            if (props == null) {
                print("No properties for component " + component + " found");
            } else if (shortOnly) {
                props.forEach(p -> print(SHORT_FMT, p.name(), p.value()));
            } else {
                props.forEach(p -> print(FMT, p.name(), p.type().toString().toLowerCase(),
                        p.value(), p.defaultValue(), p.description()));
            }
        }
    }

    private void listComponentProperty(String component, String name) {
        Set<ConfigProperty> props = service.getProperties(component);

        if (props == null) {
            return;
        }
        Optional<ConfigProperty> property = props.stream()
                .filter(p -> p.name().equals(name)).findFirst();
        if (outputJson()) {
            print("%s", jsonProperty(property.get(), new ObjectMapper()));
        } else {
            if (!property.isPresent()) {
                print("Property " + name + " for component " + component + " not found");
                return;
            }
            ConfigProperty p = property.get();
            if (shortOnly) {
                print(SHORT_FMT, p.name(), p.value());
            } else {
                print(FMT, p.name(), p.type().toString().toLowerCase(), p.value(),
                        p.defaultValue(), p.description());
            }
        }
    }

}
