/*
 * Copyright 2015-present Open Networking Foundation
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
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.intent.IntentState.*;

/**
 * A wrapper class that contains an intents, its state, and other metadata for
 * internal use.
 */
@Beta
public class IntentData { //FIXME need to make this "immutable"
                          // manager should be able to mutate a local copy while processing

    private static final Logger log = LoggerFactory.getLogger(IntentData.class);

    private final Intent intent;

    private final IntentState request; //TODO perhaps we want a full fledged object for requests
    private IntentState state;
    /**
     * Intent's user request version.
     * <p>
     * version is assigned when an Intent was picked up by batch worker
     * and added to pending map.
     */
    private final Timestamp version;
    /**
     * Intent's internal state version.
     */
    // ~= mutation count
    private int internalStateVersion;
    private NodeId origin;
    private int errorCount;

    private List<Intent> installables;

    /**
     * Creates IntentData for Intent submit request.
     *
     * @param intent to request
     * @return IntentData
     */
    public static IntentData submit(Intent intent) {
        return new IntentData(checkNotNull(intent), INSTALL_REQ);
    }

    /**
     * Creates IntentData for Intent withdraw request.
     *
     * @param intent to request
     * @return IntentData
     */
    public static IntentData withdraw(Intent intent) {
        return new IntentData(checkNotNull(intent), WITHDRAW_REQ);
    }

    /**
     * Creates IntentData for Intent purge request.
     *
     * @param intent to request
     * @return IntentData
     */
    public static IntentData purge(Intent intent) {
        return new IntentData(checkNotNull(intent), PURGE_REQ);
    }

    /**
     * Creates updated IntentData after assigning task to a node.
     *
     * @param data IntentData to update work assignment
     * @param timestamp to assign to current request
     * @param node node which was assigned to handle this request (local node id)
     * @return updated IntentData object
     */
    public static IntentData assign(IntentData data, Timestamp timestamp, NodeId node) {
        IntentData assigned = new IntentData(data, checkNotNull(timestamp));
        assigned.origin = checkNotNull(node);
        assigned.internalStateVersion++;
        return assigned;
    }

    /**
     * Creates a copy of given IntentData.
     *
     * @param data intent data to copy
     * @return copy
     */
    public static IntentData copy(IntentData data) {
        return new IntentData(data);
    }

    /**
     * Creates a copy of given IntentData, and update request version.
     *
     * @param data intent data to copy
     * @param reqVersion request version to be updated
     * @return copy
     */
    public static IntentData copy(IntentData data, Timestamp reqVersion) {
        return new IntentData(data, checkNotNull(reqVersion));
    }

    /**
     * Create a copy of IntentData in next state.
     *
     * @param data intent data to copy
     * @param nextState to transition to
     * @return next state
     */
    public static IntentData nextState(IntentData data, IntentState nextState) {
        IntentData next = new IntentData(data);
        // TODO state machine sanity check
        next.setState(checkNotNull(nextState));
        return next;
    }

    // TODO Should this be method of it's own, or
    // should nextState(*, CORRUPT) call increment error count?
    /**
     * Creates a copy of IntentData in corrupt state,
     * incrementing error count.
     *
     * @param data intent data to copy
     * @return next state
     */
    public static IntentData corrupt(IntentData data) {
        IntentData next = new IntentData(data);
        next.setState(IntentState.CORRUPT);
        next.incrementErrorCount();
        return next;
    }

    /**
     * Creates updated IntentData with compilation result.
     *
     * @param data IntentData to update
     * @param installables compilation result
     * @return updated IntentData object
     */
    public static IntentData compiled(IntentData data, List<Intent> installables) {
        return new IntentData(data, checkNotNull(installables));
    }


    /**
     * Constructor for creating IntentData representing user request.
     *
     * @param intent this metadata references
     * @param reqState request state
     */
    private IntentData(Intent intent,
                       IntentState reqState) {
        this.intent = checkNotNull(intent);
        this.request = checkNotNull(reqState);
        this.version = null;
        this.state = reqState;
        this.installables = ImmutableList.of();
    }

    /**
     * Constructor for creating updated IntentData.
     *
     * @param original IntentData to copy from
     * @param newReqVersion new request version
     */
    private IntentData(IntentData original, Timestamp newReqVersion) {
        intent = original.intent;
        state = original.state;
        request = original.request;
        version = newReqVersion;
        internalStateVersion = original.internalStateVersion;
        origin = original.origin;
        installables = original.installables;
        errorCount = original.errorCount;
    }

    /**
     * Creates a new intent data object.
     *
     * @param intent intent this metadata references
     * @param state intent state
     * @param version version of the intent for this key
     *
     * @deprecated in 1.11.0
     */
    // used to create initial IntentData (version = null)
    @Deprecated
    public IntentData(Intent intent, IntentState state, Timestamp version) {
        checkNotNull(intent);
        checkNotNull(state);

        this.intent = intent;
        this.state = state;
        this.request = state;
        this.version = version;
    }

    /**
     * Copy constructor.
     *
     * @param intentData intent data to copy
     *
     */
    // used to create a defensive copy
    private IntentData(IntentData intentData) {
        checkNotNull(intentData);

        intent = intentData.intent;
        state = intentData.state;
        request = intentData.request;
        version = intentData.version;
        internalStateVersion = intentData.internalStateVersion;
        origin = intentData.origin;
        installables = intentData.installables;
        errorCount = intentData.errorCount;
    }

