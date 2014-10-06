package org.onlab.onos.net.intent.impl;

import org.onlab.onos.net.intent.IntentId;

/**
 * Auxiliary delegate for integration of intent manager and flow trackerService.
 */
public interface TopologyChangeDelegate {

    /**
     * Notifies that topology has changed in such a way that the specified
     * intents should be recompiled.
     *
     * @param intentIds intents that should be recompiled
     */
    void bumpIntents(Iterable<IntentId> intentIds);

    /**
     * Notifies that topology has changed in such a way that the specified
     * intents should be marked failed and then recompiled.
     *
     * @param intentIds intents that should be failed and recompiled
     */
    void failIntents(Iterable<IntentId> intentIds);

}
