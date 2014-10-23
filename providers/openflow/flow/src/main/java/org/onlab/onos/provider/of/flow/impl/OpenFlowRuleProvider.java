package org.onlab.onos.provider.of.flow.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.ApplicationId;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.flow.CompletedBatchOperation;
import org.onlab.onos.net.flow.DefaultFlowEntry;
import org.onlab.onos.net.flow.FlowEntry;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRuleBatchEntry;
import org.onlab.onos.net.flow.FlowRuleBatchEntry.FlowRuleOperation;
import org.onlab.onos.net.flow.FlowRuleProvider;
import org.onlab.onos.net.flow.FlowRuleProviderRegistry;
import org.onlab.onos.net.flow.FlowRuleProviderService;
import org.onlab.onos.net.intent.BatchOperation;
import org.onlab.onos.net.provider.AbstractProvider;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.net.topology.TopologyService;
import org.onlab.onos.openflow.controller.Dpid;
import org.onlab.onos.openflow.controller.OpenFlowController;
import org.onlab.onos.openflow.controller.OpenFlowEventListener;
import org.onlab.onos.openflow.controller.OpenFlowSwitch;
import org.onlab.onos.openflow.controller.OpenFlowSwitchListener;
import org.onlab.onos.openflow.controller.RoleState;
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
import org.projectfloodlight.openflow.protocol.OFStatsReplyFlags;
import org.projectfloodlight.openflow.protocol.OFStatsType;
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
import org.projectfloodlight.openflow.types.U32;
import org.slf4j.Logger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ExecutionList;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Provider which uses an OpenFlow controller to detect network
 * end-station hosts.
 */
@Component(immediate = true)
public class OpenFlowRuleProvider extends AbstractProvider implements FlowRuleProvider {

    enum BatchState { STARTED, FINISHED, CANCELLED };

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

