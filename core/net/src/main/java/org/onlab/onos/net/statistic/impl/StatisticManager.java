package org.onlab.onos.net.statistic.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.Path;

import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRuleEvent;
import org.onlab.onos.net.flow.FlowRuleListener;
import org.onlab.onos.net.flow.FlowRuleService;
import org.onlab.onos.net.statistic.Load;
import org.onlab.onos.net.statistic.StatisticService;
import org.onlab.onos.net.statistic.StatisticStore;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides an implementation of the Statistic Service.
 */
@Component(immediate = true)
@Service
public class StatisticManager implements StatisticService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StatisticStore statisticStore;


    private final InternalFlowRuleListener listener = new InternalFlowRuleListener();

    @Activate
    public void activate() {
        flowRuleService.addListener(listener);
        log.info("Started");

    }

    @Deactivate
    public void deactivate() {
        flowRuleService.removeListener(listener);
        log.info("Stopped");
    }

    @Override
    public Load load(Link link) {
        return null;
    }

    @Override
    public Load load(ConnectPoint connectPoint) {
        return null;
    }

    @Override
    public Link max(Path path) {
        return null;
    }

    @Override
    public Link min(Path path) {
        return null;
    }

    @Override
    public FlowRule highestHitter(ConnectPoint connectPoint) {
        return null;
    }

    /**
     * Internal flow rule event listener.
     */
    private class InternalFlowRuleListener implements FlowRuleListener {

        @Override
        public void event(FlowRuleEvent event) {
//            FlowRule rule = event.subject();
//            switch (event.type()) {
//                case RULE_ADDED:
//                case RULE_UPDATED:
//                    if (rule instanceof FlowEntry) {
//                        statisticStore.addOrUpdateStatistic((FlowEntry) rule);
//                    }
//                    break;
//                case RULE_ADD_REQUESTED:
//                    statisticStore.prepareForStatistics(rule);
//                    break;
//                case RULE_REMOVE_REQUESTED:
//                case RULE_REMOVED:
//                    statisticStore.removeFromStatistics(rule);
//                    break;
//            }
        }
    }


}
