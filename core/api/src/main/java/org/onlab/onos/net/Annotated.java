package org.onlab.onos.net;

/**
 * Represents an entity that carries arbitrary annotations.
 */
public interface Annotated {

    /**
     * Returns the key/value annotations.
     *
     * @return key/value annotations
     */
    Annotations annotations();

}
