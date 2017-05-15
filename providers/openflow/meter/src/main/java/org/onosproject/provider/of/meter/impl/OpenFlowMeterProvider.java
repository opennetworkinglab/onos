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

package org.onosproject.provider.of.meter.impl;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.CoreService;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.DefaultBand;
import org.onosproject.net.meter.DefaultMeter;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterFailReason;
import org.onosproject.net.meter.MeterFeatures;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.meter.MeterOperation;
import org.onosproject.net.meter.MeterOperations;
import org.onosproject.net.meter.MeterProvider;
import org.onosproject.net.meter.MeterProviderRegistry;
import org.onosproject.net.meter.MeterProviderService;
import org.onosproject.net.meter.MeterState;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowController;
import org.onosproject.openflow.controller.OpenFlowEventListener;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.OpenFlowSwitchListener;
import org.onosproject.openflow.controller.RoleState;
import org.onosproject.provider.of.meter.util.MeterFeaturesBuilder;
import org.projectfloodlight.openflow.protocol.OFErrorMsg;
import org.projectfloodlight.openflow.protocol.OFErrorType;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMeterBandStats;
import org.projectfloodlight.openflow.protocol.OFMeterConfigStatsReply;
import org.projectfloodlight.openflow.protocol.OFMeterFeatures;
import org.projectfloodlight.openflow.protocol.OFMeterStats;
import org.projectfloodlight.openflow.protocol.OFMeterStatsReply;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.errormsg.OFMeterModFailedErrorMsg;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.openflow.controller.Dpid.uri;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which uses an OpenFlow controller to handle meters.
 */
@Component(immediate = true, enabled = true)
public class OpenFlowMeterProvider extends AbstractProvider implements MeterProvider {


    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenFlowController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MeterProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    private MeterProviderService providerService;

    private static final AtomicLong XID_COUNTER = new AtomicLong(1);

    static final int POLL_INTERVAL = 10;
    static final long TIMEOUT = 30;

    private Cache<Long, MeterOperation> pendingOperations;


    private InternalMeterListener listener = new InternalMeterListener();
    private Map<Dpid, MeterStatsCollector> collectors = Maps.newHashMap();

    private static final Set<Device.Type> NO_METER_SUPPORT =
            ImmutableSet.copyOf(EnumSet.of(Device.Type.ROADM,
                                           Device.Type.ROADM_OTN,
                                           Device.Type.FIBER_SWITCH,
                                           Device.Type.OTN));

