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
package org.onosproject.segmentrouting.grouphandler;

import static com.google.common.base.Preconditions.checkArgument;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onosproject.core.ApplicationId;
import org.onosproject.segmentrouting.SegmentRoutingManager;
import org.onosproject.segmentrouting.config.DeviceConfigNotFoundException;
import org.onosproject.segmentrouting.config.DeviceProperties;
import org.onosproject.segmentrouting.grouphandler.GroupBucketIdentifier.BucketOutputType;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.link.LinkService;
import org.slf4j.Logger;

/**
 * A module to create group chains based on the specified device
 * ports and label stack to be applied on each port.
 */
public class PolicyGroupHandler extends DefaultGroupHandler {

    private final Logger log = getLogger(getClass());
    private HashMap<PolicyGroupIdentifier, PolicyGroupIdentifier> dependentGroups = new HashMap<>();

    /**
     * Constructs policy group handler.
     *
     * @param deviceId device identifier
     * @param appId application identifier
     * @param config interface to retrieve the device properties
     * @param linkService link service object
     * @param flowObjService flow objective service object
     * @param srManager segment routing manager
     */
    public PolicyGroupHandler(DeviceId deviceId,
                              ApplicationId appId,
                              DeviceProperties config,
                              LinkService linkService,
                              FlowObjectiveService flowObjService,
                              SegmentRoutingManager srManager) {
        super(deviceId, appId, config, linkService, flowObjService, srManager);
    }

    /**
     * Creates policy group chain.
     *
     * @param id unique identifier associated with the policy group
     * @param params a list of policy group params
     * @return policy group identifier
     */
    public PolicyGroupIdentifier createPolicyGroupChain(String id,
                                                        List<PolicyGroupParams> params) {
        List<GroupBucketIdentifier> bucketIds = new ArrayList<>();
        for (PolicyGroupParams param: params) {
            List<PortNumber> ports = param.getPorts();
            if (ports == null) {
                log.warn("createPolicyGroupChain in sw {} with wrong "
                        + "input parameters", deviceId);
                return null;
            }

            int labelStackSize = (param.getLabelStack() != null) ?
                                      param.getLabelStack().size() : 0;

            if (labelStackSize > 1) {
                for (PortNumber sp : ports) {
                    PolicyGroupIdentifier previousGroupkey = null;
                    DeviceId neighbor = portDeviceMap.get(sp);
                    for (int idx = 0; idx < param.getLabelStack().size(); idx++) {
                        int label = param.getLabelStack().get(idx);
                        if (idx == (labelStackSize - 1)) {
                            // Innermost Group
                            GroupBucketIdentifier bucketId =
                                    new GroupBucketIdentifier(label,
                                                              previousGroupkey);
                            bucketIds.add(bucketId);
                        } else if (idx == 0) {
                            // Outermost Group
                            List<GroupBucket> outBuckets = new ArrayList<>();
                            GroupBucketIdentifier bucketId =
                                    new GroupBucketIdentifier(label, sp);
                            PolicyGroupIdentifier key = new
                                    PolicyGroupIdentifier(id,
                                                          Collections.singletonList(param),
                                                          Collections.singletonList(bucketId));
                            MacAddress neighborEthDst;
                            try {
                                neighborEthDst = deviceConfig.getDeviceMac(neighbor);
                            } catch (DeviceConfigNotFoundException e) {
                                log.warn(e.getMessage()
                                        + " Skipping createPolicyGroupChain for this label.");
                                continue;
                            }

                            TrafficTreatment.Builder tBuilder =
                                    DefaultTrafficTreatment.builder();
                            tBuilder.setOutput(sp)
                                    .setEthDst(neighborEthDst)
                                    .setEthSrc(nodeMacAddr)
                                    .pushMpls()
                                    .setMpls(MplsLabel.mplsLabel(label));
                            /*outBuckets.add(DefaultGroupBucket.
                                           createSelectGroupBucket(tBuilder.build()));
                            GroupDescription desc = new
                                    DefaultGroupDescription(deviceId,
                                                            GroupDescription.Type.INDIRECT,
                                                            new GroupBuckets(outBuckets));
                            //TODO: BoS*/
                            previousGroupkey = key;
                            //groupService.addGroup(desc);
                            //TODO: Use nextObjective APIs here
                        } else {
                            // Intermediate Groups
                            GroupBucketIdentifier bucketId =
                                    new GroupBucketIdentifier(label,
                                                              previousGroupkey);
                            PolicyGroupIdentifier key = new
                                    PolicyGroupIdentifier(id,
                                                          Collections.singletonList(param),
                                                          Collections.singletonList(bucketId));
                            // Add to group dependency list
                            dependentGroups.put(previousGroupkey, key);
                            previousGroupkey = key;
                        }
                    }
                }
            } else {
                int label = -1;
                if (labelStackSize == 1) {
                    label = param.getLabelStack().get(0);
                }
                for (PortNumber sp : ports) {
                    GroupBucketIdentifier bucketId =
                            new GroupBucketIdentifier(label, sp);
                    bucketIds.add(bucketId);
                }
            }
        }
        PolicyGroupIdentifier innermostGroupkey = null;
        if (!bucketIds.isEmpty()) {
            innermostGroupkey = new
                    PolicyGroupIdentifier(id,
                                          params,
                                          bucketIds);
            // Add to group dependency list
            boolean fullyResolved = true;
            for (GroupBucketIdentifier bucketId:bucketIds) {
                if (bucketId.type() == BucketOutputType.GROUP) {
                    dependentGroups.put(bucketId.outGroup(),
                                        innermostGroupkey);
                    fullyResolved = false;
                }
            }

            if (fullyResolved) {
                List<GroupBucket> outBuckets = new ArrayList<>();
                for (GroupBucketIdentifier bucketId : bucketIds) {
                    DeviceId neighbor = portDeviceMap.
                            get(bucketId.outPort());

                    MacAddress neighborEthDst;
                    try {
                        neighborEthDst = deviceConfig.getDeviceMac(neighbor);
                    } catch (DeviceConfigNotFoundException e) {
                        log.warn(e.getMessage()
                                + " Skipping createPolicyGroupChain for this bucketId.");
                        continue;
                    }

                    TrafficTreatment.Builder tBuilder =
                            DefaultTrafficTreatment.builder();
                    tBuilder.setOutput(bucketId.outPort())
                            .setEthDst(neighborEthDst)
                            .setEthSrc(nodeMacAddr);
                    if (bucketId.label() != NeighborSet.NO_EDGE_LABEL) {
                        tBuilder.pushMpls()
                            .setMpls(MplsLabel.mplsLabel(bucketId.label()));
                    }
                    //TODO: BoS
                    /*outBuckets.add(DefaultGroupBucket.
                                   createSelectGroupBucket(tBuilder.build()));*/
                }
                /*GroupDescription desc = new
                        DefaultGroupDescription(deviceId,
                                                GroupDescription.Type.SELECT,
                                                new GroupBuckets(outBuckets));
                groupService.addGroup(desc);*/
                //TODO: Use nextObjective APIs here
            }
        }
        return innermostGroupkey;
    }

