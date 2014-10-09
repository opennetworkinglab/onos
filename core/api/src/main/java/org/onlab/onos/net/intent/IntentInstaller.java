package org.onlab.onos.net.intent;

import java.util.concurrent.Future;

import org.onlab.onos.net.flow.CompletedBatchOperation;

/**
 * Abstraction of entity capable of installing intents to the environment.
 */
public interface IntentInstaller<T extends InstallableIntent> {
    /**
     * Installs the specified intent to the environment.
     *
     * @param intent intent to be installed
     * @throws IntentException if issues are encountered while installing the intent
     */
    Future<CompletedBatchOperation> install(T intent);

    /**
     * Uninstalls the specified intent from the environment.
     *
     * @param intent intent to be uninstalled
     * @throws IntentException if issues are encountered while uninstalling the intent
     */
    Future<CompletedBatchOperation> uninstall(T intent);
}
