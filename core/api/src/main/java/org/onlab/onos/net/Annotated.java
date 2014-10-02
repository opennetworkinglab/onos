package org.onlab.onos.net;

import java.util.Set;

/**
 * Represents an entity that carries arbitrary annotations.
 */
public interface Annotated {

    /**
     * Returns the set of annotation keys currently available.
     *
     * @return set of annotation keys
     */
    Set<String> annotationKeys();

    /**
     * Returns the annotation value for the specified key.
     *
     * @param key annotation key
     * @return annotation value; null if there is no annotation
     */
    String annotation(String key);

}
