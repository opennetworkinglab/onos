package org.onlab.onos.store.cluster.messaging;

/**
 * Representation of a message subject.
 */
public enum MessageSubject {

    /** Represents a first greeting message. */
    HELLO,

    /** Signifies node's intent to leave the cluster. */
    GOODBYE,

    /** Signifies a heart-beat message. */
    ECHO

}