    /**
     * Creates a OpenFlow meter provider.
     */
    public OpenFlowMeterProvider() {
        super(new ProviderId("of", "org.onosproject.provider.meter"));
    }

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);

        pendingOperations = CacheBuilder.newBuilder()
                .expireAfterWrite(TIMEOUT, TimeUnit.SECONDS)
                .removalListener((RemovalNotification<Long, MeterOperation> notification) -> {
                    if (notification.getCause() == RemovalCause.EXPIRED) {
                        providerService.meterOperationFailed(notification.getValue(),
                                                             MeterFailReason.TIMEOUT);
                    }
                }).build();

        controller.addEventListener(listener);
        controller.addListener(listener);

        controller.getSwitches().forEach((sw -> createStatsCollection(sw)));
    }

    @Deactivate
    public void deactivate() {
        providerRegistry.unregister(this);
        collectors.values().forEach(MeterStatsCollector::stop);
        collectors.clear();
        controller.removeEventListener(listener);
        controller.removeListener(listener);
        providerService = null;
    }

    @Override
    public void performMeterOperation(DeviceId deviceId, MeterOperations meterOps) {
        Dpid dpid = Dpid.dpid(deviceId.uri());
        OpenFlowSwitch sw = controller.getSwitch(dpid);
        if (sw == null) {
            log.error("Unknown device {}", deviceId);
            meterOps.operations().forEach(op ->
                                                  providerService.meterOperationFailed(op,
                                                                                       MeterFailReason.UNKNOWN_DEVICE)
            );
            return;
        }

        meterOps.operations().forEach(op -> performOperation(sw, op));
    }

    @Override
    public void performMeterOperation(DeviceId deviceId, MeterOperation meterOp) {
        Dpid dpid = Dpid.dpid(deviceId.uri());
        OpenFlowSwitch sw = controller.getSwitch(dpid);
        if (sw == null) {
            log.error("Unknown device {}", deviceId);
            providerService.meterOperationFailed(meterOp,
                                                 MeterFailReason.UNKNOWN_DEVICE);
            return;
        }

        performOperation(sw, meterOp);

        if (meterOp.type().equals(MeterOperation.Type.REMOVE)) {
            forceMeterStats(deviceId);
        }

    }

    private void forceMeterStats(DeviceId deviceId) {
        Dpid dpid = Dpid.dpid(deviceId.uri());
        OpenFlowSwitch sw = controller.getSwitch(dpid);

        MeterStatsCollector once = new MeterStatsCollector(sw, 1);
        once.sendMeterStatisticRequest();

    }

    private void performOperation(OpenFlowSwitch sw, MeterOperation op) {

        pendingOperations.put(op.meter().id().id(), op);


        Meter meter = op.meter();
        MeterModBuilder builder = MeterModBuilder.builder(meter.id().id(), sw.factory());
        if (meter.isBurst()) {
            builder.burst();
        }
        builder.withBands(meter.bands())
                .withId(meter.id())
                .withRateUnit(meter.unit());

        switch (op.type()) {
            case ADD:
                sw.sendMsg(builder.add());
                break;
            case REMOVE:
                sw.sendMsg(builder.remove());
                break;
            case MODIFY:
                sw.sendMsg(builder.modify());
                break;
            default:
                log.warn("Unknown Meter command {}; not sending anything",
                         op.type());
                providerService.meterOperationFailed(op,
                                                     MeterFailReason.UNKNOWN_COMMAND);
        }

    }

    private void createStatsCollection(OpenFlowSwitch sw) {
        if (sw != null && isMeterSupported(sw)) {
            MeterStatsCollector msc = new MeterStatsCollector(sw, POLL_INTERVAL);
            stopCollectorIfNeeded(collectors.put(new Dpid(sw.getId()), msc));
            msc.start();
        }
    }

    private void stopCollectorIfNeeded(MeterStatsCollector collector) {
        if (collector != null) {
            collector.stop();
        }
    }

    // TODO: ONOS-3546 Support per device enabling/disabling via network config
    private boolean isMeterSupported(OpenFlowSwitch sw) {
        if (sw.factory().getVersion() == OFVersion.OF_10 ||
                sw.factory().getVersion() == OFVersion.OF_11 ||
                sw.factory().getVersion() == OFVersion.OF_12 ||
                NO_METER_SUPPORT.contains(sw.deviceType()) ||
                sw.softwareDescription().equals("OF-DPA 2.0")) {
            return false;
        }

        return true;
    }

    private void pushMeterStats(Dpid dpid, OFStatsReply msg) {
        DeviceId deviceId = DeviceId.deviceId(Dpid.uri(dpid));

        if (msg.getStatsType() == OFStatsType.METER) {
            OFMeterStatsReply reply = (OFMeterStatsReply) msg;
            Collection<Meter> meters = buildMeters(deviceId, reply.getEntries());
            //TODO do meter accounting here.
            providerService.pushMeterMetrics(deviceId, meters);
        } else if (msg.getStatsType() == OFStatsType.METER_CONFIG) {
            OFMeterConfigStatsReply reply  = (OFMeterConfigStatsReply) msg;
            // FIXME: Map<Long, Meter> meters = collectMeters(deviceId, reply);
        }

    }

    private MeterFeatures buildMeterFeatures(Dpid dpid, OFMeterFeatures mf) {
        if (mf != null) {
            return new MeterFeaturesBuilder(mf, deviceId(uri(dpid)))
                    .build();
        } else {
            // This will usually happen for OpenFlow devices prior to 1.3
            return MeterFeaturesBuilder.noMeterFeatures(deviceId(uri(dpid)));
        }
    }

    private void pushMeterFeatures(Dpid dpid, OFMeterFeatures meterFeatures) {
        providerService.pushMeterFeatures(deviceId(uri(dpid)),
                buildMeterFeatures(dpid, meterFeatures));
    }

    private void destroyMeterFeatures(Dpid dpid) {
        providerService.deleteMeterFeatures(deviceId(uri(dpid)));
    }

    private Map<Long, Meter> collectMeters(DeviceId deviceId,
                                           OFMeterConfigStatsReply reply) {
        return Maps.newHashMap();
        //TODO: Needs a fix to be applied to loxi MeterConfig stat is incorrect
    }

    private Collection<Meter> buildMeters(DeviceId deviceId,
                                          List<OFMeterStats> entries) {
        return entries.stream().map(stat -> {
            DefaultMeter.Builder builder = DefaultMeter.builder();
            Collection<Band> bands = buildBands(stat.getBandStats());
            builder.forDevice(deviceId)
                    .withId(MeterId.meterId(stat.getMeterId()))
                    //FIXME: need to encode appId in meter id, but that makes
                    // things a little annoying for debugging
                    .fromApp(coreService.getAppId("org.onosproject.core"))
                    .withBands(bands);
            DefaultMeter meter = builder.build();
            meter.setState(MeterState.ADDED);
            meter.setLife(stat.getDurationSec());
            meter.setProcessedBytes(stat.getByteInCount().getValue());
            meter.setProcessedPackets(stat.getPacketInCount().getValue());
            meter.setReferenceCount(stat.getFlowCount());

            // marks the meter as seen on the dataplane
            pendingOperations.invalidate(stat.getMeterId());
            return meter;
        }).collect(Collectors.toSet());
    }

    private Collection<Band> buildBands(List<OFMeterBandStats> bandStats) {
        return bandStats.stream().map(stat -> {
            DefaultBand band = DefaultBand.builder().build();
            band.setBytes(stat.getByteBandCount().getValue());
            band.setPackets(stat.getPacketBandCount().getValue());
            return band;
        }).collect(Collectors.toSet());
    }

    private void signalMeterError(OFMeterModFailedErrorMsg meterError,
                                  MeterOperation op) {
        switch (meterError.getCode()) {
            case UNKNOWN:
                providerService.meterOperationFailed(op,
                                                     MeterFailReason.UNKNOWN_DEVICE);
                break;
            case METER_EXISTS:
                providerService.meterOperationFailed(op,
                                                     MeterFailReason.EXISTING_METER);
                break;
            case INVALID_METER:
                providerService.meterOperationFailed(op,
                                                     MeterFailReason.INVALID_METER);
                break;
            case UNKNOWN_METER:
                providerService.meterOperationFailed(op,
                                                     MeterFailReason.UNKNOWN);
                break;
            case BAD_COMMAND:
                providerService.meterOperationFailed(op,
                                                     MeterFailReason.UNKNOWN_COMMAND);
                break;
            case BAD_FLAGS:
                providerService.meterOperationFailed(op,
                                                     MeterFailReason.UNKNOWN_FLAGS);
                break;
            case BAD_RATE:
                providerService.meterOperationFailed(op,
                                                     MeterFailReason.BAD_RATE);
                break;
            case BAD_BURST:
                providerService.meterOperationFailed(op,
                                                     MeterFailReason.BAD_BURST);
                break;
            case BAD_BAND:
                providerService.meterOperationFailed(op,
                                                     MeterFailReason.BAD_BAND);
                break;
            case BAD_BAND_VALUE:
                providerService.meterOperationFailed(op,
                                                     MeterFailReason.BAD_BAND_VALUE);
                break;
            case OUT_OF_METERS:
                providerService.meterOperationFailed(op,
                                                     MeterFailReason.OUT_OF_METERS);
                break;
            case OUT_OF_BANDS:
                providerService.meterOperationFailed(op,
                                                     MeterFailReason.OUT_OF_BANDS);
                break;
            default:
                providerService.meterOperationFailed(op,
                                                     MeterFailReason.UNKNOWN);
        }
    }

    private class InternalMeterListener
            implements OpenFlowSwitchListener, OpenFlowEventListener {
        @Override
        public void handleMessage(Dpid dpid, OFMessage msg) {
            switch (msg.getType()) {
                case STATS_REPLY:
                    pushMeterStats(dpid, (OFStatsReply) msg);
                    break;
                case ERROR:
                    OFErrorMsg error = (OFErrorMsg) msg;
                    if (error.getErrType() == OFErrorType.METER_MOD_FAILED) {
                        MeterOperation op =
                                pendingOperations.getIfPresent(error.getXid());
                        pendingOperations.invalidate(error.getXid());
                        if (op == null) {
                            log.warn("Unknown Meter operation failed {}", error);
                        } else {
                            OFMeterModFailedErrorMsg meterError =
                                    (OFMeterModFailedErrorMsg) error;
                            signalMeterError(meterError, op);
                        }
                    }
                    break;
                default:
                    break;
            }

        }

        @Override
        public void switchAdded(Dpid dpid) {
            createStatsCollection(controller.getSwitch(dpid));
            pushMeterFeatures(dpid, controller.getSwitch(dpid).getMeterFeatures());
        }

        @Override
        public void switchRemoved(Dpid dpid) {
            stopCollectorIfNeeded(collectors.remove(dpid));
            destroyMeterFeatures(dpid);
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