    /**
     * Create a new instance based on the original instance with new installables.
     *
     * @param original original data
     * @param installables new installable intents to set
     *
     */
    // used to create an instance who reached stable state
    // note that state is mutable field, so it gets altered else where
    // (probably that design is mother of all intent bugs)
    private  IntentData(IntentData original, List<Intent> installables) {
        this(original);
        this.internalStateVersion++;

        this.installables = checkNotNull(installables).isEmpty() ?
                      ImmutableList.of() : ImmutableList.copyOf(installables);
    }

    // kryo constructor
    protected IntentData() {
        intent = null;
        request = null;
        version = null;
    }

    /**
     * Returns the intent this metadata references.
     *
     * @return intent
     */
    public Intent intent() {
        return intent;
    }

    /**
     * Returns the state of the intent.
     *
     * @return intent state
     */
    public IntentState state() {
        return state;
    }

    public IntentState request() {
        return request;
    }

    /**
     * Returns the intent key.
     *
     * @return intent key
     */
    public Key key() {
        return intent.key();
    }

    /**
     * Returns the request version of the intent for this key.
     *
     * @return intent version
     */
    public Timestamp version() {
        return version;
    }

    // had to be made public for the store timestamp provider
    public int internalStateVersion() {
        return internalStateVersion;
    }

    /**
     * Returns the origin node that created this intent.
     *
     * @return origin node ID
     */
    public NodeId origin() {
        return origin;
    }

    /**
     * Updates the state of the intent to the given new state.
     *
     * @param newState new state of the intent
     */
    public void setState(IntentState newState) {
        this.internalStateVersion++;
        this.state = newState;
    }

    /**
     * Increments the error count for this intent.
     */
    public void incrementErrorCount() {
        errorCount++;
    }

    /**
     * Sets the error count for this intent.
     *
     * @param newCount new count
     */
    public void setErrorCount(int newCount) {
        errorCount = newCount;
    }

    /**
     * Returns the number of times that this intent has encountered an error
     * during installation or withdrawal.
     *
     * @return error count
     */
    public int errorCount() {
        return errorCount;
    }

    /**
     * Returns the installables associated with this intent.
     *
     * @return list of installable intents
     */
    public List<Intent> installables() {
        return installables != null ? installables : Collections.emptyList();
    }

    /**
     * Determines whether an intent data update is allowed. The update must
     * either have a higher version than the current data, or the state
     * transition between two updates of the same version must be sane.
     *
     * @param currentData existing intent data in the store
     * @param newData new intent data update proposal
     * @return true if we can apply the update, otherwise false
     */
    public static boolean isUpdateAcceptable(IntentData currentData, IntentData newData) {

        if (currentData == null) {
            return true;
        } else if (currentData.version().isOlderThan(newData.version())) {
            return true;
        } else if (currentData.version().isNewerThan(newData.version())) {
            log.trace("{} update not acceptable: current is newer", newData.key());
            return false;
        }

        assert (currentData.version().equals(newData.version()));
        if (currentData.internalStateVersion >= newData.internalStateVersion) {
            log.trace("{} update not acceptable: current is newer internally", newData.key());
            return false;
        }

        // current and new data versions are the same
        IntentState currentState = currentData.state();
        IntentState newState = newData.state();

        switch (newState) {
        case INSTALLING:
            if (currentState == INSTALLING) {
                log.trace("{} update not acceptable: no-op INSTALLING", newData.key());
                return false;
            }
            // FALLTHROUGH
        case REALLOCATING:
            if (currentState == REALLOCATING) {
                log.trace("{} update not acceptable: no-op REALLOCATING", newData.key());
                return false;
            } else if (currentState == INSTALLED) {
                return true;
            }
            // FALLTHROUGH
        case INSTALLED:
            if (currentState == INSTALLED) {
                return false;
            } else if (currentState == WITHDRAWING || currentState == WITHDRAWN
                    || currentState == PURGE_REQ) {
                log.warn("Invalid state transition from {} to {} for intent {}",
                         currentState, newState, newData.key());
                return false;
            }
            return true;

        case WITHDRAWING:
            if (currentState == WITHDRAWING) {
                log.trace("{} update not acceptable: no-op WITHDRAWING", newData.key());
                return false;
            }
            // FALLTHROUGH
        case WITHDRAWN:
            if (currentState == WITHDRAWN) {
                log.trace("{} update not acceptable: no-op WITHDRAWN", newData.key());
                return false;
            } else if (currentState == INSTALLING || currentState == INSTALLED
                    || currentState == PURGE_REQ) {
                log.warn("Invalid state transition from {} to {} for intent {}",
                         currentState, newState, newData.key());
                return false;
            }
            return true;

        case FAILED:
            if (currentState == FAILED) {
                log.trace("{} update not acceptable: no-op FAILED", newData.key());
                return false;
            }
            return true;

        case CORRUPT:
            if (currentState == CORRUPT) {
                log.trace("{} update not acceptable: no-op CORRUPT", newData.key());
                return false;
            }
            return true;

        case PURGE_REQ:
            // TODO we should enforce that only WITHDRAWN intents can be purged
            return true;

        case COMPILING:
        case RECOMPILING:
        case INSTALL_REQ:
        case WITHDRAW_REQ:
        default:
            log.warn("Invalid state {} for intent {}", newState, newData.key());
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(intent, version);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final IntentData other = (IntentData) obj;
        return Objects.equals(this.intent, other.intent)
                && Objects.equals(this.version, other.version);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("key", key())
                .add("state", state())
                .add("version", version())
                .add("internalStateVersion", internalStateVersion)
                .add("intent", intent())
                .add("origin", origin())
                .add("installables", installables())
                .toString();
    }

}
