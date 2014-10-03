package org.onlab.onos.net.intent.impl;

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
import org.onlab.onos.net.flow.DefaultTrafficTreatment;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRuleService;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.flow.criteria.Criterion;
import org.onlab.onos.net.intent.IntentExtensionService;
import org.onlab.onos.net.intent.IntentInstaller;
import org.onlab.onos.net.intent.PathIntent;

/**
 * An intent installer for {@link PathIntent}.
 */
@Component(immediate = true)
public class PathIntentInstaller
        implements IntentInstaller<PathIntent> {
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private FlowRuleService flowRuleService;

    private final ApplicationId appId = ApplicationId.valueOf(1);

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
        TrafficSelector.Builder builder = new DefaultTrafficSelector.Builder();
        TrafficSelector selector = intent.getTrafficSelector();
        for (Criterion c : selector.criteria()) {
            builder.add(c);
        }

        Iterator<Link> links = intent.getPath().links().iterator();
        ConnectPoint prev = links.next().dst();
        while (links.hasNext()) {
            builder.matchInport(prev.port());
            Link link = links.next();

            TrafficTreatment.Builder treat = new DefaultTrafficTreatment.Builder();
            treat.setOutput(link.src().port());

            FlowRule f = new DefaultFlowRule(link.src().deviceId(),
                    builder.build(), treat.build(), 0, appId);
            flowRuleService.applyFlowRules(f);

            prev = link.dst();
        }

    }

    @Override
    public void uninstall(PathIntent intent) {
        //TODO
    }
}
