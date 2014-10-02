package org.onlab.onos.net.intent;

/**
 * This class represents the states of an intent.
 *
 * <p>
 * Note: The state is expressed as enum, but there is possibility
 * in the future that we define specific class instead of enum to improve
 * the extensibility of state definition.
 * </p>
 */
public enum IntentState {
    // FIXME: requires discussion on State vs. EventType and a solid state-transition diagram
    // TODO: consider the impact of conflict detection
    // TODO: consider the impact that external events affect an installed intent
    /**
     * The beginning state.
     *
     * All intent in the runtime take this state first.
     */
    SUBMITTED,

    /**
     * The intent compilation has been completed.
     *
     * An intent translation graph (tree) is completely created.
     * Leaves of the graph are installable intent type.
     */
    COMPILED,

    /**
     * The intent has been successfully installed.
     */
    INSTALLED,

    /**
     * The intent is being withdrawn.
     *
     * When {@link IntentService#withdraw(Intent)} is called,
     * the intent takes this state first.
     */
    WITHDRAWING,

    /**
     * The intent has been successfully withdrawn.
     */
    WITHDRAWN,

    /**
     * The intent has failed to be compiled, installed, or withdrawn.
     *
     * When the intent failed to be withdrawn, it is still, at least partially installed.
     */
    FAILED,
}
