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
package org.onosproject.provider.nil;

import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import org.onlab.util.Timer;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.group.DefaultGroup;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupOperation;
import org.onosproject.net.group.GroupOperations;
import org.onosproject.net.group.GroupProvider;
import org.onosproject.net.group.GroupProviderService;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Null provider to accept any group and report them.
 */
public class NullGroupProvider extends NullProviders.AbstractNullProvider
        implements GroupProvider {

    private static final long DEFAULT_POLL_DELAY = 1;

    private final Logger log = getLogger(getClass());
    private final Map<DeviceId, Map<GroupId, Group>> groupTables = new ConcurrentHashMap<>();
    private GroupProviderService providerService;
    private Timeout timeout;

    /**
     * Start group provider.
     *
     * @param providerService group provider service
     */
    void start(GroupProviderService providerService) {
        this.providerService = providerService;
        timeout = Timer.newTimeout(new StatisticTask(), DEFAULT_POLL_DELAY, TimeUnit.SECONDS);
    }

    /**
     * Stops the group provider simulation.
     */
    void stop() {
        timeout.cancel();
    }

    public void initDevicesGroupTable(Collection<DeviceId> deviceIds) {
        // Initialize group table for the device
        deviceIds.forEach(deviceId -> groupTables.put(deviceId, new ConcurrentHashMap<>()));
        pushGroupMetrics();
    }

    @Override
    public void performGroupOperation(DeviceId deviceId, GroupOperations groupOps) {
        groupOps.operations().forEach(go -> {
            switch (go.opType()) {
                case ADD:
                    groupAdd(deviceId, go);
                    break;
                case MODIFY:
                    groupModify(deviceId, go);
                    break;
                case DELETE:
                    groupDelete(deviceId, go);
                    break;
                default:
                    log.warn("Unsupported op type: {}", go.opType());
                    break;
            }
        });
    }

    private void groupAdd(DeviceId deviceId, GroupOperation go) {
        GroupId gid = go.groupId();
        DefaultGroup group = new DefaultGroup(gid, deviceId, go.groupType(), go.buckets());
        group.setState(Group.GroupState.ADDED);
        groupTables.get(deviceId).put(gid, group);
    }

    private void groupModify(DeviceId deviceId, GroupOperation go) {
        groupTables.get(deviceId).computeIfPresent(go.groupId(), (gid, group) -> {
            DefaultGroup grp = new DefaultGroup(gid, deviceId, go.groupType(), go.buckets());
            grp.setState(Group.GroupState.ADDED);
            return grp;
        });
    }

    private void groupDelete(DeviceId deviceId, GroupOperation go) {
        groupTables.get(deviceId).remove(go.groupId());
    }

    private void pushGroupMetrics() {
        groupTables.forEach((deviceId, groups) -> {
            providerService.pushGroupMetrics(deviceId, groups.values());
        });
    }

    // Periodically reports flow rule statistics.
    private class StatisticTask implements TimerTask {
        @Override
        public void run(Timeout to) {
            pushGroupMetrics();
            timeout = to.timer().newTimeout(to.task(), DEFAULT_POLL_DELAY, TimeUnit.SECONDS);
        }
    }
}
