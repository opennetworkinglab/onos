package org.onlab.onos.store.statistic.impl;

import org.onlab.onos.store.cluster.messaging.MessageSubject;

/**
 * MessageSubjects used by DistributedStatisticStore peer-peer communication.
 */
public final class StatisticStoreMessageSubjects {
    private StatisticStoreMessageSubjects() {}
        public static final MessageSubject GET_CURRENT =
                new MessageSubject("peer-return-current");
        public static final MessageSubject GET_PREVIOUS =
            new MessageSubject("peer-return-previous");

}
