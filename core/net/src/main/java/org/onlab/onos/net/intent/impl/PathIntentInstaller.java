package org.onlab.onos.net.intent.impl;

import static org.onlab.onos.net.flow.DefaultTrafficTreatment.builder;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.ApplicationId;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.flow.CompletedBatchOperation;
import org.onlab.onos.net.flow.DefaultFlowRule;
import org.onlab.onos.net.flow.DefaultTrafficSelector;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRuleBatchEntry;
import org.onlab.onos.net.flow.FlowRuleBatchEntry.FlowRuleOperation;
import org.onlab.onos.net.flow.FlowRuleBatchOperation;
import org.onlab.onos.net.flow.FlowRuleService;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.intent.IntentExtensionService;
import org.onlab.onos.net.intent.IntentInstaller;
import org.onlab.onos.net.intent.PathIntent;
import org.slf4j.Logger;

import com.google.common.collect.Lists;

/**
 * Installer for {@link PathIntent path connectivity intents}.
 */
@Component(immediate = true)
public class PathIntentInstaller implements IntentInstaller<PathIntent> {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    private final ApplicationId appId = ApplicationId.getAppId();

    @Activate
    public void activate() {
        intentManager.registerInstaller(PathIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterInstaller(PathIntent.class);
    }

    /**
     * Apply a list of FlowRules.
     *
     * @param rules rules to apply
     */
    private Future<CompletedBatchOperation> applyBatch(List<FlowRuleBatchEntry> rules) {
        FlowRuleBatchOperation batch = new FlowRuleBatchOperation(rules);
        Future<CompletedBatchOperation> future = flowRuleService.applyBatch(batch);
        return future;
//        try {
//            //FIXME don't do this here
//            future.get();
//        } catch (InterruptedException | ExecutionException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
    }

    @Override
    public Future<CompletedBatchOperation> install(PathIntent intent) {
        TrafficSelector.Builder builder =
                DefaultTrafficSelector.builder(intent.selector());
        Iterator<Link> links = intent.path().links().iterator();
        ConnectPoint prev = links.next().dst();
        List<FlowRuleBatchEntry> rules = Lists.newLinkedList();
        while (links.hasNext()) {
            builder.matchInport(prev.port());
            Link link = links.next();
            TrafficTreatment treatment = builder()
                    .setOutput(link.src().port()).build();

            FlowRule rule = new DefaultFlowRule(link.src().deviceId(),
                    builder.build(), treatment,
                    123, appId, 600);
            rules.add(new FlowRuleBatchEntry(FlowRuleOperation.ADD, rule));
            prev = link.dst();
        }

        return applyBatch(rules);
    }

    @Override
    public Future<CompletedBatchOperation> uninstall(PathIntent intent) {
        TrafficSelector.Builder builder =
                DefaultTrafficSelector.builder(intent.selector());
        Iterator<Link> links = intent.path().links().iterator();
        ConnectPoint prev = links.next().dst();
        List<FlowRuleBatchEntry> rules = Lists.newLinkedList();

        while (links.hasNext()) {
            builder.matchInport(prev.port());
            Link link = links.next();
            TrafficTreatment treatment = builder()
                    .setOutput(link.src().port()).build();
            FlowRule rule = new DefaultFlowRule(link.src().deviceId(),
                    builder.build(), treatment,
                    123, appId, 600);
            rules.add(new FlowRuleBatchEntry(FlowRuleOperation.REMOVE, rule));
            prev = link.dst();
        }
        return applyBatch(rules);
    }

    // TODO refactor below this line... ----------------------------

    /**
     * Generates the series of MatchActionOperations from the
     * {@link FlowBatchOperation}.
     * <p>
     * FIXME: Currently supporting PacketPathFlow and SingleDstTreeFlow only.
     * <p>
     * FIXME: MatchActionOperations should have dependency field to the other
     * match action operations, and this method should use this.
     *
     * @param op the {@link FlowBatchOperation} object
     * @return the list of {@link MatchActionOperations} objects
     */
    /*
    private List<MatchActionOperations>
            generateMatchActionOperationsList(FlowBatchOperation op) {

        // MatchAction operations at head (ingress) switches.
        MatchActionOperations headOps = matchActionService.createOperationsList();

        // MatchAction operations at rest of the switches.
        MatchActionOperations tailOps = matchActionService.createOperationsList();

        MatchActionOperations removeOps = matchActionService.createOperationsList();

        for (BatchOperationEntry<Operator, ?> e : op.getOperations()) {

            if (e.getOperator() == FlowBatchOperation.Operator.ADD) {
                generateInstallMatchActionOperations(e, tailOps, headOps);
            } else if (e.getOperator() == FlowBatchOperation.Operator.REMOVE) {
                generateRemoveMatchActionOperations(e, removeOps);
            } else {
                throw new UnsupportedOperationException(
                        "FlowManager supports ADD and REMOVE operations only.");
            }

        }

        return Arrays.asList(tailOps, headOps, removeOps);
    }
    */

    /**
     * Generates MatchActionOperations for an INSTALL FlowBatchOperation.
     * <p/>
     * FIXME: Currently only supports flows that generate exactly two match
     * action operation sets.
     *
     * @param e Flow BatchOperationEntry
     * @param tailOps MatchActionOperation set that the tail
     * MatchActionOperations will be placed in
     * @param headOps MatchActionOperation set that the head
     * MatchActionOperations will be placed in
     */
    /*
    private void generateInstallMatchActionOperations(
            BatchOperationEntry<Operator, ?> e,
            MatchActionOperations tailOps,
            MatchActionOperations headOps) {

        if (!(e.getTarget() instanceof Flow)) {
            throw new IllegalStateException(
                    "The target is not Flow object: " + e.getTarget());
        }

        // Compile flows to match-actions
        Flow flow = (Flow) e.getTarget();
        List<MatchActionOperations> maOps = flow.compile(
                e.getOperator(), matchActionService);
        verifyNotNull(maOps, "Could not compile the flow: " + flow);
        verify(maOps.size() == 2,
                "The flow generates unspported match-action operations.");

        // Map FlowId to MatchActionIds
        for (MatchActionOperations maOp : maOps) {
            for (MatchActionOperationEntry entry : maOp.getOperations()) {
                flowMatchActionsMap.put(
                        KryoFactory.serialize(flow.getId()),
                        KryoFactory.serialize(entry.getTarget()));
            }
        }

        // Merge match-action operations
        for (MatchActionOperationEntry mae : maOps.get(0).getOperations()) {
            verify(mae.getOperator() == MatchActionOperations.Operator.INSTALL);
            tailOps.addOperation(mae);
        }
        for (MatchActionOperationEntry mae : maOps.get(1).getOperations()) {
            verify(mae.getOperator() == MatchActionOperations.Operator.INSTALL);
            headOps.addOperation(mae);
        }
    }
    */
    /**
     * Generates MatchActionOperations for a REMOVE FlowBatchOperation.
     *
     * @param e Flow BatchOperationEntry
     * @param removeOps MatchActionOperation set that the remove
     * MatchActionOperations will be placed in
     */
    /*
    private void generateRemoveMatchActionOperations(
            BatchOperationEntry<Operator, ?> e,
            MatchActionOperations removeOps) {

        if (!(e.getTarget() instanceof FlowId)) {
            throw new IllegalStateException(
                    "The target is not a FlowId object: " + e.getTarget());
        }

        // Compile flows to match-actions
        FlowId flowId = (FlowId) e.getTarget();

        for (byte[] matchActionIdBytes :
            flowMatchActionsMap.remove(KryoFactory.serialize(flowId))) {
            MatchActionId matchActionId = KryoFactory.deserialize(matchActionIdBytes);
            removeOps.addOperation(new MatchActionOperationEntry(
                    MatchActionOperations.Operator.REMOVE, matchActionId));
        }
    }
    */
}
