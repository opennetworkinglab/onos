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
package org.onosproject.incubator.store.meter.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Tools;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.incubator.net.meter.DefaultBand;
import org.onosproject.incubator.net.meter.DefaultMeter;
import org.onosproject.incubator.net.meter.Meter;
import org.onosproject.incubator.net.meter.MeterEvent;
import org.onosproject.incubator.net.meter.MeterFailReason;
import org.onosproject.incubator.net.meter.MeterId;
import org.onosproject.incubator.net.meter.MeterOperation;
import org.onosproject.incubator.net.meter.MeterStore;
import org.onosproject.incubator.net.meter.MeterStoreDelegate;
import org.onosproject.mastership.MastershipService;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A distributed meter store implementation. Meters are stored consistently
 * across the cluster.
 */
public class DistributedMeterStore extends AbstractStore<MeterEvent, MeterStoreDelegate>
                    implements MeterStore {

    private Logger log = getLogger(getClass());

    private static final String METERSTORE = "onos-meter-store";
    private static final int MESSAGE_HANDLER_THREAD_POOL_SIZE = 8;

    private static final MessageSubject UPDATE_METER = new MessageSubject("peer-mod-meter");


    @Property(name = "msgHandlerPoolSize", intValue = MESSAGE_HANDLER_THREAD_POOL_SIZE,
            label = "Number of threads in the message handler pool")
    private int msgPoolSize;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ClusterCommunicationService clusterCommunicationService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ClusterService clusterService;

    private ConsistentMap<MeterId, Meter> meters;
    private NodeId local;
    private KryoNamespace kryoNameSpace;

    private Serializer serializer;

    @Activate
    public void activate() {

        local = clusterService.getLocalNode().id();

        kryoNameSpace =
                KryoNamespace.newBuilder()
                                .register(DefaultMeter.class)
                                .register(DefaultBand.class)
                                .build();

        serializer = Serializer.using(kryoNameSpace);

        meters = storageService.<MeterId, Meter>consistentMapBuilder()
                    .withName(METERSTORE)
                    .withSerializer(serializer)
                    .build();

        ExecutorService executors = Executors.newFixedThreadPool(
                msgPoolSize, Tools.groupedThreads("onos/store/meter", "message-handlers"));
        registerMessageHandlers(executors);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {


        log.info("Stopped");
    }

    private void registerMessageHandlers(ExecutorService executor) {
        clusterCommunicationService.<MeterEvent>addSubscriber(UPDATE_METER, kryoNameSpace::deserialize,
                                                              this::notifyDelegate, executor);

    }


    @Override
    public void storeMeter(Meter meter) {
        NodeId master = mastershipService.getMasterFor(meter.deviceId());

        meters.put(meter.id(), meter);

        MeterEvent event = new MeterEvent(MeterEvent.Type.METER_OP_REQ,
                                          new MeterOperation(meter, MeterOperation.Type.ADD));
        if (Objects.equals(local, master)) {
            notifyDelegate(event);
        } else {
            clusterCommunicationService.unicast(
                    event,
                    UPDATE_METER,
                    serializer::encode,
                    master
            ).whenComplete((result, error) -> {
                if (error != null) {
                    log.warn("Failed to install meter {} because {} on {}",
                             meter, error, master);

                    // notify app of failure
                    meter.context().ifPresent(c -> c.onError(
                            event.subject(), MeterFailReason.UNKNOWN));
                }
            });
        }

    }

    @Override
    public void deleteMeter(Meter meter) {

        NodeId master = mastershipService.getMasterFor(meter.deviceId());

        // update the state of the meter. It will be pruned by observing
        // that it has been removed from the dataplane.
        meters.put(meter.id(), meter);

        MeterEvent event = new MeterEvent(MeterEvent.Type.METER_OP_REQ,
                                          new MeterOperation(meter, MeterOperation.Type.REMOVE));
        if (Objects.equals(local, master)) {
            notifyDelegate(event);
        } else {
            clusterCommunicationService.unicast(
                    event,
                    UPDATE_METER,
                    serializer::encode,
                    master
            ).whenComplete((result, error) -> {
                if (error != null) {
                    log.warn("Failed to delete meter {} because {} on {}",
                             meter, error, master);

                    // notify app of failure
                    meter.context().ifPresent(c -> c.onError(
                            event.subject(), MeterFailReason.UNKNOWN));
                }
            });
        }

    }

    @Override
    public void updateMeter(Meter meter) {

        NodeId master = mastershipService.getMasterFor(meter.deviceId());

        meters.put(meter.id(), meter);

        MeterEvent event = new MeterEvent(MeterEvent.Type.METER_OP_REQ,
                                          new MeterOperation(meter, MeterOperation.Type.MODIFY));
        if (Objects.equals(local, master)) {
            notifyDelegate(event);
        } else {
            clusterCommunicationService.unicast(
                    event,
                    UPDATE_METER,
                    serializer::encode,
                    master
            ).whenComplete((result, error) -> {
                if (error != null) {
                    log.warn("Failed to update meter {} because {} on {}",
                             meter, error, master);

                    // notify app of failure
                    meter.context().ifPresent(c -> c.onError(
                            event.subject(), MeterFailReason.UNKNOWN));
                }
            });
        }

    }

    @Override
    public void updateMeterState(Meter meter) {
        meters.compute(meter.id(), (id, v) -> {
            DefaultMeter m = (DefaultMeter) v;
            m.setState(meter.state());
            m.setProcessedPackets(meter.packetsSeen());
            m.setProcessedBytes(meter.bytesSeen());
            m.setLife(meter.life());
            m.setReferenceCount(meter.referenceCount());
            return m;
        });
    }

    @Override
    public Meter getMeter(MeterId meterId) {
        return meters.get(meterId).value();
    }

    @Override
    public Collection<Meter> getAllMeters() {
        return meters.values().stream()
                .map(v -> v.value()).collect(Collectors.toSet());
    }

    @Override
    public void failedMeter(MeterOperation op, MeterFailReason reason) {
        NodeId master = mastershipService.getMasterFor(op.meter().deviceId());
        meters.remove(op.meter().id());

        MeterEvent event = new MeterEvent(MeterEvent.Type.METER_OP_FAILED, op, reason);
        if (Objects.equals(local, master)) {
            notifyDelegate(event);
        } else {
            clusterCommunicationService.unicast(
                    event,
                    UPDATE_METER,
                    serializer::encode,
                    master
            ).whenComplete((result, error) -> {
                if (error != null) {
                    log.warn("Failed to delete failed meter {} because {} on {}",
                             op.meter(), error, master);

                    // Can't do any more...
                }
            });
        }

    }

}
