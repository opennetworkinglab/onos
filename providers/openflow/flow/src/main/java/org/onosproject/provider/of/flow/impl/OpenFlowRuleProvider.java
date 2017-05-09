/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.provider.of.flow.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.DefaultTableStatisticsEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleBatchEntry;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.FlowRuleExtPayLoad;
import org.onosproject.net.flow.FlowRuleProvider;
import org.onosproject.net.flow.FlowRuleProviderRegistry;
import org.onosproject.net.flow.FlowRuleProviderService;
import org.onosproject.net.flow.TableStatisticsEntry;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.statistic.DefaultLoad;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowController;
import org.onosproject.openflow.controller.OpenFlowEventListener;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.OpenFlowSwitchListener;
import org.onosproject.openflow.controller.RoleState;
import org.onosproject.openflow.controller.ThirdPartyMessage;
import org.onosproject.provider.of.flow.util.FlowEntryBuilder;
import org.osgi.service.component.ComponentContext;
import org.projectfloodlight.openflow.protocol.OFBadRequestCode;
import org.projectfloodlight.openflow.protocol.OFBarrierRequest;
import org.projectfloodlight.openflow.protocol.OFErrorMsg;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowRemoved;
import org.projectfloodlight.openflow.protocol.OFFlowStatsReply;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsType;
import org.projectfloodlight.openflow.protocol.OFTableStatsEntry;
import org.projectfloodlight.openflow.protocol.OFTableStatsReply;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.errormsg.OFBadActionErrorMsg;
import org.projectfloodlight.openflow.protocol.errormsg.OFBadInstructionErrorMsg;
import org.projectfloodlight.openflow.protocol.errormsg.OFBadMatchErrorMsg;
import org.projectfloodlight.openflow.protocol.errormsg.OFBadRequestErrorMsg;
import org.projectfloodlight.openflow.protocol.errormsg.OFFlowModFailedErrorMsg;
import org.projectfloodlight.openflow.types.U16;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which uses an OpenFlow controller to detect network end-station
 * hosts.
 */
