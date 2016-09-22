/*
 * Copyright 2015-present Open Networking Laboratory
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
    private final Timestamp version;
    private NodeId origin;
    private int errorCount;

    private List<Intent> installables;

    /**
     * Creates a new intent data object.
     *
     * @param intent intent this metadata references
     * @param state intent state
     * @param version version of the intent for this key
     */
    public IntentData(Intent intent, IntentState state, Timestamp version) {
        checkNotNull(intent);
        checkNotNull(state);

        this.intent = intent;
        this.state = state;
        this.request = state;
        this.version = version;
    }

    /**
     * Creates a new intent data object.
     *
     * @param intent intent this metadata references
     * @param state intent state
     * @param version version of the intent for this key
     * @param origin ID of the node where the data was originally created
     */
    public IntentData(Intent intent, IntentState state, Timestamp version, NodeId origin) {
        checkNotNull(intent);
        checkNotNull(state);
        checkNotNull(version);
        checkNotNull(origin);

        this.intent = intent;
        this.state = state;
        this.request = state;
        this.version = version;
        this.origin = origin;
    }

    /**
     * Copy constructor.
     *
     * @param intentData intent data to copy
     */
    public IntentData(IntentData intentData) {
        checkNotNull(intentData);

        intent = intentData.intent;
        state = intentData.state;
        request = intentData.request;
        version = intentData.version;
        origin = intentData.origin;
        installables = intentData.installables;
        errorCount = intentData.errorCount;
    }

    /**
     * Create a new instance based on the original instance with new installables.
     *
     * @param original original data
     * @param installables new installable intents to set
     */
    public IntentData(IntentData original, List<Intent> installables) {
        this(original);

        this.installables = checkNotNull(installables).isEmpty() ?
                Collections.emptyList() : ImmutableList.copyOf(installables);
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
     * Returns the version of the intent for this key.
     *
     * @return intent version
     */
    public Timestamp version() {
        return version;
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
            return false;
        }

        // current and new data versions are the same
        IntentState currentState = currentData.state();
        IntentState newState = newData.state();

        switch (newState) {
        case INSTALLING:
            if (currentState == INSTALLING) {
                return false;
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
                return false;
            }
            // FALLTHROUGH
        case WITHDRAWN:
            if (currentState == WITHDRAWN) {
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
                return false;
            }
            return true;

        case CORRUPT:
            if (currentState == CORRUPT) {
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
                .add("intent", intent())
                .add("origin", origin())
                .add("installables", installables())
                .toString();
    }

}
