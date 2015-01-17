/*
 * Copyright 2014 Open Networking Laboratory
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

import static com.google.common.base.Preconditions.checkState;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.BatchOperation;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleBatchEntry;
import org.onosproject.net.flow.FlowRuleBatchEntry.FlowRuleOperation;
import org.onosproject.net.flow.FlowRuleProvider;
import org.onosproject.net.flow.FlowRuleProviderRegistry;
import org.onosproject.net.flow.FlowRuleProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowController;
import org.onosproject.openflow.controller.OpenFlowEventListener;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.OpenFlowSwitchListener;
import org.onosproject.openflow.controller.RoleState;
import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFBarrierRequest;
import org.projectfloodlight.openflow.protocol.OFErrorMsg;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowRemoved;
import org.projectfloodlight.openflow.protocol.OFFlowStatsEntry;
import org.projectfloodlight.openflow.protocol.OFFlowStatsReply;
import org.projectfloodlight.openflow.protocol.OFInstructionType;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.errormsg.OFBadActionErrorMsg;
import org.projectfloodlight.openflow.protocol.errormsg.OFBadInstructionErrorMsg;
import org.projectfloodlight.openflow.protocol.errormsg.OFBadMatchErrorMsg;
import org.projectfloodlight.openflow.protocol.errormsg.OFBadRequestErrorMsg;
import org.projectfloodlight.openflow.protocol.errormsg.OFFlowModFailedErrorMsg;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionApplyActions;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * Provider which uses an OpenFlow controller to detect network
 * end-station hosts.
 */
@Component(immediate = true)
public class OpenFlowRuleProvider extends AbstractProvider implements FlowRuleProvider {

    enum BatchState { STARTED, FINISHED, CANCELLED };

    private static final int LOWEST_PRIORITY = 0;

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenFlowController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    private FlowRuleProviderService providerService;

    private final InternalFlowProvider listener = new InternalFlowProvider();

    // FIXME: This should be an expiring map to ensure futures that don't have
    // a future eventually get garbage collected.
    private final Map<Long, InstallationFuture> pendingFutures =
            new ConcurrentHashMap<Long, InstallationFuture>();

    private final Map<Long, InstallationFuture> pendingFMs =
            new ConcurrentHashMap<Long, InstallationFuture>();

    private final Map<Dpid, FlowStatsCollector> collectors = Maps.newHashMap();

    private final AtomicLong xidCounter = new AtomicLong(1);

    /**
     * Creates an OpenFlow host provider.
     */
    public OpenFlowRuleProvider() {
        super(new ProviderId("of", "org.onosproject.provider.openflow"));
    }

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        controller.addListener(listener);
        controller.addEventListener(listener);

        for (OpenFlowSwitch sw : controller.getSwitches()) {
            FlowStatsCollector fsc = new FlowStatsCollector(sw, POLL_INTERVAL);
            fsc.start();
            collectors.put(new Dpid(sw.getId()), fsc);
        }


        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        providerRegistry.unregister(this);
        providerService = null;

