package org.onlab.onos.net.intent.impl;

import static org.onlab.onos.net.flow.DefaultTrafficTreatment.builder;

import java.util.Iterator;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.ApplicationId;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.flow.DefaultFlowRule;
import org.onlab.onos.net.flow.DefaultTrafficSelector;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRuleService;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.intent.IntentExtensionService;
import org.onlab.onos.net.intent.IntentInstaller;
import org.onlab.onos.net.intent.PathIntent;

/**
 * Installer for {@link PathIntent path connectivity intents}.
 */
@Component(immediate = true)
public class PathIntentInstaller implements IntentInstaller<PathIntent> {

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

    @Override
    public void install(PathIntent intent) {
        TrafficSelector.Builder builder =
                DefaultTrafficSelector.builder(intent.getTrafficSelector());
        Iterator<Link> links = intent.getPath().links().iterator();
        ConnectPoint prev = links.next().dst();

        while (links.hasNext()) {
            builder.matchInport(prev.port());
            Link link = links.next();
            TrafficTreatment treatment = builder()
                    .setOutput(link.src().port()).build();
            FlowRule rule = new DefaultFlowRule(link.src().deviceId(),
                    builder.build(), treatment,
                    123, appId, 600);
            flowRuleService.applyFlowRules(rule);
            prev = link.dst();
        }

    }

    @Override
    public void uninstall(PathIntent intent) {
        TrafficSelector.Builder builder =
                DefaultTrafficSelector.builder(intent.getTrafficSelector());
        Iterator<Link> links = intent.getPath().links().iterator();
        ConnectPoint prev = links.next().dst();

        while (links.hasNext()) {
            builder.matchInport(prev.port());
            Link link = links.next();
            TrafficTreatment treatment = builder()
                    .setOutput(link.src().port()).build();
            FlowRule rule = new DefaultFlowRule(link.src().deviceId(),
                    builder.build(), treatment,
                    123, appId, 600);
            flowRuleService.removeFlowRules(rule);
            prev = link.dst();
        }
    }
}
