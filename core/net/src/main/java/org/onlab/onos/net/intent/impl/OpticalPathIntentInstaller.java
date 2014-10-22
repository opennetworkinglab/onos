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
import org.onlab.onos.CoreService;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.flow.CompletedBatchOperation;
import org.onlab.onos.net.flow.DefaultFlowRule;
import org.onlab.onos.net.flow.DefaultTrafficSelector;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRuleBatchEntry;
import org.onlab.onos.net.flow.FlowRuleBatchOperation;
import org.onlab.onos.net.flow.FlowRuleService;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.flow.FlowRuleBatchEntry.FlowRuleOperation;
import org.onlab.onos.net.intent.IntentExtensionService;
import org.onlab.onos.net.intent.IntentInstaller;
import org.onlab.onos.net.intent.OpticalPathIntent;
import org.slf4j.Logger;

import com.google.common.collect.Lists;

/**
 * OpticaliIntentInstaller for optical path intents.
 * It essentially generates optical FlowRules and
 * call the flowRule service to execute them.
 */

@Component(immediate = true)
public class OpticalPathIntentInstaller implements IntentInstaller<OpticalPathIntent> {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    private ApplicationId appId;

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onlab.onos.net.intent");
        intentManager.registerInstaller(OpticalPathIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterInstaller(OpticalPathIntent.class);
    }

    @Override
    public Future<CompletedBatchOperation> install(OpticalPathIntent intent) {
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
                    builder.build(),
                    treatment,
                    100,   // priority is set to 100
                    appId,
                    100); // ROADM may need longer cross-connect time, here it is assumed 100ms.

            rules.add(new FlowRuleBatchEntry(FlowRuleOperation.ADD, rule));
            prev = link.dst();
        }

        FlowRuleBatchOperation batch = new FlowRuleBatchOperation(rules);
        Future<CompletedBatchOperation> future = flowRuleService.applyBatch(batch);
        return future;
    }

    @Override
    public Future<CompletedBatchOperation> uninstall(OpticalPathIntent intent) {
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
                    builder.build(),
                    treatment,
                    100,
                    appId,
                    100);

            rules.add(new FlowRuleBatchEntry(FlowRuleOperation.REMOVE, rule));
            prev = link.dst();
        }

        FlowRuleBatchOperation batch = new FlowRuleBatchOperation(rules);
        Future<CompletedBatchOperation> future = flowRuleService.applyBatch(batch);
        return future;
    }

}
