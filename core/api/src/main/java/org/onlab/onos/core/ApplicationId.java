package org.onlab.onos.core;


/**
 * Application identifier.
 */
public interface ApplicationId {

    /**
     * Returns the application id.
     * @return a short value
     */
    short id();

    /**
     * Returns the applications supplied identifier.
     * @return a string identifier
     */
    String name();

}
