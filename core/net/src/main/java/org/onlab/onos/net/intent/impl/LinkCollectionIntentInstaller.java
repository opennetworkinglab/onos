package org.onlab.onos.net.intent.impl;

import java.util.List;
import java.util.concurrent.Future;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.ApplicationId;
import org.onlab.onos.CoreService;
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
import org.onlab.onos.net.intent.LinkCollectionIntent;
import org.onlab.onos.net.intent.PathIntent;
import org.slf4j.Logger;

import com.google.common.collect.Lists;

import static org.onlab.onos.net.flow.DefaultTrafficTreatment.builder;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Installer for {@link org.onlab.onos.net.intent.LinkCollectionIntent}
 * path segment intents.
 */
@Component(immediate = true)
public class LinkCollectionIntentInstaller implements IntentInstaller<LinkCollectionIntent> {

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
        intentManager.registerInstaller(LinkCollectionIntent.class, this);
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
        return flowRuleService.applyBatch(batch);
    }

    @Override
    public Future<CompletedBatchOperation> install(LinkCollectionIntent intent) {
        TrafficSelector.Builder builder =
                DefaultTrafficSelector.builder(intent.selector());
        List<FlowRuleBatchEntry> rules = Lists.newLinkedList();
        for (Link link : intent.links()) {
            TrafficTreatment treatment = builder()
                    .setOutput(link.src().port()).build();

            FlowRule rule = new DefaultFlowRule(link.src().deviceId(),
                    builder.build(), treatment,
                    123, appId, 600);
            rules.add(new FlowRuleBatchEntry(FlowRuleOperation.ADD, rule));
        }

        return applyBatch(rules);
    }

    @Override
    public Future<CompletedBatchOperation> uninstall(LinkCollectionIntent intent) {
        TrafficSelector.Builder builder =
                DefaultTrafficSelector.builder(intent.selector());
        List<FlowRuleBatchEntry> rules = Lists.newLinkedList();

        for (Link link : intent.links()) {
            TrafficTreatment treatment = builder()
                    .setOutput(link.src().port()).build();
            FlowRule rule = new DefaultFlowRule(link.src().deviceId(),
                    builder.build(), treatment,
                    123, appId, 600);
            rules.add(new FlowRuleBatchEntry(FlowRuleOperation.REMOVE, rule));
        }
        return applyBatch(rules);
    }
}
