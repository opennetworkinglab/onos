/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.impl;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupService;
import org.onosproject.openstacknetworking.api.OpenstackGroupRuleService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.List;

import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getGroupKey;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Sets group table rules directly using GroupService.
 */
@Component(immediate = true, service = OpenstackGroupRuleService.class)
public class OpenstackGroupRuleManager implements OpenstackGroupRuleService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Activate
    protected void activate() {
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void setRule(ApplicationId appId, DeviceId deviceId, int groupId,
                        GroupDescription.Type type, List<GroupBucket> buckets,
                        boolean install) {
        Group group = groupService.getGroup(deviceId, getGroupKey(groupId));
        if (install) {
            if (group == null) {
                GroupDescription groupDesc = new DefaultGroupDescription(deviceId,
                        type, new GroupBuckets(buckets), getGroupKey(groupId), groupId, appId);
                groupService.addGroup(groupDesc);
                log.debug("Adding group table rule {}", groupId);
            }
        } else {
            if (group != null) {
                groupService.removeGroup(deviceId, getGroupKey(groupId), appId);
                log.debug("Removing group table rule {}", groupId);
            }
        }
    }

    @Override
    public boolean hasGroup(DeviceId deviceId, int groupId) {
        return groupService.getGroup(deviceId, getGroupKey(groupId)) != null;
    }

    @Override
    public void setBuckets(ApplicationId appId, DeviceId deviceId,
                           int groupId, List<GroupBucket> buckets, boolean install) {
        if (!hasGroup(deviceId, groupId)) {
            return;
        }
        if (install) {
            // we add the buckets into the group, only if the buckets do not exist
            // in the given group
            Group group = groupService.getGroup(deviceId, getGroupKey(groupId));
            if (group.buckets() != null && !group.buckets().buckets().containsAll(buckets)) {
                groupService.addBucketsToGroup(deviceId, getGroupKey(groupId),
                        new GroupBuckets(buckets), getGroupKey(groupId), appId);
                log.debug("Adding buckets for group rule {}", groupId);
            }
        } else {
            groupService.removeBucketsFromGroup(deviceId, getGroupKey(groupId),
                    new GroupBuckets(buckets), getGroupKey(groupId), appId);
            log.debug("Removing buckets for group rule {}", groupId);
        }
    }

    @Override
    public void setBuckets(ApplicationId appId, DeviceId deviceId,
                           int groupId, List<GroupBucket> buckets) {
        groupService.setBucketsForGroup(deviceId, getGroupKey(groupId),
                new GroupBuckets(buckets), getGroupKey(groupId), appId);
    }
}