        log.info("Stopped");
    }

    @Override
    public void applyFlowRule(FlowRule... flowRules) {
        for (int i = 0; i < flowRules.length; i++) {
            applyRule(flowRules[i]);
        }
    }

    private void applyRule(FlowRule flowRule) {
        OpenFlowSwitch sw = controller.getSwitch(Dpid.dpid(flowRule.deviceId().uri()));
        sw.sendMsg(FlowModBuilder.builder(flowRule, sw.factory(),
                                          Optional.empty()).buildFlowAdd());
    }


    @Override
    public void removeFlowRule(FlowRule... flowRules) {
        for (int i = 0; i < flowRules.length; i++) {
            removeRule(flowRules[i]);
        }

    }

    private void removeRule(FlowRule flowRule) {
        OpenFlowSwitch sw = controller.getSwitch(Dpid.dpid(flowRule.deviceId().uri()));
        sw.sendMsg(FlowModBuilder.builder(flowRule, sw.factory(),
                                          Optional.empty()).buildFlowDel());
    }

    @Override
    public void removeRulesById(ApplicationId id, FlowRule... flowRules) {
        // TODO: optimize using the ApplicationId
        removeFlowRule(flowRules);
    }

    @Override
    public Future<CompletedBatchOperation> executeBatch(BatchOperation<FlowRuleBatchEntry> batch) {
        final Set<Dpid> sws = Sets.newConcurrentHashSet();
        final Map<Long, FlowRuleBatchEntry> fmXids = new HashMap<>();
        /*
         * Use identity hash map for reference equality as we could have equal
         * flow mods for different switches.
         */
        Map<OFFlowMod, OpenFlowSwitch> mods = Maps.newIdentityHashMap();
        for (FlowRuleBatchEntry fbe : batch.getOperations()) {
            FlowRule flowRule = fbe.getTarget();
            final Dpid dpid = Dpid.dpid(flowRule.deviceId().uri());
            OpenFlowSwitch sw = controller.getSwitch(dpid);
            if (sw == null) {
                /*
                 * if a switch we are supposed to install to is gone then
                 * cancel (ie. rollback) the work that has been done so far
                 * and return the associated future.
                 */
                InstallationFuture failed = new InstallationFuture(sws, fmXids);
                failed.cancel(true);
                return failed;
            }
            sws.add(dpid);
            final Long flowModXid = xidCounter.getAndIncrement();
            FlowModBuilder builder =
                    FlowModBuilder.builder(flowRule, sw.factory(),
                                           Optional.of(flowModXid));
            OFFlowMod mod = null;
            switch (fbe.getOperator()) {
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
                    log.error("Unsupported batch operation {}", fbe.getOperator());
            }
            if (mod != null) {
                mods.put(mod, sw);
                fmXids.put(flowModXid, fbe);
            } else {
                log.error("Conversion of flowrule {} failed.", flowRule);
            }
        }
        InstallationFuture installation = new InstallationFuture(sws, fmXids);
        for (Long xid : fmXids.keySet()) {
            pendingFMs.put(xid, installation);
        }

        pendingFutures.put(installation.xid(), installation);
        for (Map.Entry<OFFlowMod, OpenFlowSwitch> entry : mods.entrySet()) {
            OpenFlowSwitch sw = entry.getValue();
            OFFlowMod mod = entry.getKey();
            sw.sendMsg(mod);
        }
        installation.verify();
        return installation;
    }


    private class InternalFlowProvider
            implements OpenFlowSwitchListener, OpenFlowEventListener {


        private final Multimap<DeviceId, FlowEntry> completeEntries =
                ArrayListMultimap.create();

        @Override
        public void switchAdded(Dpid dpid) {
            FlowStatsCollector fsc = new FlowStatsCollector(controller.getSwitch(dpid), POLL_INTERVAL);
            fsc.start();
            collectors.put(dpid, fsc);
        }

        @Override
        public void switchRemoved(Dpid dpid) {
            FlowStatsCollector collector = collectors.remove(dpid);
            if (collector != null) {
                collector.stop();
            }
        }

        @Override
        public void switchChanged(Dpid dpid) {
        }

        @Override
        public void portChanged(Dpid dpid, OFPortStatus status) {
            //TODO: Decide whether to evict flows internal store.
        }

        @Override
        public void handleMessage(Dpid dpid, OFMessage msg) {
            InstallationFuture future = null;
            switch (msg.getType()) {
                case FLOW_REMOVED:
                    OFFlowRemoved removed = (OFFlowRemoved) msg;

                    FlowEntry fr = new FlowEntryBuilder(dpid, removed).build();
                    providerService.flowRemoved(fr);
                    break;
                case STATS_REPLY:
                    pushFlowMetrics(dpid, (OFStatsReply) msg);
                    break;
                case BARRIER_REPLY:
                    future = pendingFutures.get(msg.getXid());
                    if (future != null) {
                        future.satisfyRequirement(dpid);
                    } else {
                        log.warn("Received unknown Barrier Reply: {}", msg.getXid());
                    }
                    break;
                case ERROR:
                    log.warn("received Error message {} from {}", msg, dpid);
                    future = pendingFMs.get(msg.getXid());
                    if (future != null) {
                        future.fail((OFErrorMsg) msg, dpid);
                    } else {
                        log.warn("Received unknown Error Reply: {} {}", msg.getXid(), msg);
                    }
                    break;
                default:
                    log.debug("Unhandled message type: {}", msg.getType());
            }

        }

        @Override
        public void receivedRoleReply(Dpid dpid, RoleState requested,
                                      RoleState response) {
            // Do nothing here for now.
        }

        private void pushFlowMetrics(Dpid dpid, OFStatsReply stats) {

            DeviceId did = DeviceId.deviceId(Dpid.uri(dpid));
            final OFFlowStatsReply replies = (OFFlowStatsReply) stats;

            List<FlowEntry> flowEntries = replies.getEntries().stream()
                    .filter(entry -> !tableMissRule(dpid, entry))
                    .map(entry -> new FlowEntryBuilder(dpid, entry).build())
                    .collect(Collectors.toList());

            providerService.pushFlowMetrics(did, flowEntries);

        }

        private boolean tableMissRule(Dpid dpid, OFFlowStatsEntry reply) {
            if (reply.getMatch().getMatchFields().iterator().hasNext()) {
                return false;
            }
            if (reply.getVersion().equals(OFVersion.OF_10)) {
                return reply.getPriority() == LOWEST_PRIORITY
                        && reply.getActions().isEmpty();
            }
            for (OFInstruction ins : reply.getInstructions()) {
                if (ins.getType() == OFInstructionType.APPLY_ACTIONS) {
                    OFInstructionApplyActions apply = (OFInstructionApplyActions) ins;
                    List<OFAction> acts = apply.getActions();
                    for (OFAction act : acts) {
                        if (act.getType() == OFActionType.OUTPUT) {
                            OFActionOutput out = (OFActionOutput) act;
                            if (out.getPort() == OFPort.CONTROLLER) {
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        }

    }

    private class InstallationFuture implements Future<CompletedBatchOperation> {

        // barrier xid
        private final Long xid;
        // waiting for barrier reply from...
        private final Set<Dpid> sws;
        private final AtomicBoolean ok = new AtomicBoolean(true);
        // FlowMod xid ->
        private final Map<Long, FlowRuleBatchEntry> fms;


        private final Set<FlowEntry> offendingFlowMods = Sets.newHashSet();
        // Failed batch operation id
        private Long failedId;

        private final CountDownLatch countDownLatch;
        private BatchState state;

        public InstallationFuture(Set<Dpid> sws, Map<Long, FlowRuleBatchEntry> fmXids) {
            this.xid = xidCounter.getAndIncrement();
            this.state = BatchState.STARTED;
            this.sws = sws;
            this.fms = fmXids;
            countDownLatch = new CountDownLatch(sws.size());
        }

        public Long xid() {
            return xid;
        }

        public void fail(OFErrorMsg msg, Dpid dpid) {

            ok.set(false);
            FlowEntry fe = null;
            FlowRuleBatchEntry fbe = fms.get(msg.getXid());
            failedId = fbe.id();
            FlowRule offending = fbe.getTarget();
            //TODO handle specific error msgs
            switch (msg.getErrType()) {
                case BAD_ACTION:
                    OFBadActionErrorMsg bad = (OFBadActionErrorMsg) msg;
                    fe = new DefaultFlowEntry(offending, bad.getErrType().ordinal(),
                                              bad.getCode().ordinal());
                    break;
                case BAD_INSTRUCTION:
                    OFBadInstructionErrorMsg badins = (OFBadInstructionErrorMsg) msg;
                    fe = new DefaultFlowEntry(offending, badins.getErrType().ordinal(),
                                              badins.getCode().ordinal());
                    break;
                case BAD_MATCH:
                    OFBadMatchErrorMsg badMatch = (OFBadMatchErrorMsg) msg;
                    fe = new DefaultFlowEntry(offending, badMatch.getErrType().ordinal(),
                                              badMatch.getCode().ordinal());
                    break;
                case BAD_REQUEST:
                    OFBadRequestErrorMsg badReq = (OFBadRequestErrorMsg) msg;
                    fe = new DefaultFlowEntry(offending, badReq.getErrType().ordinal(),
                                              badReq.getCode().ordinal());
                    break;
                case FLOW_MOD_FAILED:
                    OFFlowModFailedErrorMsg fmFail = (OFFlowModFailedErrorMsg) msg;
                    fe = new DefaultFlowEntry(offending, fmFail.getErrType().ordinal(),
                                              fmFail.getCode().ordinal());
                    break;
                case EXPERIMENTER:
                case GROUP_MOD_FAILED:
                case HELLO_FAILED:
                case METER_MOD_FAILED:
                case PORT_MOD_FAILED:
                case QUEUE_OP_FAILED:
                case ROLE_REQUEST_FAILED:
                case SWITCH_CONFIG_FAILED:
                case TABLE_FEATURES_FAILED:
                case TABLE_MOD_FAILED:
                    fe = new DefaultFlowEntry(offending, msg.getErrType().ordinal(), 0);
                    break;
                default:
                    log.error("Unknown error type {}", msg.getErrType());

            }
            offendingFlowMods.add(fe);

            removeRequirement(dpid);
        }


        public void satisfyRequirement(Dpid dpid) {
            log.debug("Satisfaction from switch {}", dpid);
            removeRequirement(dpid);
        }


        public void verify() {
            checkState(!sws.isEmpty());
            for (Dpid dpid : sws) {
                OpenFlowSwitch sw = controller.getSwitch(dpid);
                OFBarrierRequest.Builder builder = sw.factory()
                        .buildBarrierRequest()
                        .setXid(xid);
                sw.sendMsg(builder.build());
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (isDone()) {
                return false;
            }
            ok.set(false);
            this.state = BatchState.CANCELLED;
            cleanUp();
            for (FlowRuleBatchEntry fbe : fms.values()) {
                if (fbe.getOperator() == FlowRuleOperation.ADD ||
                        fbe.getOperator() == FlowRuleOperation.MODIFY) {
                    removeFlowRule(fbe.getTarget());
                } else if (fbe.getOperator() == FlowRuleOperation.REMOVE) {
                    applyRule(fbe.getTarget());
                }

            }
            return true;
        }

        @Override
        public boolean isCancelled() {
            return this.state == BatchState.CANCELLED;
        }

        @Override
        public boolean isDone() {
            return this.state == BatchState.FINISHED || isCancelled();
        }

        @Override
        public CompletedBatchOperation get() throws InterruptedException, ExecutionException {
            countDownLatch.await();
            this.state = BatchState.FINISHED;
            Set<Long> failedIds = (failedId != null) ?  Sets.newHashSet(failedId) : Collections.emptySet();
            CompletedBatchOperation result =
                    new CompletedBatchOperation(ok.get(), offendingFlowMods, failedIds);
            //FIXME do cleanup here (moved by BOC)
            cleanUp();
            return result;
        }

        @Override
        public CompletedBatchOperation get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException,
                TimeoutException {
            if (countDownLatch.await(timeout, unit)) {
                this.state = BatchState.FINISHED;
                Set<Long> failedIds = (failedId != null) ?  Sets.newHashSet(failedId) : Collections.emptySet();
                CompletedBatchOperation result =
                        new CompletedBatchOperation(ok.get(), offendingFlowMods, failedIds);
                // FIXME do cleanup here (moved by BOC)
                cleanUp();
                return result;
            }
            throw new TimeoutException(this.toString());
        }

        private void cleanUp() {
            if (isDone() || isCancelled()) {
                pendingFutures.remove(xid);
                for (Long xid : fms.keySet()) {
                    pendingFMs.remove(xid);
                }
            }
        }

        private void removeRequirement(Dpid dpid) {
            countDownLatch.countDown();
            sws.remove(dpid);
            //FIXME don't do cleanup here (moved by BOC)
            //cleanUp();
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("xid", xid)
                    .add("pending devices", sws)
                    .add("devices in batch",
                         fms.values().stream()
                             .map((fbe) -> fbe.getTarget().deviceId())
                             .distinct().collect(Collectors.toList()))
                    .add("failedId", failedId)
                    .add("latchCount", countDownLatch.getCount())
                    .add("state", state)
                    .add("no error?", ok.get())
                    .toString();
        }
    }

}
