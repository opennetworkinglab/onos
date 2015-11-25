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
package org.onosproject.cli.net;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.Comparators;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.Group.GroupState;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Lists all groups in the system.
 */
@Command(scope = "onos", name = "groups",
        description = "Lists all groups in the system")
public class GroupsListCommand extends AbstractShellCommand {

    private static final String FORMAT =
            "   id=0x%s, state=%s, type=%s, bytes=%s, packets=%s, appId=%s";
    private static final String BUCKET_FORMAT =
            "   id=0x%s, bucket=%s, bytes=%s, packets=%s, actions=%s";

    @Argument(index = 1, name = "uri", description = "Device ID",
            required = false, multiValued = false)
    String uri = null;

    @Argument(index = 0, name = "state", description = "Group state",
            required = false, multiValued = false)
    String state;

    private JsonNode json(Map<Device, List<Group>> sortedGroups) {
        ArrayNode result = mapper().createArrayNode();

        sortedGroups.forEach((device, groups) ->
                groups.forEach(group ->
                        result.add(jsonForEntity(group, Group.class))));

        return result;
    }

    @Override
    protected void execute() {
        DeviceService deviceService = get(DeviceService.class);
        GroupService groupService = get(GroupService.class);
        SortedMap<Device, List<Group>> sortedGroups =
                getSortedGroups(deviceService, groupService);

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
    protected SortedMap<Device, List<Group>>
        getSortedGroups(DeviceService deviceService,
                        GroupService groupService) {
        SortedMap<Device, List<Group>> sortedGroups =
                new TreeMap<>(Comparators.ELEMENT_COMPARATOR);
        List<Group> groups;
        GroupState s = null;
        if (state != null && !state.equals("any")) {
            s = GroupState.valueOf(state.toUpperCase());
        }
        Iterable<Device> devices = (uri == null) ? deviceService.getDevices() :
                Collections.singletonList(deviceService.getDevice(DeviceId.deviceId(uri)));
        for (Device d : devices) {
            if (s == null) {
                groups = newArrayList(groupService.getGroups(d.id()));
            } else {
                groups = newArrayList();
                for (Group g : groupService.getGroups(d.id())) {
                    if (g.state().equals(s)) {
                        groups.add(g);
                    }
                }
            }
            groups.sort(Comparators.GROUP_COMPARATOR);
            sortedGroups.put(d, groups);
        }
        return sortedGroups;
    }

    private void printGroups(DeviceId deviceId, List<Group> groups) {
        print("deviceId=%s, groupCount=%s", deviceId, groups.size());
        for (Group group : groups) {
            print(FORMAT, Integer.toHexString(group.id().id()), group.state(), group.type(),
                  group.bytes(), group.packets(), group.appId().name());
            int i = 0;
            for (GroupBucket bucket:group.buckets().buckets()) {
                print(BUCKET_FORMAT, Integer.toHexString(group.id().id()), ++i,
                      bucket.bytes(), bucket.packets(),
                      bucket.treatment().allInstructions());
            }
        }
    }
}
