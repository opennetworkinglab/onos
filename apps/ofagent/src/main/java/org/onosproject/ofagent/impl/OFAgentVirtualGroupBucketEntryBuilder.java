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
package org.onosproject.ofagent.impl;

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
 * Group Bucket builder customized for OFAgent.
 * <p>
 * This builder is used to build GroupBucketEntry objects for virtual devices encountered by
 * OFAgent.  Its code is based on org.onosproject.provider.of.group.impl.GroupBucketEntryBuilder,
 * except that the driver has been hardcoded to "ovs".
 */
public class OFAgentVirtualGroupBucketEntryBuilder {

    private static final String DRIVER_NAME = "ovs";

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
    public OFAgentVirtualGroupBucketEntryBuilder(Dpid dpid, List<OFBucket> ofBuckets, OFGroupType type,
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

    /**
     * Retrieves the driver handler for the specified device.
     *
     * @param dpid datapath identifier
     * @return driver handler
     */
    protected DriverHandler getDriver(Dpid dpid) {
        DeviceId devId = DeviceId.deviceId(Dpid.uri(dpid));
        log.debug("running getDriver for {}", devId);
        Driver driver = driverService.getDriver(DRIVER_NAME);
        DriverHandler handler = new DefaultDriverHandler(new DefaultDriverData(driver, devId));
        return handler;
    }

}
