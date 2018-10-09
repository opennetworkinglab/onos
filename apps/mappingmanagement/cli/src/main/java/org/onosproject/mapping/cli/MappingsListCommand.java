/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.mapping.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.DeviceIdCompleter;
import org.onosproject.mapping.MappingEntry;
import org.onosproject.mapping.MappingKey;
import org.onosproject.mapping.MappingTreatment;
import org.onosproject.mapping.MappingValue;
import org.onosproject.mapping.MappingService;
import org.onosproject.mapping.MappingStore;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * A command for querying mapping information.
 */
@Service
@Command(scope = "onos", name = "mappings",
        description = "Lists mappings")
public class MappingsListCommand extends AbstractShellCommand {

    private static final String DB = "database";
    private static final String CACHE = "cache";

    private static final String SUMMARY_FORMAT = "deviceId=%s, mappingCount=%d";
    private static final String MAPPING_ID_FORMAT = "  id=%s";
    private static final String MAPPING_STATE_FORMAT = "  state=%s";
    private static final String MAPPING_KEY_FORMAT = "  key=%s";
    private static final String MAPPING_VALUE_FORMAT = "  value=";
    private static final String MAPPING_ACTION_FORMAT = "    action=%s";
    private static final String MAPPING_TREATMENTS_FORMAT = "    treatments=";
    private static final String MAPPING_TREATMENT_LONG_FORMAT =
            "      address=%s, instructions=%s";
    private static final String MAPPING_TREATMENT_SHORT_FORMAT = "      %s";
    private static final String JSON_FORMAT = "%s";

    private static final String TYPE_NOT_NULL = "Mapping store type should not be null";
    private static final String TYPE_ILLEGAL = "Mapping store type is not correct";

    @Argument(index = 0, name = "type",
            description = "Shows mappings with specified type",
            required = true, multiValued = false)
    @Completion(MappingStoreTypeCompleter.class)
    private String type = null;

    @Argument(index = 1, name = "deviceId", description = "Device identity",
            required = false, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    private String deviceId = null;

    @Option(name = "-s", aliases = "--short",
            description = "Print more succinct output for each mapping",
            required = false, multiValued = false)
    private boolean shortOutput = false;

    private MappingService mappingService =
            AbstractShellCommand.get(MappingService.class);
    private List<MappingEntry> mappings;

    @Override
    protected void doExecute() {

        MappingStore.Type typeEnum = getTypeEnum(type);

        DeviceService deviceService = get(DeviceService.class);
        Iterable<Device> devices = deviceService.getDevices();

        if (outputJson()) {
            print(JSON_FORMAT, json(typeEnum, devices));
        } else {
            if (deviceId != null) {
                mappings = newArrayList(mappingService.getMappingEntries(typeEnum,
                        DeviceId.deviceId(deviceId)));
                printMappings(DeviceId.deviceId(deviceId), mappings);

            } else {

                for (Device d : devices) {
                    mappings = newArrayList(mappingService.getMappingEntries(typeEnum, d.id()));
                    printMappings(d.id(), mappings);
                }
            }
        }
    }

    /**
     * Prints out mapping information.
     *
     * @param deviceId device identifier
     * @param mappings a collection of mapping
     */
    private void printMappings(DeviceId deviceId, List<MappingEntry> mappings) {

        print(SUMMARY_FORMAT, deviceId, mappings.size());

        for (MappingEntry m : mappings) {
            print(MAPPING_ID_FORMAT, Long.toHexString(m.id().value()));
            print(MAPPING_STATE_FORMAT, m.state().name());
            print(MAPPING_KEY_FORMAT, printMappingKey(m.key()));
            printMappingValue(m.value());
        }
    }

    /**
     * Prints out mapping key.
     *
     * @param key mapping key
     * @return string format of mapping key
     */
    private String printMappingKey(MappingKey key) {
        StringBuilder builder = new StringBuilder();

        if (key.address() != null) {
            builder.append(key.address().toString());
        }

        return builder.toString();
    }

    /**
     * Prints out mapping value.
     *
     * @param value mapping value
     * @return string format of mapping value
     */
    private void printMappingValue(MappingValue value) {

        print(MAPPING_VALUE_FORMAT);

        if (value.action() != null) {
            print(MAPPING_ACTION_FORMAT, value.action().toString());
        }

        if (!value.treatments().isEmpty()) {
            print(MAPPING_TREATMENTS_FORMAT);
            for (MappingTreatment treatment : value.treatments()) {
                printMappingTreatment(treatment);
            }
        }

    }

    /**
     * Prints out mapping treatment.
     *
     * @param treatment mapping treatment
     * @return string format of mapping treatment
     */
    private void printMappingTreatment(MappingTreatment treatment) {
        if (treatment != null) {
            if (shortOutput) {
                print(MAPPING_TREATMENT_SHORT_FORMAT, treatment.address());
            } else {
                print(MAPPING_TREATMENT_LONG_FORMAT, treatment.address(),
                        treatment.instructions());
            }
        }
    }

    /**
     * Returns corresponding type enumeration based on the given
     * string formatted type.
     *
     * @param type string formatted type
     * @return type enumeration
     */
    private MappingStore.Type getTypeEnum(String type) {

        if (type == null) {
            throw new IllegalArgumentException(TYPE_NOT_NULL);
        }

        switch (type) {
            case DB:
                return MappingStore.Type.MAP_DATABASE;
            case CACHE:
                return MappingStore.Type.MAP_CACHE;
            default:
                throw new IllegalArgumentException(TYPE_ILLEGAL);
        }
    }

    /**
     * Generates JSON object with the mappings of the given device.
     *
     * @param mapper   object mapper
     * @param device   device
     * @param mappings a collection of mappings
     * @return JSON object
     */
    private ObjectNode json(ObjectMapper mapper, Device device, List<MappingEntry> mappings) {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode array = mapper.createArrayNode();

        mappings.forEach(mapping -> array.add(jsonForEntity(mapping, MappingEntry.class)));

        result.put("device", device.id().toString())
                .put("mappingCount", mappings.size())
                .set("mappings", array);
        return result;
    }

    /**
     * Generates JSON object with the mappings of all devices.
     *
     * @param type    mapping store type
     * @param devices a collection of devices
     * @return JSON object
     */
    private JsonNode json(MappingStore.Type type, Iterable<Device> devices) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (Device device : devices) {
            result.add(json(mapper, device,
                    newArrayList(mappingService.getMappingEntries(type, device.id()))));
        }
        return result;
    }
}
