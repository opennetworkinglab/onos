package org.onlab.onos.net.flow;

import java.util.List;

import org.onlab.onos.net.flow.criteria.Criterion;

/**
 * Abstraction of a slice of network traffic.
 */
public interface TrafficSelector {

    /**
     * Returns selection criteria as an ordered list.
     *
     * @return list of criteria
     */
    List<Criterion> criteria();

    /**
     * Builder of traffic selector entities.
     */
    public interface Builder {

        /**
         * Adds a traffic selection criterion. If a same type criterion has
         * already been added, it will be replaced by this one.
         *
         * @param criterion new criterion
         * @return self
         */
        Builder add(Criterion criterion);

        /**
         * Builds an immutable traffic selector.
         *
         * @return traffic selector
         */
        TrafficSelector build();
    }

}
