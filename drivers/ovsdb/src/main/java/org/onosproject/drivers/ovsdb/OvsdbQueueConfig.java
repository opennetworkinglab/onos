/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.drivers.ovsdb;

import org.onlab.packet.IpAddress;
import org.onlab.util.Bandwidth;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.DefaultQueueDescription;
import org.onosproject.net.behaviour.QueueConfigBehaviour;
import org.onosproject.net.behaviour.QueueDescription;
import org.onosproject.net.behaviour.QueueDescription.Type;
import org.onosproject.net.behaviour.QueueId;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.onosproject.ovsdb.controller.OvsdbQueue;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.ovsdb.controller.OvsdbConstant.BURST;
import static org.onosproject.ovsdb.controller.OvsdbConstant.MAX_RATE;
import static org.onosproject.ovsdb.controller.OvsdbConstant.MIN_RATE;
import static org.onosproject.ovsdb.controller.OvsdbConstant.PRIORITY;
import static org.onosproject.ovsdb.controller.OvsdbConstant.QUEUE_EXTERNAL_ID_KEY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * OVSDB-based implementation of queue config behaviour.
 */
public class OvsdbQueueConfig extends AbstractHandlerBehaviour implements QueueConfigBehaviour {

    private final Logger log = getLogger(getClass());

    @Override
    public Collection<QueueDescription> getQueues() {
        OvsdbClientService ovsdbClient = getOvsdbClient(handler());
        if (ovsdbClient == null) {
            return Collections.emptyList();
        }

        Set<OvsdbQueue> queues = ovsdbClient.getQueues();
        return queues.stream()
                .map(q -> DefaultQueueDescription.builder()
                        .queueId(QueueId.queueId(q.externalIds().get(QUEUE_EXTERNAL_ID_KEY)))
                        .type(types(q))
                        .dscp(q.dscp().isPresent() ? q.dscp().get().intValue() : null)
                        .maxRate(q.otherConfigs().get(MAX_RATE) != null ?
                                Bandwidth.bps(Long.parseLong(q.otherConfigs().get(MAX_RATE))) :
                                Bandwidth.bps(0L))
                        .minRate(q.otherConfigs().get(MIN_RATE) != null ?
                                Bandwidth.bps(Long.parseLong(q.otherConfigs().get(MIN_RATE))) :
                                Bandwidth.bps(0L))
                        .burst(q.otherConfigs().get(BURST) != null ?
                                Long.valueOf(q.otherConfigs().get(BURST)) : 0L)
                        .priority(q.otherConfigs().get(PRIORITY) != null ?
                                Long.valueOf(q.otherConfigs().get(PRIORITY)) : 0L)
                        .build())
                .collect(Collectors.toSet());
    }

    @Override
    public QueueDescription getQueue(QueueDescription queueDesc) {
        OvsdbClientService ovsdbClient = getOvsdbClient(handler());
        if (ovsdbClient == null) {
            return null;
        }

        OvsdbQueue queue = ovsdbClient.getQueue(queueDesc.queueId());
        if (queue == null) {
            return null;
        }
        return DefaultQueueDescription.builder()
                .queueId(QueueId.queueId(queue.externalIds().get(QUEUE_EXTERNAL_ID_KEY)))
                .type(types(queue))
                .dscp(queue.dscp().isPresent() ? queue.dscp().get().intValue() : null)
                .maxRate(queue.otherConfigs().get(MAX_RATE) != null ?
                        Bandwidth.bps(Long.parseLong(queue.otherConfigs().get(MAX_RATE))) :
                        Bandwidth.bps(0L))
                .minRate(queue.otherConfigs().get(MIN_RATE) != null ?
                        Bandwidth.bps(Long.parseLong(queue.otherConfigs().get(MIN_RATE))) :
                        Bandwidth.bps(0L))
                .burst(queue.otherConfigs().get(BURST) != null ?
                        Long.valueOf(queue.otherConfigs().get(BURST)) : 0L)
                .priority(queue.otherConfigs().get(PRIORITY) != null ?
                        Long.valueOf(queue.otherConfigs().get(PRIORITY)) : 0L)
                .build();
    }

    @Override
    public boolean addQueue(QueueDescription queue) {
        OvsdbClientService ovsdbClient = getOvsdbClient(handler());
        OvsdbQueue ovsdbQueue = OvsdbQueue.builder(queue).build();
        return ovsdbClient.createQueue(ovsdbQueue);
    }

    @Override
    public void deleteQueue(QueueId queueId) {
        OvsdbClientService ovsdbClient = getOvsdbClient(handler());
        ovsdbClient.dropQueue(queueId);
    }

    private EnumSet<Type> types(OvsdbQueue queue) {
        EnumSet<Type> enumSet = EnumSet.noneOf(Type.class);
        if (queue == null) {
            return enumSet;
        }

        if (queue.otherConfigs().get(MAX_RATE) != null) {
            enumSet.add(Type.MAX);
        }
        if (queue.otherConfigs().get(MIN_RATE) != null) {
            enumSet.add(Type.MIN);
        }
        if (queue.otherConfigs().get(BURST) != null) {
            enumSet.add(Type.BURST);
        }
        if (queue.otherConfigs().get(PRIORITY) != null) {
            enumSet.add(Type.PRIORITY);
        }
        return enumSet;
    }

    // OvsdbNodeId(IP) is used in the adaptor while DeviceId(ovsdb:IP)
    // is used in the core. So DeviceId need be changed to OvsdbNodeId.
    private OvsdbNodeId changeDeviceIdToNodeId(DeviceId deviceId) {
        String[] splits = deviceId.toString().split(":");
        if (splits.length < 1) {
            return null;
        }
        IpAddress ipAddress = IpAddress.valueOf(splits[1]);
        return new OvsdbNodeId(ipAddress, 0);
    }

    private OvsdbClientService getOvsdbClient(DriverHandler handler) {
        OvsdbController ovsController = handler.get(OvsdbController.class);
        OvsdbNodeId nodeId = changeDeviceIdToNodeId(handler.data().deviceId());

        return ovsController.getOvsdbClient(nodeId);
    }
}