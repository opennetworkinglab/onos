package org.onlab.onos.net.statistic;

/**
 * Simple data repository for link load information.
 */
public interface Load {

    /**
     * Obtain the current observed rate (in bytes/s) on a link.
     * @return long value
     */
    long rate();

    /**
     * Obtain the latest bytes counter viewed on that link.
     * @return long value
     */
    long latest();

    /**
     * Indicates whether this load was built on valid values.
     * @return boolean
     */
    boolean isValid();

    /**
     * Returns when this value was seen.
     * @return epoch time
     */
    long time();

}
