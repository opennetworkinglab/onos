package org.onlab.onos.net.intent;

import java.util.List;

import org.onlab.onos.net.flow.FlowRuleBatchOperation;

/**
 * Abstraction of entity capable of installing intents to the environment.
 */
public interface IntentInstaller<T extends Intent> {
    /**
     * Installs the specified intent to the environment.
     *
     * @param intent intent to be installed
     * @throws IntentException if issues are encountered while installing the intent
     */
    List<FlowRuleBatchOperation> install(T intent);

    /**
     * Uninstalls the specified intent from the environment.
     *
     * @param intent intent to be uninstalled
     * @throws IntentException if issues are encountered while uninstalling the intent
     */
    List<FlowRuleBatchOperation> uninstall(T intent);
}
