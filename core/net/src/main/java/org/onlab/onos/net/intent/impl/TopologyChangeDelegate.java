package org.onlab.onos.net.intent.impl;

import org.onlab.onos.net.intent.IntentId;

/**
 * Auxiliary delegate for integration of intent manager and flow trackerService.
 */
public interface TopologyChangeDelegate {

    /**
     * Notifies that topology has changed in such a way that the specified
     * intents should be recompiled. If the {@code compileAllFailed} parameter
     * is true, the all intents in {@link org.onlab.onos.net.intent.IntentState#FAILED}
     * state should be compiled as well.
     *
     * @param intentIds intents that should be recompiled
     * @param compileAllFailed true implies full compile is required; false for
     *                         selective recompile only
     */
    void triggerCompile(Iterable<IntentId> intentIds, boolean compileAllFailed);

}
