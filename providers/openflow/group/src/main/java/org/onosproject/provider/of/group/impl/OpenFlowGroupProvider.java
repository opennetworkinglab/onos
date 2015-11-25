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

package org.onosproject.provider.of.group.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
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
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.group.DefaultGroup;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupOperation;
import org.onosproject.net.group.GroupOperations;
import org.onosproject.net.group.GroupProvider;
import org.onosproject.net.group.GroupProviderRegistry;
import org.onosproject.net.group.GroupProviderService;
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
import org.projectfloodlight.openflow.protocol.OFGroupStatsEntry;
import org.projectfloodlight.openflow.protocol.OFGroupStatsReply;
import org.projectfloodlight.openflow.protocol.OFGroupType;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsType;
import org.projectfloodlight.openflow.protocol.OFVersion;
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
        Map<OFGroupMod, OpenFlowSwitch> mods = Maps.newIdentityHashMap();
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

        if (groupStatsReply != null && groupDescStatsReply != null) {
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


        for (OFGroupDescStatsEntry entry: groupDescStatsReply.getEntries()) {
            int id = entry.getGroup().getGroupNumber();
            GroupId groupId = new DefaultGroupId(id);
            GroupDescription.Type type = getGroupType(entry.getGroupType());
            GroupBuckets buckets = new GroupBucketEntryBuilder(entry.getBuckets(),
                    entry.getGroupType()).build();
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
                                providerService.groupOperationFailed(deviceId,
                                        operation);
                                pendingGroupOperations.remove(pendingGroupId);
                                pendingXidMaps.remove(pendingGroupId);
                                log.warn("Received an group mod error {}", msg);
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
                GroupStatsCollector gsc = new GroupStatsCollector(
                        controller.getSwitch(dpid), POLL_INTERVAL);
                gsc.start();
                collectors.put(dpid, gsc);
            }

            //figure out race condition
            if (controller.getSwitch(dpid) == null) {
                switchRemoved(dpid);
            }
        }

        @Override
        public void switchRemoved(Dpid dpid) {
            GroupStatsCollector collector = collectors.remove(dpid);
            if (collector != null) {
                collector.stop();
            }
        }

        @Override
        public void switchChanged(Dpid dpid) {
        }

        @Override
        public void portChanged(Dpid dpid, OFPortStatus status) {
        }

        @Override
        public void receivedRoleReply(Dpid dpid, RoleState requested, RoleState response) {
        }
    }

}
