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

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.group.DefaultGroup;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupOperation;
import org.onosproject.net.group.GroupOperation.GroupMsgErrorCode;
import org.onosproject.net.group.GroupOperations;
import org.onosproject.net.group.GroupProvider;
import org.onosproject.net.group.GroupProviderRegistry;
import org.onosproject.net.group.GroupProviderService;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.group.StoredGroupBucketEntry;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowController;
import org.onosproject.openflow.controller.OpenFlowEventListener;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.OpenFlowSwitchListener;
import org.onosproject.openflow.controller.RoleState;
import org.projectfloodlight.openflow.protocol.OFBucketCounter;
import org.projectfloodlight.openflow.protocol.OFErrorMsg;
import org.projectfloodlight.openflow.protocol.OFErrorType;
import org.projectfloodlight.openflow.protocol.OFGroupDescStatsEntry;
import org.projectfloodlight.openflow.protocol.OFGroupDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFGroupMod;
import org.projectfloodlight.openflow.protocol.OFGroupModFailedCode;
import org.projectfloodlight.openflow.protocol.OFGroupStatsEntry;
import org.projectfloodlight.openflow.protocol.OFGroupStatsReply;
import org.projectfloodlight.openflow.protocol.OFGroupType;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.errormsg.OFGroupModFailedErrorMsg;
import org.slf4j.Logger;

import com.google.common.collect.Maps;

/**
 * Provider which uses an OpenFlow controller to handle Group.
 */
