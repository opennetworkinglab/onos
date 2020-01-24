/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.cli.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.action.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.Group.GroupState;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupService;
import org.onosproject.utils.Comparators;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Lists all groups in the system.
 */
@Service
@Command(scope = "onos", name = "groups",
        description = "Lists all groups in the system")
public class GroupsListCommand extends AbstractShellCommand {

    public static final String ANY = "any";

    private static final String FORMAT =
            "   id=0x%s, state=%s, type=%s, bytes=%s, packets=%s, appId=%s, referenceCount=%s";
    private static final String BUCKET_FORMAT =
            "       id=0x%s, bucket=%s, bytes=%s, packets=%s, weight=%s, actions=%s";

    @Argument(index = 1, name = "uri", description = "Device ID",
            required = false, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    String uri = null;

    @Argument(index = 0, name = "state", description = "Group state",
            required = false, multiValued = false)
    @Completion(GroupStatusCompleter.class)
    String state;

    @Option(name = "-c", aliases = "--count",
            description = "Print group count only",
            required = false, multiValued = false)
    private boolean countOnly = false;

    @Option(name = "-r", aliases = "--referenced",
            description = "Print referenced groups only",
            required = false, multiValued = false)
    private boolean referencedOnly = false;

    @Option(name = "-t", aliases = "--type",
            description = "Print groups with specified type",
            required = false, multiValued = false)
    @Completion(GroupTypeCompleter.class)
    private String type = null;

    @Option(name = "-u", aliases = "--unreferenced",
            description = "Print unreferenced groups only",
            required = false, multiValued = false)
    private boolean unreferencedOnly = false;


    private JsonNode json(Map<Device, List<Group>> sortedGroups) {
        ArrayNode result = mapper().createArrayNode();

        sortedGroups.forEach((device, groups) ->
                groups.forEach(group ->
                        result.add(jsonForEntity(group, Group.class))));

        return result;
    }

    @Override
    protected void doExecute() {
        DeviceService deviceService = get(DeviceService.class);
        GroupService groupService = get(GroupService.class);
        SortedMap<Device, List<Group>> sortedGroups =
                getSortedGroups(deviceService, groupService);

        if (referencedOnly && unreferencedOnly) {
            print("Options -r and -u cannot be used at the same time");
            return;
        }

        if (outputJson()) {
            print("%s", json(sortedGroups));
        } else {
            sortedGroups.forEach((device, groups) -> printGroups(device.id(), groups));
        }
    }
    /**
     * Returns the list of devices sorted using the device ID URIs.
     *
     * @param deviceService device service
     * @param groupService group service
     * @return sorted device list
     */
    protected SortedMap<Device, List<Group>> getSortedGroups(DeviceService deviceService, GroupService groupService) {
        final GroupState groupsState = (this.state != null && !"any".equals(this.state)) ?
                GroupState.valueOf(this.state.toUpperCase()) :
                null;
        final Iterable<Device> devices = Optional.ofNullable(uri)
                .map(DeviceId::deviceId)
                .map(deviceService::getDevice)
                .map(dev -> (Iterable<Device>) Collections.singletonList(dev))
                .orElse(deviceService.getDevices());

        SortedMap<Device, List<Group>> sortedGroups = new TreeMap<>(Comparators.ELEMENT_COMPARATOR);
        for (Device d : devices) {
            Stream<Group> groupStream = Lists.newArrayList(groupService.getGroups(d.id())).stream();
            if (groupsState != null) {
                groupStream = groupStream.filter(g -> g.state().equals(groupsState));
            }
            if (referencedOnly) {
                groupStream = groupStream.filter(g -> g.referenceCount() != 0);
            }
            if (type != null && !"any".equals(type)) {
                groupStream = groupStream.filter(g ->
                        g.type().equals(GroupDescription.Type.valueOf(type.toUpperCase())));
            }
            if (unreferencedOnly) {
                groupStream = groupStream.filter(g -> g.referenceCount() == 0);
            }
            sortedGroups.put(d, groupStream.sorted(Comparators.GROUP_COMPARATOR).collect(Collectors.toList()));
        }
        return sortedGroups;
    }

    private void printGroups(DeviceId deviceId, List<Group> groups) {
        print("deviceId=%s, groupCount=%s", deviceId, groups.size());

        if (countOnly) {
            return;
        }

        for (Group group : groups) {
            print(FORMAT, Integer.toHexString(group.id().id()), group.state(), group.type(),
                  group.bytes(), group.packets(), group.appId().name(), group.referenceCount());
            int i = 0;
            for (GroupBucket bucket:group.buckets().buckets()) {
                print(BUCKET_FORMAT, Integer.toHexString(group.id().id()), ++i,
                      bucket.bytes(), bucket.packets(), bucket.weight(),
                      bucket.treatment().allInstructions());
            }
        }
    }
}