    //TODO: Use nextObjective APIs to handle the group chains
    /*
    @Override
    protected void handleGroupEvent(GroupEvent event) {}
    */

    /**
     * Generates policy group key.
     *
     * @param id unique identifier associated with the policy group
     * @param params a list of policy group params
     * @return policy group identifier
     */
    public PolicyGroupIdentifier generatePolicyGroupKey(String id,
                                   List<PolicyGroupParams> params) {
        List<GroupBucketIdentifier> bucketIds = new ArrayList<>();
        for (PolicyGroupParams param: params) {
            List<PortNumber> ports = param.getPorts();
            if (ports == null) {
                log.warn("generateGroupKey in sw {} with wrong "
                        + "input parameters", deviceId);
                return null;
            }

            int labelStackSize = (param.getLabelStack() != null)
                    ? param.getLabelStack().size() : 0;

            if (labelStackSize > 1) {
                for (PortNumber sp : ports) {
                    PolicyGroupIdentifier previousGroupkey = null;
                    for (int idx = 0; idx < param.getLabelStack().size(); idx++) {
                        int label = param.getLabelStack().get(idx);
                        if (idx == (labelStackSize - 1)) {
                            // Innermost Group
                            GroupBucketIdentifier bucketId =
                                    new GroupBucketIdentifier(label,
                                                              previousGroupkey);
                            bucketIds.add(bucketId);
                        } else if (idx == 0) {
                            // Outermost Group
                            GroupBucketIdentifier bucketId =
                                    new GroupBucketIdentifier(label, sp);
                            PolicyGroupIdentifier key = new
                                    PolicyGroupIdentifier(id,
                                                          Collections.singletonList(param),
                                                          Collections.singletonList(bucketId));
                            previousGroupkey = key;
                        } else {
                            // Intermediate Groups
                            GroupBucketIdentifier bucketId =
                                    new GroupBucketIdentifier(label,
                                                              previousGroupkey);
                            PolicyGroupIdentifier key = new
                                    PolicyGroupIdentifier(id,
                                                          Collections.singletonList(param),
                                                          Collections.singletonList(bucketId));
                            previousGroupkey = key;
                        }
                    }
                }
            } else {
                int label = -1;
                if (labelStackSize == 1) {
                    label = param.getLabelStack().get(0);
                }
                for (PortNumber sp : ports) {
                    GroupBucketIdentifier bucketId =
                            new GroupBucketIdentifier(label, sp);
                    bucketIds.add(bucketId);
                }
            }
        }
        PolicyGroupIdentifier innermostGroupkey = null;
        if (!bucketIds.isEmpty()) {
            innermostGroupkey = new
                    PolicyGroupIdentifier(id,
                                          params,
                                          bucketIds);
        }
        return innermostGroupkey;
    }

    /**
     * Removes policy group chain.
     *
     * @param key policy group identifier
     */
    public void removeGroupChain(PolicyGroupIdentifier key) {
        checkArgument(key != null);
        List<PolicyGroupIdentifier> groupsToBeDeleted = new ArrayList<>();
        groupsToBeDeleted.add(key);

        Iterator<PolicyGroupIdentifier> it =
                groupsToBeDeleted.iterator();

        while (it.hasNext()) {
            PolicyGroupIdentifier innerMostGroupKey = it.next();
            for (GroupBucketIdentifier bucketId:
                        innerMostGroupKey.bucketIds()) {
                if (bucketId.type() != BucketOutputType.GROUP) {
                    groupsToBeDeleted.add(bucketId.outGroup());
                }
            }
            /*groupService.removeGroup(deviceId,
                                     getGroupKey(innerMostGroupKey),
                                     appId);*/
            //TODO: Use nextObjective APIs here
            it.remove();
        }
    }

}
