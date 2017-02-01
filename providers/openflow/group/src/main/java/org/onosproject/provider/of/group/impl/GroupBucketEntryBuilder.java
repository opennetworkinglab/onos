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
package org.onosproject.provider.of.group.impl;

import com.google.common.collect.Lists;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.provider.of.flow.util.FlowEntryBuilder;
import org.projectfloodlight.openflow.protocol.OFBucket;
import org.projectfloodlight.openflow.protocol.OFGroupType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.slf4j.Logger;

import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Builder for GroupBucketEntry.
 */
public class GroupBucketEntryBuilder {

    private Dpid dpid;
    private List<OFBucket> ofBuckets;
    private OFGroupType type;
    private DriverService driverService;

    private final Logger log = getLogger(getClass());


    /**
     * Creates a builder.
     *
     * @param dpid dpid
     * @param ofBuckets list of OFBucket
     * @param type Group type
     * @param driverService driver service
     */
    public GroupBucketEntryBuilder(Dpid dpid, List<OFBucket> ofBuckets, OFGroupType type,
                                   DriverService driverService) {
        this.dpid = dpid;
        this.ofBuckets = ofBuckets;
        this.type = type;
        this.driverService = driverService;
    }

    /**
     * Builds a GroupBuckets.
     *
     * @return GroupBuckets object, a list of GroupBuckets
     */
    public GroupBuckets build() {
        List<GroupBucket> bucketList = Lists.newArrayList();

        for (OFBucket bucket: ofBuckets) {
            TrafficTreatment treatment = buildTreatment(bucket.getActions());
            // TODO: Use GroupBucketEntry
            GroupBucket groupBucket = null;
            switch (type) {
                case INDIRECT:
                    groupBucket =
                            DefaultGroupBucket.createIndirectGroupBucket(treatment);
                    break;
                case SELECT:
                    groupBucket =
                            DefaultGroupBucket.createSelectGroupBucket(treatment, (short) bucket.getWeight());
                    break;
                case FF:
                    PortNumber port =
                            PortNumber.portNumber(bucket.getWatchPort().getPortNumber());
                    GroupId groupId =
                            new GroupId(bucket.getWatchGroup().getGroupNumber());
                    groupBucket =
                            DefaultGroupBucket.createFailoverGroupBucket(treatment,
                                    port, groupId);
                    break;
                case ALL:
                    groupBucket =
                            DefaultGroupBucket.createAllGroupBucket(treatment);
                    break;
                default:
                    log.error("Unsupported Group type : {}", type);
            }
            if (groupBucket != null) {
                bucketList.add(groupBucket);
            }
        }
        return new GroupBuckets(bucketList);
    }

    private TrafficTreatment buildTreatment(List<OFAction> actions) {
        DriverHandler driverHandler = getDriver(dpid);
        TrafficTreatment.Builder builder = DefaultTrafficTreatment.builder();

        // If this is a drop rule
        if (actions.isEmpty()) {
            builder.drop();
            return builder.build();
        }

        return FlowEntryBuilder.configureTreatmentBuilder(actions, builder,
                driverHandler, DeviceId.deviceId(Dpid.uri(dpid))).build();
    }

    private DriverHandler getDriver(Dpid dpid) {
        DeviceId deviceId = DeviceId.deviceId(Dpid.uri(dpid));
        Driver driver = driverService.getDriver(deviceId);
        DriverHandler handler = new DefaultDriverHandler(new DefaultDriverData(driver, deviceId));
        return handler;
    }
}