    /**
     * Creates an OpenFlow host provider.
     */
    public OpenFlowRuleProvider() {
        super(new ProviderId("of", "org.onlab.onos.provider.openflow"));
    }

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        controller.addListener(listener);
        controller.addEventListener(listener);
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
        sw.sendMsg(new FlowModBuilder(flowRule, sw.factory()).buildFlowAdd());
    }



    @Override
    public void removeFlowRule(FlowRule... flowRules) {
        for (int i = 0; i < flowRules.length; i++) {
            removeRule(flowRules[i]);
        }

    }

    private void removeRule(FlowRule flowRule) {
        OpenFlowSwitch sw = controller.getSwitch(Dpid.dpid(flowRule.deviceId().uri()));
        sw.sendMsg(new FlowModBuilder(flowRule, sw.factory()).buildFlowDel());
    }

    @Override
    public void removeRulesById(ApplicationId id, FlowRule... flowRules) {
        // TODO: optimize using the ApplicationId
        removeFlowRule(flowRules);
    }

    @Override
    public ListenableFuture<CompletedBatchOperation> executeBatch(BatchOperation<FlowRuleBatchEntry> batch) {
        final Set<Dpid> sws =
                Collections.newSetFromMap(new ConcurrentHashMap<Dpid, Boolean>());
        final Map<Long, FlowRuleBatchEntry> fmXids = new HashMap<Long, FlowRuleBatchEntry>();
        OFFlowMod mod = null;
        for (FlowRuleBatchEntry fbe : batch.getOperations()) {
            FlowRule flowRule = fbe.getTarget();
            OpenFlowSwitch sw = controller.getSwitch(Dpid.dpid(flowRule.deviceId().uri()));
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
            sws.add(new Dpid(sw.getId()));
            FlowModBuilder builder = new FlowModBuilder(flowRule, sw.factory());
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
                sw.sendMsg(mod);
                fmXids.put(mod.getXid(), fbe);
            } else {
                log.error("Conversion of flowrule {} failed.", flowRule);
            }

        }
        InstallationFuture installation = new InstallationFuture(sws, fmXids);
        for (Long xid : fmXids.keySet()) {
            pendingFMs.put(xid, installation);
        }
        pendingFutures.put(U32.f(batch.hashCode()), installation);
        installation.verify(U32.f(batch.hashCode()));
        return installation;
    }


    private class InternalFlowProvider
    implements OpenFlowSwitchListener, OpenFlowEventListener {

        private final Map<Dpid, FlowStatsCollector> collectors = Maps.newHashMap();
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
                }
                break;
            case ERROR:
                future = pendingFMs.get(msg.getXid());
                if (future != null) {
                    future.fail((OFErrorMsg) msg, dpid);
                }
                break;
            default:
                log.debug("Unhandled message type: {}", msg.getType());
            }

        }

        @Override
        public void roleAssertFailed(Dpid dpid, RoleState role) {}

        private synchronized void pushFlowMetrics(Dpid dpid, OFStatsReply stats) {
            if (stats.getStatsType() != OFStatsType.FLOW) {
                return;
            }
            DeviceId did = DeviceId.deviceId(Dpid.uri(dpid));
            final OFFlowStatsReply replies = (OFFlowStatsReply) stats;
            //final List<FlowRule> entries = Lists.newLinkedList();

            for (OFFlowStatsEntry reply : replies.getEntries()) {
                if (!tableMissRule(dpid, reply)) {
                    completeEntries.put(did, new FlowEntryBuilder(dpid, reply).build());
                }
            }

            if (!stats.getFlags().contains(OFStatsReplyFlags.REPLY_MORE)) {
                log.debug("sending flowstats to core {}", completeEntries.get(did));
                providerService.pushFlowMetrics(did, completeEntries.get(did));
                completeEntries.removeAll(did);
            }
        }

        private boolean tableMissRule(Dpid dpid, OFFlowStatsEntry reply) {
            if (reply.getVersion().equals(OFVersion.OF_10) ||
                    reply.getMatch().getMatchFields().iterator().hasNext()) {
                return false;
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

    private class InstallationFuture implements ListenableFuture<CompletedBatchOperation> {

        private final Set<Dpid> sws;
        private final AtomicBoolean ok = new AtomicBoolean(true);
        private final Map<Long, FlowRuleBatchEntry> fms;

        private final Set<FlowEntry> offendingFlowMods = Sets.newHashSet();

        private final CountDownLatch countDownLatch;
        private Long pendingXid;
        private BatchState state;

        private final ExecutionList executionList = new ExecutionList();

        public InstallationFuture(Set<Dpid> sws, Map<Long, FlowRuleBatchEntry> fmXids) {
            this.state = BatchState.STARTED;
            this.sws = sws;
            this.fms = fmXids;
            countDownLatch = new CountDownLatch(sws.size());
        }

        public void fail(OFErrorMsg msg, Dpid dpid) {

            ok.set(false);
            removeRequirement(dpid);
            FlowEntry fe = null;
            FlowRuleBatchEntry fbe = fms.get(msg.getXid());
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

        }


        public void satisfyRequirement(Dpid dpid) {
            log.debug("Satisfaction from switch {}", dpid);
            removeRequirement(dpid);
        }


        public void verify(Long id) {
            pendingXid = id;
            for (Dpid dpid : sws) {
                OpenFlowSwitch sw = controller.getSwitch(dpid);
                OFBarrierRequest.Builder builder = sw.factory()
                        .buildBarrierRequest()
                        .setXid(id);
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
            invokeCallbacks();
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
            CompletedBatchOperation result = new CompletedBatchOperation(ok.get(), offendingFlowMods);
            return result;
        }

        @Override
        public CompletedBatchOperation get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException,
                TimeoutException {
            if (countDownLatch.await(timeout, unit)) {
                this.state = BatchState.FINISHED;
                CompletedBatchOperation result = new CompletedBatchOperation(ok.get(), offendingFlowMods);
                return result;
            }
            throw new TimeoutException();
        }

        private void cleanUp() {
            if (isDone() || isCancelled()) {
                if (pendingXid != null) {
                    pendingFutures.remove(pendingXid);
                }
                for (Long xid : fms.keySet()) {
                    pendingFMs.remove(xid);
                }
            }
        }

        private void removeRequirement(Dpid dpid) {
            countDownLatch.countDown();
            if (countDownLatch.getCount() == 0) {
                invokeCallbacks();
            }
            sws.remove(dpid);
            cleanUp();
        }

        @Override
        public void addListener(Runnable runnable, Executor executor) {
            executionList.add(runnable, executor);
        }

        private void invokeCallbacks() {
            executionList.execute();
        }
    }

}
