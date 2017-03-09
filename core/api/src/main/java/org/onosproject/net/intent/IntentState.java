/*
 * Copyright 2014-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.net.intent;

import com.google.common.annotations.Beta;
import org.onosproject.net.intent.constraint.NonDisruptiveConstraint;

/**
 * Representation of the phases an intent may attain during its lifecycle.
 */
@Beta
public enum IntentState {

    /**
     * Signifies that the intent has been submitted and will start compiling
     * shortly. However, this compilation may not necessarily occur on the
     * local controller instance.
     * <p>
     * All intent in the runtime take this state first.
     * </p><p>
     * Intents will also pass through this state when they are updated.
     * </p>
     */
    INSTALL_REQ, // TODO submit_REQ?

    /**
     * Signifies that the intent is being compiled into installable intents.
     * This is a transitional state after which the intent will enter either
     * {@link #FAILED} state or {@link #INSTALLING} state.
     */
    COMPILING, //TODO do we really need this?

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
     * <p>
     * Exit to the {@link #FAILED} state may be caused by failure to compile
     * or by compiling into the same set of installable intents which have
     * previously failed to be installed.
     * </p>
     */
    RECOMPILING, // TODO perhaps repurpose as BROKEN.

    /**
     * Indicates that an application has requested that an intent be withdrawn.
     * It will start withdrawing shortly, but not necessarily on this instance.
     * Intents can also be parked here if it is impossible to withdraw them.
     */
    WITHDRAW_REQ,

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
     * Signifies that the intent has failed to be installed and cannot be
     * satisfied given current network conditions. But, the framework will
     * reattempt to install it when network conditions change until it is
     * withdrawn by an application.
     */
    FAILED, //TODO consider renaming to UNSATISFIABLE

    /**
     * Signifies that an intent has failed either installation or withdrawal,
     * and still hold some or all of its resources.
     * (e.g. link reservations, flow rules on the data plane, etc.)
     */
    CORRUPT, //TODO consider renaming to ERROR

    /**
     * Indicates that the intent should be purged from the database.
     * <p>
     * Note: This operation will only be performed if the intent is already
     * in WITHDRAWN or FAILED.
     * </p>
     */
    PURGE_REQ,

    /**
     * Indicates that the intent is being reallocated in a non-disruptive way {@link NonDisruptiveConstraint}.
     * This state can be used to inform other applications that the intent is performing
     * the reallocation. In particular type of networks this operation can require
     * a long time. After all the reallocation stages are completed, the intent
     * returns to the {@link #INSTALLED} state. If any of the reallocation stages fails,
     * the intent is flagged as {@link #FAILED}.
     */
    REALLOCATING
}
