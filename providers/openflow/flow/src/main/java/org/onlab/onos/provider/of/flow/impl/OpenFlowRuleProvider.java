package org.onlab.onos.provider.of.flow.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
import org.onlab.onos.net.flow.FlowEntry;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRuleBatchEntry;
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
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionApplyActions;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U32;
import org.slf4j.Logger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * Provider which uses an OpenFlow controller to detect network
 * end-station hosts.
 */
@Component(immediate = true)
public class OpenFlowRuleProvider extends AbstractProvider implements FlowRuleProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenFlowController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    private FlowRuleProviderService providerService;

    private final InternalFlowProvider listener = new InternalFlowProvider();

    private final Map<Long, InstallationFuture> pendingFutures =
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


    //TODO: InternalFlowRuleProvider listening to stats and error and flowremoved.
    // possibly barriers as well. May not be internal at all...
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
            collectors.remove(dpid).stop();
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
                //TODO: make this better
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
                future = pendingFutures.get(msg.getXid());
                if (future != null) {
                    future.fail((OFErrorMsg) msg, dpid);
                }
                break;
            default:
                log.debug("Unhandled message type: {}", msg.getType());
            }

        }

        @Override
        public void roleAssertFailed(Dpid dpid, RoleState role) {
            // TODO Auto-generated method stub

        }

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
            // TODO NEED TO FIND A BETTER WAY TO AVOID DOING THIS
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


    @Override
    public Future<Void> executeBatch(BatchOperation<FlowRuleBatchEntry> batch) {
        final Set<Dpid> sws = new HashSet<Dpid>();

        for (FlowRuleBatchEntry fbe : batch.getOperations()) {
            FlowRule flowRule = fbe.getTarget();
            OpenFlowSwitch sw = controller.getSwitch(Dpid.dpid(flowRule.deviceId().uri()));
            sws.add(new Dpid(sw.getId()));
            switch (fbe.getOperator()) {
                case ADD:
                  //TODO: Track XID for each flowmod
                    sw.sendMsg(new FlowModBuilder(flowRule, sw.factory()).buildFlowAdd());
                    break;
                case REMOVE:
                  //TODO: Track XID for each flowmod
                    sw.sendMsg(new FlowModBuilder(flowRule, sw.factory()).buildFlowDel());
                    break;
                case MODIFY:
                  //TODO: Track XID for each flowmod
                    sw.sendMsg(new FlowModBuilder(flowRule, sw.factory()).buildFlowMod());
                    break;
                default:
                    log.error("Unsupported batch operation {}", fbe.getOperator());
            }
        }
        InstallationFuture installation = new InstallationFuture(sws);
        pendingFutures.put(U32.f(batch.hashCode()), installation);
        installation.verify(batch.hashCode());
        return installation;
    }

    private class InstallationFuture implements Future<Void> {

        private final Set<Dpid> sws;
        private final AtomicBoolean ok = new AtomicBoolean(true);
        private final List<FlowEntry> offendingFlowMods = Lists.newLinkedList();

        private final CountDownLatch countDownLatch;

        public InstallationFuture(Set<Dpid> sws) {
            this.sws = sws;
            countDownLatch = new CountDownLatch(sws.size());
        }

        public void fail(OFErrorMsg msg, Dpid dpid) {
            ok.set(false);
            //TODO add reason to flowentry
            //TODO handle specific error msgs
            //offendingFlowMods.add(new FlowEntryBuilder(dpid, msg.));
            switch (msg.getErrType()) {
                case BAD_ACTION:
                    break;
                case BAD_INSTRUCTION:
                    break;
                case BAD_MATCH:
                    break;
                case BAD_REQUEST:
                    break;
                case EXPERIMENTER:
                    break;
                case FLOW_MOD_FAILED:
                    break;
                case GROUP_MOD_FAILED:
                    break;
                case HELLO_FAILED:
                    break;
                case METER_MOD_FAILED:
                    break;
                case PORT_MOD_FAILED:
                    break;
                case QUEUE_OP_FAILED:
                    break;
                case ROLE_REQUEST_FAILED:
                    break;
                case SWITCH_CONFIG_FAILED:
                    break;
                case TABLE_FEATURES_FAILED:
                    break;
                case TABLE_MOD_FAILED:
                    break;
                default:
                    break;

            }

        }

        public void satisfyRequirement(Dpid dpid) {
            log.warn("Satisfaction from switch {}", dpid);
            sws.remove(controller.getSwitch(dpid));
            countDownLatch.countDown();
        }

        public void verify(Integer id) {
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
                // TODO Auto-generated method stub
                return false;
        }

        @Override
        public boolean isCancelled() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isDone() {
            return sws.isEmpty();
        }

        @Override
        public Void get() throws InterruptedException, ExecutionException {
            countDownLatch.await();
            //return offendingFlowMods;
            return null;
        }

        @Override
        public Void get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException,
                TimeoutException {
            countDownLatch.await(timeout, unit);
            //return offendingFlowMods;
            return null;
        }

    }

}
