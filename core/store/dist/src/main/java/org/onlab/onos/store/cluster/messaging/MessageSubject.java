package org.onlab.onos.store.cluster.messaging;

/**
 * Representation of a message subject.
 */
public enum MessageSubject {

    /** Represents a first greeting message. */
    HELLO,

    /** Signifies announcement about new member. */
    NEW_MEMBER,

    /** Signifies announcement about leaving member. */
    LEAVING_MEMBER,

    /** Signifies a heart-beat message. */
    ECHO,

    /** Anti-Entropy advertisement message. */
    AE_ADVERTISEMENT,

    /** Anti-Entropy reply message. */
    AE_REPLY,

}
