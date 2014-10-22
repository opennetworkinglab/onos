package org.onlab.onos.net.statistic;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.flow.FlowEntry;
import org.onlab.onos.net.flow.FlowRule;

import java.util.Set;

/**
 * Store to house the computed statistics.
 */
public interface StatisticStore {

    /**
     * Lay the foundation for receiving flow stats for this rule.
     * @param rule a {@link org.onlab.onos.net.flow.FlowRule}
     */
    void prepareForStatistics(FlowRule rule);

    /**
     * Remove entries associated with this rule.
     a @param rule {@link org.onlab.onos.net.flow.FlowRule}
     */
    void removeFromStatistics(FlowRule rule);

    /**
     * Adds a stats observation for a flow rule.
     * @param rule a {@link org.onlab.onos.net.flow.FlowEntry}
     */
    void addOrUpdateStatistic(FlowEntry rule);

    /**
     * Fetches the current observed stats values.
     * @param connectPoint the port to fetch information for
     * @return set of current flow rules
     */
    Set<FlowEntry> getCurrentStatistic(ConnectPoint connectPoint);

    /**
     * Fetches the current observed stats values.
     * @param connectPoint the port to fetch information for
     * @return set of current values
     */
    Set<FlowEntry> getPreviousStatistic(ConnectPoint connectPoint);
}
