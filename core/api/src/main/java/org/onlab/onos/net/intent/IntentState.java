package org.onlab.onos.net.intent;

/**
 * Representation of the phases an intent may attain during its lifecycle.
 */
public enum IntentState {

    /**
     * Signifies that the intent has been submitted and will start compiling
     * shortly. However, this compilation may not necessarily occur on the
     * local controller instance.
     * <p/>
     * All intent in the runtime take this state first.
     */
    SUBMITTED,

    /**
     * Signifies that the intent is being compiled into installable intents.
     * This is a transitional state after which the intent will enter either
     * {@link #FAILED} state or {@link #INSTALLING} state.
     */
    COMPILING,

    /**
     * Signifies that the resulting installable intents are being installed
     * into the network environment. This is a transitional state after which
     * the intent will enter either {@link #INSTALLED} state or
     * {@link #RECOMPILING} state.
     */
    INSTALLING,

    /**
     * The intent has been successfully installed. This is a state where the
     * intent may remain parked until it is withdrawn by the application or
     * until the network environment changes in some way to make the original
     * set of installable intents untenable.
     */
    INSTALLED,

    /**
     * Signifies that the intent is being recompiled into installable intents
     * as an attempt to adapt to an anomaly in the network environment.
     * This is a transitional state after which the intent will enter either
     * {@link #FAILED} state or {@link #INSTALLING} state.
     * <p/>
     * Exit to the {@link #FAILED} state may be caused by failure to compile
     * or by compiling into the same set of installable intents which have
     * previously failed to be installed.
     */
    RECOMPILING,

    /**
     * Indicates that the intent is being withdrawn. This is a transitional
     * state, triggered by invocation of the
     * {@link IntentService#withdraw(Intent)} but one with only one outcome,
     * which is the the intent being placed in the {@link #WITHDRAWN} state.
     */
    WITHDRAWING,

    /**
     * Indicates that the intent has been successfully withdrawn.
     */
    WITHDRAWN,

    /**
     * Signifies that the intent has failed compiling, installing or
     * recompiling states.
     */
    FAILED
}
