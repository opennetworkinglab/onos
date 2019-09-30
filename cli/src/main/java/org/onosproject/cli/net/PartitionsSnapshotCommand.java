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

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cluster.PartitionId;
import org.onosproject.store.primitives.PartitionAdminService;

/**
 * Command to force a snapshot of the partitions.
 */
@Service
@Command(scope = "onos", name = "snapshot-partitions",
    description = "Force snapshot partitions")
public class PartitionsSnapshotCommand extends AbstractShellCommand {

    @Option(name = "-p", aliases = "--partition",
        description = "The partition to snapshot",
        required = false, multiValued = false)
    private Integer partitionId;

    @Override
    protected void doExecute() {
        PartitionAdminService partitionAdminService = get(PartitionAdminService.class);
        if (partitionId != null) {
            partitionAdminService.snapshot(PartitionId.from(partitionId));
        } else {
            partitionAdminService.snapshot();
        }
    }
}
