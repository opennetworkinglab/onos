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
import org.onlab.onos.net.flow.DefaultTrafficTreatment;
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
import org.onlab.onos.net.resource.Lambda;
import org.onlab.onos.net.resource.LinkResourceAllocations;
import org.onlab.onos.net.resource.LinkResourceRequest;
import org.onlab.onos.net.resource.LinkResourceService;
import org.onlab.onos.net.resource.ResourceRequest;
import org.onlab.onos.net.topology.TopologyService;
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

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkResourceService resourceService;

    private ApplicationId appId;

    final static short WAVELENGTH = 80;

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
    public List<FlowRuleBatchOperation> install(OpticalPathIntent intent) {
        Lambda la = assignWavelength(intent.path().links());
        if (la == null) {
            return null;
        }
        // resourceService.requestResources(la);
        // la.toInt();
        //intent.selector().criteria();

        //TrafficSelector.Builder builder = DefaultTrafficSelector.builder();
        //builder.matchLambdaType(la.toInt())
        //        .matchInport(intent.getSrcConnectPoint().port());

        TrafficSelector.Builder builder =
                DefaultTrafficSelector.builder(intent.selector());
        Iterator<Link> links = intent.path().links().iterator();
        ConnectPoint prev = links.next().dst();
        List<FlowRuleBatchEntry> rules = Lists.newLinkedList();
        // TODO Generate multiple batches
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
                    100,
                    true);
            rules.add(new FlowRuleBatchEntry(FlowRuleOperation.ADD, rule));
            prev = link.dst();
        }
        return Lists.newArrayList(new FlowRuleBatchOperation(rules));
    }

    private Lambda assignWavelength(List<Link> links) {
        // TODO More wavelength assignment algorithm
        int wavenum = 0;
        Iterator<Link> itrlink = links.iterator();
        for (int i = 1; i <= WAVELENGTH; i++) {
            wavenum = i;
            boolean found = true;
            while (itrlink.hasNext()) {
                Link link = itrlink.next();
                if (isWavelengthUsed(link, i)) {
                    found = false;
                    break;
                }
            }
            // First-Fit wavelength assignment algorithm
            if (found) {
                break;
            }
        }

        if (wavenum == 0) {
            return null;
        }

        Lambda wave = Lambda.valueOf(wavenum);
        return wave;
    }

    private boolean isWavelengthUsed(Link link, int i) {
        Iterable<LinkResourceAllocations> wave = resourceService.getAllocations(link);
        for (LinkResourceAllocations ir : wave) {
            //if ir.resources().contains(i) {
            //}
        }
        return false;
    }

    @Override
    public List<FlowRuleBatchOperation> uninstall(OpticalPathIntent intent) {
        TrafficSelector.Builder builder =
                DefaultTrafficSelector.builder(intent.selector());
        Iterator<Link> links = intent.path().links().iterator();
        ConnectPoint prev = links.next().dst();
        List<FlowRuleBatchEntry> rules = Lists.newLinkedList();
        // TODO Generate multiple batches
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
                    100,
                    true);
            rules.add(new FlowRuleBatchEntry(FlowRuleOperation.REMOVE, rule));
            prev = link.dst();
        }
        return Lists.newArrayList(new FlowRuleBatchOperation(rules));
    }

}
