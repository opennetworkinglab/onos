package org.onlab.onos.net.statistic;

/**
 * Simple data repository for link load information.
 */
public interface Load {

    /**
     * Obtain the current observed rate on a link.
     * @return long value
     */
    long rate();

    /**
     * Obtain the latest counter viewed on that link.
     * @return long value
     */
    long latest();

}
