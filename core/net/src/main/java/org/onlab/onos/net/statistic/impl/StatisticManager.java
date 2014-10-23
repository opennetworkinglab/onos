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

import org.onlab.onos.net.flow.FlowEntry;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRuleEvent;
import org.onlab.onos.net.flow.FlowRuleListener;
import org.onlab.onos.net.flow.FlowRuleService;
import org.onlab.onos.net.statistic.DefaultLoad;
import org.onlab.onos.net.statistic.Load;
import org.onlab.onos.net.statistic.StatisticService;
import org.onlab.onos.net.statistic.StatisticStore;
import org.slf4j.Logger;
import java.util.Set;

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
       return load(link.src());
    }

    @Override
    public Load load(ConnectPoint connectPoint) {
        return loadInternal(connectPoint);
    }

    @Override
    public Link max(Path path) {
        if (path.links().isEmpty()) {
            return null;
        }
        Load maxLoad = new DefaultLoad();
        Link maxLink = null;
        for (Link link : path.links()) {
            Load load = loadInternal(link.src());
            if (load.rate() > maxLoad.rate()) {
                maxLoad = load;
                maxLink = link;
            }
        }
        return maxLink;
    }

    @Override
    public Link min(Path path) {
        if (path.links().isEmpty()) {
            return null;
        }
        Load minLoad = new DefaultLoad();
        Link minLink = null;
        for (Link link : path.links()) {
            Load load = loadInternal(link.src());
            if (load.rate() < minLoad.rate()) {
                minLoad = load;
                minLink = link;
            }
        }
        return minLink;
    }

    @Override
    public FlowRule highestHitter(ConnectPoint connectPoint) {
        Set<FlowEntry> hitters = statisticStore.getCurrentStatistic(connectPoint);
        if (hitters.isEmpty()) {
            return null;
        }

        FlowEntry max = hitters.iterator().next();
        for (FlowEntry entry : hitters) {
            if (entry.bytes() > max.bytes()) {
                max = entry;
            }
        }
        return max;
    }

    private Load loadInternal(ConnectPoint connectPoint) {
        Set<FlowEntry> current;
        Set<FlowEntry> previous;
        synchronized (statisticStore) {
            current = statisticStore.getCurrentStatistic(connectPoint);
            previous = statisticStore.getPreviousStatistic(connectPoint);
        }
        if (current == null || previous == null) {
            return new DefaultLoad();
        }
        long currentAggregate = aggregate(current);
        long previousAggregate = aggregate(previous);

        return new DefaultLoad(currentAggregate, previousAggregate);
    }

    /**
     * Aggregates a set of values.
     * @param values the values to aggregate
     * @return a long value
     */
    private long aggregate(Set<FlowEntry> values) {
        long sum = 0;
        for (FlowEntry f : values) {
            sum += f.bytes();
        }
        return sum;
    }

    /**
     * Internal flow rule event listener.
     */
    private class InternalFlowRuleListener implements FlowRuleListener {

        @Override
        public void event(FlowRuleEvent event) {
            FlowRule rule = event.subject();
            switch (event.type()) {
                case RULE_ADDED:
                case RULE_UPDATED:
                    if (rule instanceof FlowEntry) {
                        statisticStore.addOrUpdateStatistic((FlowEntry) rule);
                    } else {
                        log.warn("IT AIN'T A FLOWENTRY");
                    }
                    break;
                case RULE_ADD_REQUESTED:
                    log.info("Preparing for stats");
                    statisticStore.prepareForStatistics(rule);
                    break;
                case RULE_REMOVE_REQUESTED:
                    log.info("Removing stats");
                    statisticStore.removeFromStatistics(rule);
                    break;
                case RULE_REMOVED:
                    break;
                default:
                    log.warn("Unknown flow rule event {}", event);
            }
        }
    }


}