@Component(immediate = true)
public class OpenFlowGroupProvider extends AbstractProvider implements GroupProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenFlowController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;

    private GroupProviderService providerService;

    static final int POLL_INTERVAL = 10;

    private final InternalGroupProvider listener = new InternalGroupProvider();

    private static final AtomicLong XID_COUNTER = new AtomicLong(1);
    private final Map<Dpid, GroupStatsCollector> collectors = Maps.newHashMap();
    private final Map<Long, OFStatsReply> groupStats = Maps.newConcurrentMap();
    private final Map<GroupId, GroupOperation> pendingGroupOperations =
            Maps.newConcurrentMap();

    /* Map<Group ID, Transaction ID> */
    private final Map<GroupId, Long> pendingXidMaps = Maps.newConcurrentMap();

    /**
     * Creates a OpenFlow group provider.
     */
    public OpenFlowGroupProvider() {
        super(new ProviderId("of", "org.onosproject.provider.group"));
    }

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        controller.addListener(listener);
        controller.addEventListener(listener);

        for (OpenFlowSwitch sw : controller.getSwitches()) {
            if (isGroupSupported(sw)) {
                GroupStatsCollector gsc = new GroupStatsCollector(sw, POLL_INTERVAL);
                gsc.start();
                collectors.put(new Dpid(sw.getId()), gsc);
            }
        }

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        providerRegistry.unregister(this);
        providerService = null;
        collectors.values().forEach(GroupStatsCollector::stop);
        collectors.clear();
        log.info("Stopped");
    }

    @Override
    public void performGroupOperation(DeviceId deviceId, GroupOperations groupOps) {
        final Dpid dpid = Dpid.dpid(deviceId.uri());
        OpenFlowSwitch sw = controller.getSwitch(dpid);
        for (GroupOperation groupOperation: groupOps.operations()) {
            if (sw == null) {
                log.error("SW {} is not found", dpid);
                return;
            }
            final Long groupModXid = XID_COUNTER.getAndIncrement();
            GroupModBuilder builder = null;
            if (driverService == null) {
                builder = GroupModBuilder.builder(groupOperation.buckets(),
                                                groupOperation.groupId(),
                                                groupOperation.groupType(),
                                                sw.factory(),
                                                Optional.of(groupModXid));
            } else {
                builder = GroupModBuilder.builder(groupOperation.buckets(),
                                                  groupOperation.groupId(),
                                                  groupOperation.groupType(),
                                                  sw.factory(),
                                                  Optional.of(groupModXid),
                                                  Optional.of(driverService));
            }
            OFGroupMod groupMod = null;
            switch (groupOperation.opType()) {
                case ADD:
                    groupMod = builder.buildGroupAdd();
                    break;
                case MODIFY:
                    groupMod = builder.buildGroupMod();
                    break;
                case DELETE:
                    groupMod = builder.buildGroupDel();
                    break;
                default:
                    log.error("Unsupported Group operation");
                    return;
            }
            sw.sendMsg(groupMod);
            GroupId groudId = new DefaultGroupId(groupMod.getGroup().getGroupNumber());
            pendingGroupOperations.put(groudId, groupOperation);
            pendingXidMaps.put(groudId, groupModXid);
        }
     }

    private void pushGroupMetrics(Dpid dpid, OFStatsReply statsReply) {
        DeviceId deviceId = DeviceId.deviceId(Dpid.uri(dpid));

        OFGroupStatsReply groupStatsReply = null;
        OFGroupDescStatsReply groupDescStatsReply = null;

        synchronized (groupStats) {
            if (statsReply.getStatsType() == OFStatsType.GROUP) {
                OFStatsReply reply = groupStats.get(statsReply.getXid() + 1);
                if (reply != null) {
                    groupStatsReply = (OFGroupStatsReply) statsReply;
                    groupDescStatsReply = (OFGroupDescStatsReply) reply;
                    groupStats.remove(statsReply.getXid() + 1);
                } else {
                    groupStats.put(statsReply.getXid(), statsReply);
                }
            } else if (statsReply.getStatsType() == OFStatsType.GROUP_DESC) {
                OFStatsReply reply = groupStats.get(statsReply.getXid() - 1);
                if (reply != null) {
                    groupStatsReply = (OFGroupStatsReply) reply;
                    groupDescStatsReply = (OFGroupDescStatsReply) statsReply;
                    groupStats.remove(statsReply.getXid() - 1);
                } else {
                    groupStats.put(statsReply.getXid(), statsReply);
                }
            }
        }

        if (providerService != null && groupStatsReply != null) {
            Collection<Group> groups = buildGroupMetrics(deviceId,
                    groupStatsReply, groupDescStatsReply);
            providerService.pushGroupMetrics(deviceId, groups);
            for (Group group: groups) {
                pendingGroupOperations.remove(group.id());
                pendingXidMaps.remove(group.id());
            }
        }
    }

    private Collection<Group> buildGroupMetrics(DeviceId deviceId,
                                   OFGroupStatsReply groupStatsReply,
                                   OFGroupDescStatsReply groupDescStatsReply) {

        Map<Integer, Group> groups = Maps.newHashMap();
        Dpid dpid = Dpid.dpid(deviceId.uri());

        for (OFGroupDescStatsEntry entry: groupDescStatsReply.getEntries()) {
            int id = entry.getGroup().getGroupNumber();
            GroupId groupId = new DefaultGroupId(id);
            GroupDescription.Type type = getGroupType(entry.getGroupType());
            GroupBuckets buckets = new GroupBucketEntryBuilder(dpid, entry.getBuckets(),
                    entry.getGroupType(), driverService).build();
            DefaultGroup group = new DefaultGroup(groupId, deviceId, type, buckets);
            groups.put(id, group);
        }

        for (OFGroupStatsEntry entry: groupStatsReply.getEntries()) {
            int groupId = entry.getGroup().getGroupNumber();
            DefaultGroup group = (DefaultGroup) groups.get(groupId);
            if (group != null) {
                group.setBytes(entry.getByteCount().getValue());
                group.setLife(entry.getDurationSec());
                group.setPackets(entry.getPacketCount().getValue());
                group.setReferenceCount(entry.getRefCount());
                int bucketIndex = 0;
                for (OFBucketCounter bucketStats:entry.getBucketStats()) {
                    ((StoredGroupBucketEntry) group.buckets().buckets()
                            .get(bucketIndex))
                            .setPackets(bucketStats
                                        .getPacketCount().getValue());
                    ((StoredGroupBucketEntry) group.buckets().buckets()
                            .get(bucketIndex))
                            .setBytes(entry.getBucketStats()
                                      .get(bucketIndex)
                                      .getByteCount().getValue());
                    bucketIndex++;
                }
            }
        }

        return groups.values();
    }

    private GroupDescription.Type getGroupType(OFGroupType type) {
        switch (type) {
            case ALL:
                return GroupDescription.Type.ALL;
            case INDIRECT:
                return GroupDescription.Type.INDIRECT;
            case SELECT:
                return GroupDescription.Type.SELECT;
            case FF:
                return GroupDescription.Type.FAILOVER;
            default:
                log.error("Unsupported OF group type : {}", type);
                break;
        }
        return null;
    }

    /**
     * Returns a transaction ID for entire group operations and increases
     * the counter by the number given.
     *
     * @param increase the amount to increase the counter by
     * @return a transaction ID
     */
    public static long getXidAndAdd(int increase) {
        return XID_COUNTER.getAndAdd(increase);
    }

    private boolean isGroupSupported(OpenFlowSwitch sw) {
        if (sw.factory().getVersion() == OFVersion.OF_10 ||
                sw.factory().getVersion() == OFVersion.OF_11 ||
                sw.factory().getVersion() == OFVersion.OF_12) {
            return false;
        }

        return true;
    }

    private class InternalGroupProvider
            implements OpenFlowSwitchListener, OpenFlowEventListener {

        @Override
        public void handleMessage(Dpid dpid, OFMessage msg) {
            switch (msg.getType()) {
                case STATS_REPLY:
                    pushGroupMetrics(dpid, (OFStatsReply) msg);
                    break;
                case ERROR:
                    OFErrorMsg errorMsg = (OFErrorMsg) msg;
                    if (errorMsg.getErrType() == OFErrorType.GROUP_MOD_FAILED) {
                        GroupId pendingGroupId = null;
                        for (Map.Entry<GroupId, Long> entry: pendingXidMaps.entrySet()) {
                            if (entry.getValue() == errorMsg.getXid()) {
                                pendingGroupId = entry.getKey();
                                break;
                            }
                        }
                        if (pendingGroupId == null) {
                            log.warn("Error for unknown group operation: {}",
                                    errorMsg.getXid());
                        } else {
                            GroupOperation operation =
                                    pendingGroupOperations.get(pendingGroupId);
                            DeviceId deviceId = DeviceId.deviceId(Dpid.uri(dpid));
                            if (operation != null) {
                                OFGroupModFailedCode code =
                                        ((OFGroupModFailedErrorMsg) errorMsg).getCode();
                                GroupMsgErrorCode failureCode =
                                        GroupMsgErrorCode.values()[(code.ordinal())];
                                GroupOperation failedOperation = GroupOperation
                                        .createFailedGroupOperation(operation, failureCode);
                                log.warn("Received a group mod error {}", msg);
                                providerService.groupOperationFailed(deviceId,
                                        failedOperation);
                                pendingGroupOperations.remove(pendingGroupId);
                                pendingXidMaps.remove(pendingGroupId);
                            } else {
                                log.error("Cannot find pending group operation with group ID: {}",
                                        pendingGroupId);
                            }
                        }
                        break;
                    }
                default:
                    break;
            }
        }

        @Override
        public void switchAdded(Dpid dpid) {
            OpenFlowSwitch sw = controller.getSwitch(dpid);
            if (sw == null) {
                return;
            }
            if (isGroupSupported(sw)) {
                GroupStatsCollector gsc = new GroupStatsCollector(sw, POLL_INTERVAL);
                gsc.start();
                stopCollectorIfNeeded(collectors.put(dpid, gsc));
            }

            //figure out race condition
            if (controller.getSwitch(dpid) == null) {
                switchRemoved(dpid);
            }
        }

        @Override
        public void switchRemoved(Dpid dpid) {
            stopCollectorIfNeeded(collectors.remove(dpid));
        }

        private void stopCollectorIfNeeded(GroupStatsCollector collector) {
            if (collector != null) {
                collector.stop();
            }
        }

        @Override
        public void switchChanged(Dpid dpid) {
        }

        @Override
        public void portChanged(Dpid dpid, OFPortStatus status) {
            providerService.notifyOfFailovers(checkFailoverGroups(dpid, status));
        }

        @Override
        public void receivedRoleReply(Dpid dpid, RoleState requested, RoleState response) {
        }
    }

    /**
     * Builds a list of failover Groups whose primary live bucket failed over
     * (i.e. bucket in use has changed).
     *
     * @param dpid    DPID of switch whose port's status changed
     * @param status  new status of port
     * @return        list of groups whose primary live bucket failed over
     */
    private List<Group> checkFailoverGroups(Dpid dpid, OFPortStatus status) {
        List<Group> groupList = new ArrayList<>();
        OFPortDesc desc = status.getDesc();
        PortNumber portNumber = PortNumber.portNumber(desc.getPortNo().getPortNumber());
        DeviceId id = DeviceId.deviceId(Dpid.uri(dpid));
        if (desc.isEnabled()) {
            return groupList;
        }
        Iterator<Group> iterator = groupService.getGroups(id).iterator();
        while (iterator.hasNext()) {
            Group group = iterator.next();
            if (group.type() == GroupDescription.Type.FAILOVER &&
                    checkFailoverGroup(group, id, portNumber)) {
                groupList.add(group);
            }
        }
    return groupList;
    }

    /**
     * Checks whether the first live port in the failover group's bucket
     * has failed over.
     *
     * @param group       failover group to be checked for failover
     * @param id          device ID of switch whose port's status changed
     * @param portNumber  port number of port that was disabled
     * @return            whether the failover group experienced failover
     */
    private boolean checkFailoverGroup(Group group, DeviceId id,
                                       PortNumber portNumber) {
        boolean portReached = false;
        boolean portEnabled = false;
        Iterator<GroupBucket> bIterator = group.buckets().buckets().iterator();
        GroupBucket bucket;
        while (bIterator.hasNext() && !portReached) {
            bucket = bIterator.next();
            if (deviceService.getPort(id, bucket.watchPort()).isEnabled()) {
                portEnabled = true;
            }
            if (bucket.watchPort().equals(portNumber)) {
                portReached = true;
            }
        }
        return portReached && !portEnabled;
    }

}
