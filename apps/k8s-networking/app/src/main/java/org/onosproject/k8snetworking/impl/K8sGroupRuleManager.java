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
package org.onosproject.k8snetworking.impl;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.k8snetworking.api.K8sGroupRuleService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupDescription.Type;
import org.onosproject.net.group.GroupService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.List;

import static org.onosproject.k8snetworking.api.Constants.K8S_NETWORKING_APP_ID;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.getGroupKey;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Sets group table rules directly using GroupService.
 */
@Component(immediate = true, service = K8sGroupRuleService.class)
public class K8sGroupRuleManager implements K8sGroupRuleService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(K8S_NETWORKING_APP_ID);
        coreService.registerApplication(K8S_NETWORKING_APP_ID);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void setRule(ApplicationId appId, DeviceId deviceId, int groupId,
                        Type type, List<GroupBucket> buckets, boolean install) {

        if (install) {
            GroupDescription groupDesc = new DefaultGroupDescription(deviceId,
                    type, new GroupBuckets(buckets), getGroupKey(groupId), groupId, appId);
            groupService.addGroup(groupDesc);
        } else {
            groupService.removeGroup(deviceId, getGroupKey(groupId), appId);
        }
    }

    @Override
    public boolean hasGroup(DeviceId deviceId, int groupId) {
        return groupService.getGroup(deviceId, getGroupKey(groupId)) != null;
    }

    @Override
    public void setBuckets(ApplicationId appId, DeviceId deviceId, int groupId,
                           List<GroupBucket> buckets, boolean install) {

        if (install) {
            groupService.addBucketsToGroup(deviceId, getGroupKey(groupId),
                    new GroupBuckets(buckets), getGroupKey(groupId), appId);
        } else {
            groupService.removeBucketsFromGroup(deviceId, getGroupKey(groupId),
                    new GroupBuckets(buckets), getGroupKey(groupId), appId);
        }
    }

    @Override
    public void setBuckets(ApplicationId appId, DeviceId deviceId, int groupId,
                           List<GroupBucket> buckets) {
        groupService.setBucketsForGroup(deviceId, getGroupKey(groupId),
                new GroupBuckets(buckets), getGroupKey(groupId), appId);
    }
}
