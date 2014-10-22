package org.onlab.onos.net.statistic;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.flow.FlowRule;

/**
 * Service for obtaining statistic information about link in the system.
 * Statistics are obtained from the FlowRuleService in order to minimize the
 * amount of hammering occuring at the dataplane.
 */
public interface StatisticService {

    /**
     * Obtain the load for a the ingress to the given link.
     * @param link the link to query.
     * @return a {@link org.onlab.onos.net.statistic.Load Load}
     */
    Load load(Link link);

    /**
     * Obtain the load for the given port.
     * @param connectPoint the port to query
     * @return a {@link org.onlab.onos.net.statistic.Load}
     */
    Load load(ConnectPoint connectPoint);

    /**
     * Find the most loaded link along a path.
     * @param path the path to search in
     * @return the most loaded {@link org.onlab.onos.net.Link}.
     */
    Link max(Path path);

    /**
     * Find the least loaded link along a path.
     * @param path the path to search in
     * @return the least loaded {@link org.onlab.onos.net.Link}.
     */
    Link min(Path path);

    /**
     * Returns the highest hitter (a flow rule) for a given port, ie. the
     * flow rule which is generating the most load.
     * @param connectPoint the port
     * @return the flow rule
     */
    FlowRule highestHitter(ConnectPoint connectPoint);

}
