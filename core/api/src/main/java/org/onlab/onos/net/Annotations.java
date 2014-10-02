package org.onlab.onos.net;

import java.util.Set;

/**
 * Represents an set of simple key/value string annotations.
 */
public interface Annotations {

    /**
     * Returns the set of keys for available annotations.
     *
     * @return annotation keys
     */
    public Set<String> keys();

    /**
     * Returns the value of the specified annotation.
     *
     * @param key annotation key
     * @return annotation value
     */
    public String value(String key);

}