@Component(immediate = true)
public class OpenFlowRuleProvider extends AbstractProvider
        implements FlowRuleProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenFlowController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    private static final int DEFAULT_POLL_FREQUENCY = 5;
    private static final int MIN_EXPECTED_BYTE_LEN = 56;
    private static final int SKIP_BYTES = 4;
    private static final boolean DEFAULT_ADAPTIVE_FLOW_SAMPLING = false;

    @Property(name = "flowPollFrequency", intValue = DEFAULT_POLL_FREQUENCY,
            label = "Frequency (in seconds) for polling flow statistics")
    private int flowPollFrequency = DEFAULT_POLL_FREQUENCY;

    @Property(name = "adaptiveFlowSampling", boolValue = DEFAULT_ADAPTIVE_FLOW_SAMPLING,
            label = "Adaptive Flow Sampling is on or off")
    private boolean adaptiveFlowSampling = DEFAULT_ADAPTIVE_FLOW_SAMPLING;

    private FlowRuleProviderService providerService;

    private final InternalFlowProvider listener = new InternalFlowProvider();

    private Cache<Long, InternalCacheEntry> pendingBatches;

    private final Timer timer = new Timer("onos-openflow-collector");

    // Old simple collector set
    private final Map<Dpid, FlowStatsCollector> simpleCollectors = Maps.newConcurrentMap();

    // NewAdaptiveFlowStatsCollector Set
    private final Map<Dpid, NewAdaptiveFlowStatsCollector> afsCollectors = Maps.newConcurrentMap();
    private final Map<Dpid, TableStatisticsCollector> tableStatsCollectors = Maps.newConcurrentMap();

    /**
     * Creates an OpenFlow host provider.
     */
    public OpenFlowRuleProvider() {
        super(new ProviderId("of", "org.onosproject.provider.openflow"));
    }

    @Activate
    protected void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());
        providerService = providerRegistry.register(this);
        controller.addListener(listener);
        controller.addEventListener(listener);

        modified(context);

        pendingBatches = createBatchCache();

        createCollectors();

        log.info("Started with flowPollFrequency = {}, adaptiveFlowSampling = {}",
                flowPollFrequency, adaptiveFlowSampling);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        cfgService.unregisterProperties(getClass(), false);
        stopCollectors();
        providerRegistry.unregister(this);
        providerService = null;

        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        int newFlowPollFrequency;
        try {
            String s = get(properties, "flowPollFrequency");
            newFlowPollFrequency = isNullOrEmpty(s) ? flowPollFrequency : Integer.parseInt(s.trim());

        } catch (NumberFormatException | ClassCastException e) {
            newFlowPollFrequency = flowPollFrequency;
        }

        if (newFlowPollFrequency != flowPollFrequency) {
            flowPollFrequency = newFlowPollFrequency;
            adjustRate();
        }

        log.info("Settings: flowPollFrequency={}", flowPollFrequency);

        boolean newAdaptiveFlowSampling;
        String s = get(properties, "adaptiveFlowSampling");
        newAdaptiveFlowSampling = isNullOrEmpty(s) ? adaptiveFlowSampling : Boolean.parseBoolean(s.trim());

        if (newAdaptiveFlowSampling != adaptiveFlowSampling) {
            // stop previous collector
            stopCollectors();
            adaptiveFlowSampling = newAdaptiveFlowSampling;
            // create new collectors
            createCollectors();
        }

        log.info("Settings: adaptiveFlowSampling={}", adaptiveFlowSampling);
    }

    private Cache<Long, InternalCacheEntry> createBatchCache() {
        return CacheBuilder.newBuilder()
                .expireAfterWrite(10, TimeUnit.SECONDS)
                .removalListener((RemovalNotification<Long, InternalCacheEntry> notification) -> {
                    if (notification.getCause() == RemovalCause.EXPIRED) {
                        providerService.batchOperationCompleted(notification.getKey(),
                                                                notification.getValue().failedCompletion());
                    }
                }).build();
    }

    private void createCollectors() {
        controller.getSwitches().forEach(this::createCollector);
    }

    private void createCollector(OpenFlowSwitch sw) {
        if (sw == null) {
            return;
        }
        if (adaptiveFlowSampling) {
            // NewAdaptiveFlowStatsCollector Constructor
            NewAdaptiveFlowStatsCollector fsc =
                    new NewAdaptiveFlowStatsCollector(driverService, sw, flowPollFrequency);
            stopCollectorIfNeeded(afsCollectors.put(new Dpid(sw.getId()), fsc));
            fsc.start();
        } else {
            FlowStatsCollector fsc = new FlowStatsCollector(timer, sw, flowPollFrequency);
            stopCollectorIfNeeded(simpleCollectors.put(new Dpid(sw.getId()), fsc));
            fsc.start();
        }
        TableStatisticsCollector tsc = new TableStatisticsCollector(timer, sw, flowPollFrequency);
        stopCollectorIfNeeded(tableStatsCollectors.put(new Dpid(sw.getId()), tsc));
        tsc.start();
    }

    private void stopCollectorIfNeeded(SwitchDataCollector collector) {
        if (collector != null) {
            collector.stop();
        }
    }

    private void stopCollectors() {
        if (adaptiveFlowSampling) {
            // NewAdaptiveFlowStatsCollector Destructor
            afsCollectors.values().forEach(NewAdaptiveFlowStatsCollector::stop);
            afsCollectors.clear();
        } else {
            simpleCollectors.values().forEach(FlowStatsCollector::stop);
            simpleCollectors.clear();
        }
        tableStatsCollectors.values().forEach(TableStatisticsCollector::stop);
        tableStatsCollectors.clear();
    }

    private void adjustRate() {
        DefaultLoad.setPollInterval(flowPollFrequency);
        if (adaptiveFlowSampling) {
            // NewAdaptiveFlowStatsCollector calAndPollInterval
            afsCollectors.values().forEach(fsc -> fsc.adjustCalAndPollInterval(flowPollFrequency));
        } else {
            simpleCollectors.values().forEach(fsc -> fsc.adjustPollInterval(flowPollFrequency));
        }
        tableStatsCollectors.values().forEach(tsc -> tsc.adjustPollInterval(flowPollFrequency));
    }

    @Override
    public void applyFlowRule(FlowRule... flowRules) {
        for (FlowRule flowRule : flowRules) {
            applyRule(flowRule);
        }
    }

    private void applyRule(FlowRule flowRule) {
        Dpid dpid = Dpid.dpid(flowRule.deviceId().uri());
        OpenFlowSwitch sw = controller.getSwitch(dpid);

        if (sw == null) {
            return;
        }

        FlowRuleExtPayLoad flowRuleExtPayLoad = flowRule.payLoad();
        if (hasPayload(flowRuleExtPayLoad)) {
            OFMessage msg = new ThirdPartyMessage(flowRuleExtPayLoad.payLoad());
            sw.sendMsg(msg);
            return;
        }
        sw.sendMsg(FlowModBuilder.builder(flowRule, sw.factory(),
                Optional.empty(), Optional.of(driverService)).buildFlowAdd());
    }

    @Override
    public void removeFlowRule(FlowRule... flowRules) {
        for (FlowRule flowRule : flowRules) {
            removeRule(flowRule);
        }
    }

    private void removeRule(FlowRule flowRule) {
        Dpid dpid = Dpid.dpid(flowRule.deviceId().uri());
        OpenFlowSwitch sw = controller.getSwitch(dpid);

        if (sw == null) {
            return;
        }

        FlowRuleExtPayLoad flowRuleExtPayLoad = flowRule.payLoad();
        if (hasPayload(flowRuleExtPayLoad)) {
            OFMessage msg = new ThirdPartyMessage(flowRuleExtPayLoad.payLoad());
            sw.sendMsg(msg);
            return;
        }
        sw.sendMsg(FlowModBuilder.builder(flowRule, sw.factory(),
                                          Optional.empty(), Optional.of(driverService)).buildFlowDel());
    }

    @Override
    public void removeRulesById(ApplicationId id, FlowRule... flowRules) {
        // TODO: optimize using the ApplicationId
        removeFlowRule(flowRules);
    }

    @Override
    public void executeBatch(FlowRuleBatchOperation batch) {
        checkNotNull(batch);

        Dpid dpid = Dpid.dpid(batch.deviceId().uri());
        OpenFlowSwitch sw = controller.getSwitch(dpid);

        // If switch no longer exists, simply return.
        if (sw == null) {
            Set<FlowRule> failures = ImmutableSet.copyOf(Lists.transform(batch.getOperations(), e -> e.target()));
            providerService.batchOperationCompleted(batch.id(),
                                                    new CompletedBatchOperation(false, failures, batch.deviceId()));
            return;
        }
        pendingBatches.put(batch.id(), new InternalCacheEntry(batch));
        OFFlowMod mod;
        for (FlowRuleBatchEntry fbe : batch.getOperations()) {
            // flow is the third party privacy flow

            FlowRuleExtPayLoad flowRuleExtPayLoad = fbe.target().payLoad();
            if (hasPayload(flowRuleExtPayLoad)) {
                OFMessage msg = new ThirdPartyMessage(flowRuleExtPayLoad.payLoad());
                sw.sendMsg(msg);
                continue;
            }
            FlowModBuilder builder =
                    FlowModBuilder.builder(fbe.target(), sw.factory(),
                            Optional.of(batch.id()), Optional.of(driverService));
            NewAdaptiveFlowStatsCollector collector = afsCollectors.get(dpid);
            switch (fbe.operator()) {
                case ADD:
                    mod = builder.buildFlowAdd();
                    break;
                case REMOVE:
                    mod = builder.buildFlowDel();
                    break;
                case MODIFY:
                    mod = builder.buildFlowMod();
                    break;
                default:
                    log.error("Unsupported batch operation {}; skipping flowmod {}",
                            fbe.operator(), fbe);
                    continue;
            }
            sw.sendMsg(mod);
        }
        OFBarrierRequest.Builder builder = sw.factory().buildBarrierRequest()
                .setXid(batch.id());
        sw.sendMsg(builder.build());
    }

    private boolean hasPayload(FlowRuleExtPayLoad flowRuleExtPayLoad) {
        return flowRuleExtPayLoad != null &&
                flowRuleExtPayLoad.payLoad() != null &&
                flowRuleExtPayLoad.payLoad().length > 0;
    }

    private class InternalFlowProvider
            implements OpenFlowSwitchListener, OpenFlowEventListener {

        @Override
        public void switchAdded(Dpid dpid) {
            createCollector(controller.getSwitch(dpid));
        }

        @Override
        public void switchRemoved(Dpid dpid) {
            if (adaptiveFlowSampling) {
                stopCollectorIfNeeded(afsCollectors.remove(dpid));
            } else {
                stopCollectorIfNeeded(simpleCollectors.remove(dpid));
            }
            stopCollectorIfNeeded(tableStatsCollectors.remove(dpid));
        }

        @Override
        public void switchChanged(Dpid dpid) {
        }

        @Override
        public void portChanged(Dpid dpid, OFPortStatus status) {
            // TODO: Decide whether to evict flows internal store.
        }

        @Override
        public void handleMessage(Dpid dpid, OFMessage msg) {
            if (providerService == null) {
                // We are shutting down, nothing to be done
                return;
            }
            DeviceId deviceId = DeviceId.deviceId(Dpid.uri(dpid));
            switch (msg.getType()) {
                case FLOW_REMOVED:
                    OFFlowRemoved removed = (OFFlowRemoved) msg;

                    FlowEntry fr = new FlowEntryBuilder(deviceId, removed, driverService).build();
                    providerService.flowRemoved(fr);
                    break;
                case STATS_REPLY:
                    if (((OFStatsReply) msg).getStatsType() == OFStatsType.FLOW) {
                        pushFlowMetrics(dpid, (OFFlowStatsReply) msg);
                    } else if (((OFStatsReply) msg).getStatsType() == OFStatsType.TABLE) {
                        pushTableStatistics(dpid, (OFTableStatsReply) msg);
                    }
                    break;
                case BARRIER_REPLY:
                    try {
                        InternalCacheEntry entry = pendingBatches.getIfPresent(msg.getXid());
                        if (entry != null) {
                            providerService
                                    .batchOperationCompleted(msg.getXid(),
                                                             entry.completed());
                        } else {
                            log.warn("Received unknown Barrier Reply: {}",
                                     msg.getXid());
                        }
                    } finally {
                        pendingBatches.invalidate(msg.getXid());
                    }
                    break;
                case ERROR:
                    // TODO: This needs to get suppressed in a better way.
                    if (msg instanceof OFBadRequestErrorMsg &&
                            ((OFBadRequestErrorMsg) msg).getCode() == OFBadRequestCode.BAD_TYPE) {
                        log.debug("Received error message {} from {}", msg, dpid);
                    } else {
                        log.warn("Received error message {} from {}", msg, dpid);
                    }
                    handleErrorMsg(deviceId, msg);
                    break;
                default:
                    log.debug("Unhandled message type: {}", msg.getType());
            }
        }

        private void handleErrorMsg(DeviceId deviceId, OFMessage msg) {
            InternalCacheEntry entry = pendingBatches.getIfPresent(msg.getXid());
            OFErrorMsg error = (OFErrorMsg) msg;
            OFMessage ofMessage = null;
            switch (error.getErrType()) {
                case BAD_ACTION:
                    OFBadActionErrorMsg baErrorMsg = (OFBadActionErrorMsg) error;
                    if (baErrorMsg.getData().getParsedMessage().isPresent()) {
                        ofMessage = baErrorMsg.getData().getParsedMessage().get();
                    }
                    break;
                case BAD_INSTRUCTION:
                    OFBadInstructionErrorMsg biErrorMsg = (OFBadInstructionErrorMsg) error;
                    if (biErrorMsg.getData().getParsedMessage().isPresent()) {
                        ofMessage = biErrorMsg.getData().getParsedMessage().get();
                    }
                    break;
                case BAD_MATCH:
                    OFBadMatchErrorMsg bmErrorMsg = (OFBadMatchErrorMsg) error;
                    if (bmErrorMsg.getData().getParsedMessage().isPresent()) {
                        ofMessage = bmErrorMsg.getData().getParsedMessage().get();
                    }
                    break;
                case FLOW_MOD_FAILED:
                    OFFlowModFailedErrorMsg fmFailed = (OFFlowModFailedErrorMsg) error;
                    if (fmFailed.getData().getParsedMessage().isPresent()) {
                        ofMessage = fmFailed.getData().getParsedMessage().get();
                    }
                    break;
                default:
                    // Do nothing.
                    return;
                }

                if (ofMessage != null) {

                    if (entry != null)  {
                        OFFlowMod ofFlowMod = (OFFlowMod) ofMessage;
                        entry.appendFailure(new FlowEntryBuilder(deviceId, ofFlowMod, driverService).build());
                    } else {
                      log.error("No matching batch for this error: {}", error);
                    }

                } else {

                    U64 cookieId = readCookieIdFromOFErrorMsg(error, msg.getVersion());

                    if (cookieId != null) {
                        long flowId = cookieId.getValue();

                        if (entry != null) {
                            for (FlowRuleBatchEntry fbEntry : entry.operation.getOperations()) {
                                if (fbEntry.target().id().value() == flowId) {
                                    entry.appendFailure(fbEntry.target());
                                    break;
                                }
                            }
                        } else {
                            log.error("No matching batch for this error: {}", error);
                        }

                    } else {
                        log.error("Flow installation failed but switch " +
                                "didn't tell us which one.");
                    }
                }
        }

        /**
         * Reading cookieId from OFErrorMsg.
         *
         * Loxigen OpenFlow API failed in parsing error messages because of
         * 64 byte data truncation based on OpenFlow specs. The method written
         * is a workaround to extract the cookieId from the packet till the
         * issue is resolved in Loxigen OpenFlow code.
         * Ref: https://groups.google.com/a/onosproject.org/forum/#!topic
         * /onos-dev/_KwlHZDllLE
         *
         * @param msg OF error message
         * @param ofVersion Openflow version
         * @return cookieId
         */
        private U64 readCookieIdFromOFErrorMsg(OFErrorMsg msg,
                                               OFVersion ofVersion) {

            if (ofVersion.wireVersion < OFVersion.OF_13.wireVersion) {
                log.debug("Unhandled error msg with OF version {} " +
                        "which is less than {}",
                        ofVersion, OFVersion.OF_13);
                return null;
            }

            ByteBuf bb = Unpooled.wrappedBuffer(msg.getData().getData());

            if (bb.readableBytes() < MIN_EXPECTED_BYTE_LEN) {
                log.debug("Wrong length: Expected to be >= {}, was: {}",
                        MIN_EXPECTED_BYTE_LEN, bb.readableBytes());
                return null;
            }

            byte ofVer = bb.readByte();

            if (ofVer != ofVersion.wireVersion) {
                log.debug("Wrong version: Expected={}, got={}",
                        ofVersion.wireVersion, ofVer);
                return null;
            }

            byte type = bb.readByte();

            if (type != OFType.FLOW_MOD.ordinal()) {
                log.debug("Wrong type: Expected={}, got={}",
                        OFType.FLOW_MOD.ordinal(), type);
                return null;
            }

            int length = U16.f(bb.readShort());

            if (length < MIN_EXPECTED_BYTE_LEN) {
                log.debug("Wrong length: Expected to be >= {}, was: {}",
                        MIN_EXPECTED_BYTE_LEN, length);
                return null;
            }

            bb.skipBytes(SKIP_BYTES);
            return U64.ofRaw(bb.readLong());
        }

        @Override
        public void receivedRoleReply(Dpid dpid, RoleState requested,
                                      RoleState response) {
            // Do nothing here for now.
        }

        private void pushFlowMetrics(Dpid dpid, OFFlowStatsReply replies) {

            DeviceId did = DeviceId.deviceId(Dpid.uri(dpid));
            NewAdaptiveFlowStatsCollector afsc = afsCollectors.get(dpid);

            if (adaptiveFlowSampling && afsc != null)  {
                List<FlowEntry> flowEntries = replies.getEntries().stream()
                        .map(entry -> new FlowEntryBuilder(did, entry, driverService).withSetAfsc(afsc).build())
                        .collect(Collectors.toList());

                // Check that OFFlowStatsReply Xid is same with the one of OFFlowStatsRequest?
                if (afsc.getFlowMissingXid() != NewAdaptiveFlowStatsCollector.NO_FLOW_MISSING_XID) {
                        log.debug("OpenFlowRuleProvider:pushFlowMetrics, flowMissingXid={}, "
                                          + "OFFlowStatsReply Xid={}, for {}",
                                  afsc.getFlowMissingXid(), replies.getXid(), dpid);
                    if (afsc.getFlowMissingXid() == replies.getXid()) {
                        // call entire flow stats update with flowMissing synchronization.
                        // used existing pushFlowMetrics
                        providerService.pushFlowMetrics(did, flowEntries);
                    }
                    // reset flowMissingXid to NO_FLOW_MISSING_XID
                    afsc.setFlowMissingXid(NewAdaptiveFlowStatsCollector.NO_FLOW_MISSING_XID);
                } else {
                    // call individual flow stats update
                    providerService.pushFlowMetricsWithoutFlowMissing(did, flowEntries);
                }
            } else {
                List<FlowEntry> flowEntries = replies.getEntries().stream()
                        .map(entry -> new FlowEntryBuilder(did, entry, driverService).build())
                        .collect(Collectors.toList());

                // call existing entire flow stats update with flowMissing synchronization
                providerService.pushFlowMetrics(did, flowEntries);
            }
        }

        private void pushTableStatistics(Dpid dpid, OFTableStatsReply replies) {

            DeviceId did = DeviceId.deviceId(Dpid.uri(dpid));
            List<TableStatisticsEntry> tableStatsEntries = replies.getEntries().stream()
                    .map(entry -> buildTableStatistics(did, entry))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            providerService.pushTableStatistics(did, tableStatsEntries);
        }

        private TableStatisticsEntry buildTableStatistics(DeviceId deviceId,
                                                          OFTableStatsEntry ofEntry) {
            TableStatisticsEntry entry = null;
            if (ofEntry != null) {
                entry = new DefaultTableStatisticsEntry(deviceId,
                                                        ofEntry.getTableId().getValue(),
                                                        ofEntry.getActiveCount(),
                                                        ofEntry.getLookupCount().getValue(),
                                                        ofEntry.getMatchedCount().getValue());
            }

            return entry;

        }
    }

    /**
     * The internal cache entry holding the original request as well as
     * accumulating the any failures along the way.
     * <p/>
     * If this entry is evicted from the cache then the entire operation is
     * considered failed. Otherwise, only the failures reported by the device
     * will be propagated up.
     */
    private class InternalCacheEntry {

        private final FlowRuleBatchOperation operation;
        private final Set<FlowRule> failures = Sets.newConcurrentHashSet();

        public InternalCacheEntry(FlowRuleBatchOperation operation) {
            this.operation = operation;
        }

        /**
         * Appends a failed rule to the set of failed items.
         *
         * @param rule the failed rule
         */
        public void appendFailure(FlowRule rule) {
            failures.add(rule);
        }

        /**
         * Fails the entire batch and returns the failed operation.
         *
         * @return the failed operation
         */
        public CompletedBatchOperation failedCompletion() {
            Set<FlowRule> fails = operation.getOperations().stream()
                    .map(op -> op.target()).collect(Collectors.toSet());
            return new CompletedBatchOperation(false,
                                               Collections
                                                       .unmodifiableSet(fails),
                                               operation.deviceId());
        }

        /**
         * Returns the completed operation and whether the batch suceeded.
         *
         * @return the completed operation
         */
        public CompletedBatchOperation completed() {
            return new CompletedBatchOperation(
                    failures.isEmpty(),
                    Collections
                            .unmodifiableSet(failures),
                    operation.deviceId());
        }
    }

}
