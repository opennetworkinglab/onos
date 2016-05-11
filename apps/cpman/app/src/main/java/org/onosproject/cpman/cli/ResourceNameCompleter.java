/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.cpman.cli;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.karaf.shell.console.completer.ArgumentCompleter;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.onosproject.cli.AbstractCompleter;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cluster.NodeId;
import org.onosproject.cpman.ControlPlaneMonitorService;
import org.onosproject.cpman.ControlResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;

/**
 * Resource name completer.
 */
public class ResourceNameCompleter extends AbstractCompleter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String NETWORK = "network";
    private static final String DISK = "disk";
    private static final String CONTROL_MESSAGE = "control_message";
    private final Set<String> resourceTypes = ImmutableSet.of(NETWORK, DISK, CONTROL_MESSAGE);
    private static final String INVALID_MSG = "Invalid type name";


    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        // delegate string completer
        StringsCompleter delegate = new StringsCompleter();

        // Resource type is the second argument.
        ArgumentCompleter.ArgumentList list = getArgumentList();
        String nodeId = list.getArguments()[1];
        String type = list.getArguments()[2];

        if (resourceTypes.contains(type)) {
            ControlPlaneMonitorService monitorService =
                    AbstractShellCommand.get(ControlPlaneMonitorService.class);

            Set<String> set = Sets.newHashSet();
            switch (type) {
                case NETWORK:
                    set = monitorService.availableResourcesSync(NodeId.nodeId(nodeId),
                            ControlResource.Type.NETWORK);
                    break;
                case DISK:
                    set = monitorService.availableResourcesSync(NodeId.nodeId(nodeId),
                            ControlResource.Type.DISK);
                    break;
                case CONTROL_MESSAGE:
                    set = monitorService.availableResourcesSync(NodeId.nodeId(nodeId),
                            ControlResource.Type.CONTROL_MESSAGE);
                    break;
                default:
                    log.warn(INVALID_MSG);
                    break;
            }

            SortedSet<String> strings = delegate.getStrings();

            if (!set.isEmpty()) {
                set.forEach(strings::add);
            }
        }

        return delegate.complete(buffer, cursor, candidates);
    }
}
